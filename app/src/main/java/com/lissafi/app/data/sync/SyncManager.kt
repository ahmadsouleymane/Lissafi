package com.lissafi.app.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.lissafi.app.data.LissafiDatabase
import com.lissafi.app.data.entity.*
import com.lissafi.app.data.remote.SupabaseApi
import com.lissafi.app.data.remote.SupabaseManager
import com.lissafi.app.service.AppLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * État de la synchronisation.
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    NOT_CONFIGURED,
    NO_SESSION
}

/**
 * Gestionnaire de synchronisation bidirectionnelle entre SQLite locale et Supabase.
 *
 * Stratégie Local-First, synchro 100 % AUTOMATIQUE :
 * 1. Toutes les écritures vont D'ABORD dans SQLite locale (instantané, hors-ligne OK)
 * 2. La synchronisation Supabase est asynchrone, en arrière-plan
 * 3. Déclenchée : au démarrage, à la connexion, au retour du réseau, toutes les 15 min,
 *    et après chaque écriture (voir LissafiRepository)
 * 4. Une étape qui échoue ne bloque pas les autres ; le statut reflète les vraies erreurs
 */
class SyncManager(
    private val context: Context,
    private val db: LissafiDatabase,
    private val api: SupabaseApi
) {
    companion object {
        private const val TAG = "SyncManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Verrouille la synchro pour éviter les exécutions simultanées. */
    private val syncMutex = Mutex()

    private val _status = MutableStateFlow(SyncStatus.IDLE)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val _lastSyncAt = MutableStateFlow(0L)
    val lastSyncAt: StateFlow<Long> = _lastSyncAt.asStateFlow()

    private var isOnline = false

    /**
     * Démarre l'écoute de la connectivité réseau.
     * Appelé une fois au démarrage de l'appli.
     */
    fun startNetworkObserver() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // État initial : l'app peut déjà être en ligne au démarrage
        isOnline = isNetworkAvailable(cm)
        if (isOnline) AppLog.d(TAG, "Déjà en ligne au démarrage")

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                AppLog.d(TAG, "Réseau disponible — déclenchement synchro")
                isOnline = true
                syncInBackground()
            }

            override fun onLost(network: Network) {
                AppLog.d(TAG, "Réseau perdu")
                isOnline = false
            }
        })
    }

    private fun isNetworkAvailable(cm: ConnectivityManager): Boolean {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Déclenche une synchro automatique en arrière-plan.
     * Utilisée au démarrage, à la connexion et au retour du réseau.
     */
    fun syncInBackground() {
        scope.launch { syncAll() }
    }

    /**
     * Synchronise toutes les données : push local → remote, puis pull remote → local.
     */
    suspend fun syncAll() {
        if (!api.isConfigured) {
            AppLog.d(TAG, "Supabase non configuré, synchro ignorée")
            _status.value = SyncStatus.NOT_CONFIGURED
            return
        }
        if (!SupabaseManager.hasValidSession(context)) {
            AppLog.w(TAG, "Session invalide (user_id manquant ou non-UUID) — synchro ignorée")
            _status.value = SyncStatus.NO_SESSION
            api.logEvent("session_invalid", "warn", "Session invalide — synchro ignorée")
            return
        }

        // Le token d'accès GoTrue expire après ~1 h : le rafraîchir avant d'envoyer
        // quoi que ce soit, sinon toutes les étapes échouent en 401.
        try {
            api.ensureFreshSession()
        } catch (e: Exception) {
            AppLog.w(TAG, "Rafraîchissement de session échoué : ${e.message}")
        }

        // Amorçage boutique (Grand boutique) : garantit que le compte possède une
        // ligne shops + shop_members côté serveur — sans quoi la RLS is_shop_member
        // rejetterait ses écritures (cas d'un compte créé après le déploiement du
        // schéma multi-caisses). Best-effort, retenté à chaque cycle tant que non résolu.
        if (SupabaseManager.storedShopId(context) == null) {
            val shopId = api.getOrCreateMyShop()
            if (shopId != null) {
                SupabaseManager.setShop(context, shopId, "patron")
                AppLog.d(TAG, "Boutique amorcée: $shopId")
            }
        }

        syncMutex.withLock {
            _status.value = SyncStatus.SYNCING
            try {
                // Push : envoyer les données locales vers Supabase
                val pushOk = listOf(
                    runStep("pushProducts") { pushProducts() },
                    runStep("pushClients") { pushClients() },
                    runStep("pushSales") { pushSales() },
                    runStep("pushDebtTransactions") { pushDebtTransactions() },
                    runStep("pushSettings") { pushSettings() }
                ).all { it }

                // Pull : récupérer les données Supabase vers la base locale
                val pullOk = listOf(
                    runStep("pullProducts") { pullProducts() },
                    runStep("pullClients") { pullClients() },
                    runStep("pullSales") { pullSales() },
                    runStep("pullDebtTransactions") { pullDebtTransactions() },
                    runStep("pullSettings") { pullSettings() }
                ).all { it }

                if (pushOk && pullOk) {
                    val now = System.currentTimeMillis()
                    db.setSetting("last_sync_timestamp", now.toString())
                    _lastSyncAt.value = now
                    _status.value = SyncStatus.SUCCESS
                    AppLog.d(TAG, "Synchro terminée avec succès")
                    api.logEvent("sync", "info", "Synchronisation réussie")
                } else {
                    _status.value = SyncStatus.ERROR
                    Log.e(TAG, "Synchro partielle : certaines étapes ont échoué")
                    api.logEvent("sync_error", "warn", "Synchro partielle : certaines étapes ont échoué")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur synchro", e)
                _status.value = SyncStatus.ERROR
                api.logEvent("sync_error", "error", "Erreur de synchronisation : ${e.message}")
            }
        }
    }

    /** Exécute une étape de synchro ; retourne false si elle échoue, sans bloquer les autres. */
    private suspend fun runStep(name: String, block: suspend () -> Unit): Boolean =
        try {
            block()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Étape $name échouée", e)
            false
        }

    // ==================== PUSH (local → Supabase) ====================

    private suspend fun pushProducts() {
        val shopId = currentShopId
        // On pousse aussi les produits supprimés (soft delete) : c'est ainsi que la
        // suppression est propagée au serveur et aux autres appareils.
        val all = db.getAllProductsIncludingDeleted(shopId).map { it.copy(shopId = shopId) }
        if (all.isEmpty()) return
        api.upsertProducts(all)
        AppLog.d(TAG, "Push produits: ${all.size} envoyés")
    }

    private suspend fun pushClients() {
        val shopId = currentShopId
        val all = db.getAllClients(shopId).map { it.copy(shopId = shopId) }
        if (all.isEmpty()) return
        api.upsertClients(all)
        AppLog.d(TAG, "Push clients: ${all.size} envoyés")
    }

    private suspend fun pushSales() {
        val shopId = currentShopId
        val unsynced = db.getUnsyncedSales(shopId)
        for (sale in unsynced) {
            val items = db.getSaleItems(sale.id).map { it.copy(shopId = shopId) }
            val remoteId = api.insertSale(sale.copy(shopId = shopId), items)
            if (remoteId > 0) {
                // Aligner l'id local sur l'id généré par Supabase et marquer la vente
                // synchronisée, en une seule transaction atomique (voir finalizeSalePush).
                db.finalizeSalePush(sale.id, remoteId)
                AppLog.d(TAG, "Push vente → remote OK")
            }
        }
    }

    private suspend fun pushDebtTransactions() {
        val shopId = currentShopId
        // Ne pousser que les transactions pas encore synchronisées (flag local `synced`) :
        // repousser tout l'historique de tous les clients à chaque cycle déclenchait un GET
        // de dédoublonnage par transaction (O(n²) requêtes réseau).
        val unsynced = db.getUnsyncedDebtTransactions(shopId)
        for (txn in unsynced) {
            api.addDebtTransaction(txn.copy(shopId = shopId))
            db.markDebtTransactionSynced(txn.id)
        }
        AppLog.d(TAG, "Push dettes: ${unsynced.size} envoyées")
    }

    private suspend fun pushSettings() {
        val settings = db.getAllSettings()
        // Clés pilotées par le serveur (premium posé par redeem_premium_code ou
        // le back-office) : on ne les pousse JAMAIS — le trigger guard_premium_keys
        // les refuserait.
        val serverManaged = setOf(
            "is_premium", "plan", "premium_expiry", "activation_code",
            "demo_taken", "activation_method"
        )
        for (setting in settings) {
            if (setting.key == "last_sync_timestamp") continue // Ne pas sync le timestamp
            if (setting.key in serverManaged) continue
            api.setSetting(setting.key, setting.value)
        }
        AppLog.d(TAG, "Push paramètres terminé")
    }

    // ==================== PULL (Supabase → local) ====================

    private suspend fun pullProducts() {
        val remote = api.getAllProducts()
        val shopId = currentShopId
        var skipped = 0
        for (product in remote) {
            // Validation d'intégrité : montants/stock non négatifs, nom présent,
            // ligne liée à MA boutique (RLS en défense, mais on ne fait pas
            // confiance à un serveur compromis).
            if (!belongsToShop(product.shopId, product.userId, shopId) ||
                product.name.isBlank() ||
                product.sellPrice < 0 ||
                product.buyPrice < 0 ||
                product.stock < 0
            ) { skipped++; continue }
            db.upsertProduct(product)
        }
        AppLog.d(TAG, "Pull produits: ${remote.size} reçus, $skipped ignorés")
    }

    private suspend fun pullClients() {
        val remote = api.getAllClients()
        val shopId = currentShopId
        var skipped = 0
        for (client in remote) {
            if (!belongsToShop(client.shopId, client.userId, shopId) ||
                client.name.isBlank() ||
                client.totalDebt < 0
            ) { skipped++; continue }
            db.upsertClient(client)
        }
        AppLog.d(TAG, "Pull clients: ${remote.size} reçus, $skipped ignorés")
    }

    private suspend fun pullSales() {
        // Récupérer TOUT l'historique : après une réinstallation ou sur un 2e appareil,
        // les ventes anciennes ne doivent pas disparaître (avant : limité à 30 jours).
        val shopId = currentShopId
        val remote = api.getSalesBetween(0L, System.currentTimeMillis())
        var skipped = 0
        for (sale in remote) {
            if (!belongsToShop(sale.shopId, sale.userId, shopId) || sale.total < 0 || sale.amountPaid < 0) { skipped++; continue }
            if (!db.saleExists(sale.id)) {
                // On récupère d'abord les articles : si l'opération échoue, la vente
                // n'est pas insérée partiellement et sera retentée au prochain sync.
                val items = api.getSaleItems(sale.id)
                db.insertSaleIfNotExists(sale)
                for (item in items) {
                    if (!belongsToShop(item.shopId, item.userId, shopId) || item.price < 0 || item.quantity <= 0) { skipped++; continue }
                    db.insertSaleItemIfNotExists(item)
                }
            }
        }
        AppLog.d(TAG, "Pull ventes: ${remote.size} reçues, $skipped ignorées")
    }

    private suspend fun pullDebtTransactions() {
        val shopId = currentShopId
        val clients = db.getAllClients(shopId)
        var skipped = 0
        for (client in clients) {
            val remote = api.getDebtTransactions(client.id)
            for (txn in remote) {
                if (!belongsToShop(txn.shopId, txn.userId, shopId) || txn.amount < 0) { skipped++; continue }
                db.insertDebtTransactionIfNotExists(txn)
            }
        }
        AppLog.d(TAG, "Pull dettes terminé, $skipped ignorées")
    }

    private suspend fun pullSettings() {
        val remote = api.getAllSettings()
        for (setting in remote) {
            if (setting.key == "last_sync_timestamp") continue
            db.setSetting(setting.key, setting.value)
        }
        AppLog.d(TAG, "Pull paramètres: ${remote.size} reçus")
    }

    private val currentUserId: String
        get() = SupabaseManager.currentUserId(context) ?: ""

    /** Boutique courante (Grand boutique) : shop amorcé, sinon repli sur user_id (solo). */
    private val currentShopId: String
        get() = SupabaseManager.currentShopId(context)

    /**
     * Une ligne distante appartient-elle à ma boutique ? Si elle porte un shop_id,
     * on compare au shop_id courant. Sinon (schéma multi-caisses pas encore déployé
     * côté serveur, ou vieilles lignes), on retombe sur l'appartenance par user_id —
     * évite de tout rejeter au pull pendant la fenêtre APK-mis-à-jour / SQL-pas-encore-appliqué.
     */
    private fun belongsToShop(rowShopId: String, rowUserId: String, shopId: String): Boolean =
        if (rowShopId.isNotBlank()) rowShopId == shopId else rowUserId == currentUserId
}
