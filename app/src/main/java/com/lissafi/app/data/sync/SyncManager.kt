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
import com.lissafi.app.data.remote.SupabaseException
import com.lissafi.app.data.remote.SupabaseManager
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
    ERROR
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
        if (isOnline) Log.d(TAG, "Déjà en ligne au démarrage")

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Réseau disponible — déclenchement synchro")
                isOnline = true
                syncInBackground()
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Réseau perdu")
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
            Log.d(TAG, "Supabase non configuré, synchro ignorée")
            return
        }
        if (!SupabaseManager.hasValidSession(context)) {
            Log.w(TAG, "Session invalide (user_id manquant ou non-UUID) — synchro ignorée")
            return
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
                    Log.d(TAG, "Synchro terminée avec succès")
                } else {
                    _status.value = SyncStatus.ERROR
                    Log.e(TAG, "Synchro partielle : certaines étapes ont échoué")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur synchro", e)
                _status.value = SyncStatus.ERROR
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
        val all = db.getAllProducts()
        if (all.isEmpty()) return
        api.upsertProducts(all)
        Log.d(TAG, "Push produits: ${all.size} envoyés")
    }

    private suspend fun pushClients() {
        val all = db.getAllClients()
        if (all.isEmpty()) return
        api.upsertClients(all)
        Log.d(TAG, "Push clients: ${all.size} envoyés")
    }

    private suspend fun pushSales() {
        val unsynced = db.getUnsyncedSales()
        for (sale in unsynced) {
            val items = db.getSaleItems(sale.id)
            val remoteId = api.insertSale(sale, items)
            if (remoteId > 0) {
                // Aligner l'id local sur l'id généré par Supabase → pas de doublons au pull
                if (remoteId != sale.id) db.reassignSaleId(sale.id, remoteId)
                db.markSaleSynced(remoteId)
                Log.d(TAG, "Push vente ${sale.id} → remote $remoteId: OK")
            }
        }
    }

    private suspend fun pushDebtTransactions() {
        // Les transactions de dette sont poussées une par une (leur id est un autoincrement local,
        // donc on force l'upsert sans dépendre de l'id).
        val allClients = db.getAllClients()
        for (client in allClients) {
            val txns = db.getDebtTransactions(client.id)
            for (txn in txns) {
                api.addDebtTransaction(txn)
            }
        }
        Log.d(TAG, "Push dettes terminé")
    }

    private suspend fun pushSettings() {
        val settings = db.getAllSettings()
        for (setting in settings) {
            if (setting.key == "last_sync_timestamp") continue // Ne pas sync le timestamp
            api.setSetting(setting.key, setting.value)
        }
        Log.d(TAG, "Push paramètres terminé")
    }

    // ==================== PULL (Supabase → local) ====================

    private suspend fun pullProducts() {
        val remote = api.getAllProducts()
        for (product in remote) {
            db.upsertProduct(product)
        }
        Log.d(TAG, "Pull produits: ${remote.size} reçus")
    }

    private suspend fun pullClients() {
        val remote = api.getAllClients()
        for (client in remote) {
            db.upsertClient(client)
        }
        Log.d(TAG, "Pull clients: ${remote.size} reçus")
    }

    private suspend fun pullSales() {
        // Récupérer les ventes récentes (30 derniers jours)
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val remote = api.getSalesBetween(thirtyDaysAgo, System.currentTimeMillis())
        for (sale in remote) {
            if (!db.saleExists(sale.id)) {
                // On récupère d'abord les articles : si l'opération échoue, la vente
                // n'est pas insérée partiellement et sera retentée au prochain sync.
                val items = api.getSaleItems(sale.id)
                db.insertSaleIfNotExists(sale)
                for (item in items) {
                    db.insertSaleItemIfNotExists(item)
                }
            }
        }
        Log.d(TAG, "Pull ventes: ${remote.size} reçues")
    }

    private suspend fun pullDebtTransactions() {
        val clients = db.getAllClients()
        for (client in clients) {
            val remote = api.getDebtTransactions(client.id)
            for (txn in remote) {
                db.insertDebtTransactionIfNotExists(txn)
            }
        }
        Log.d(TAG, "Pull dettes terminé")
    }

    private suspend fun pullSettings() {
        val remote = api.getAllSettings()
        for (setting in remote) {
            if (setting.key == "last_sync_timestamp") continue
            db.setSetting(setting.key, setting.value)
        }
        Log.d(TAG, "Pull paramètres: ${remote.size} reçus")
    }
}
