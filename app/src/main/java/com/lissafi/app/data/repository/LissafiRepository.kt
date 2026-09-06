package com.lissafi.app.data.repository

import com.lissafi.app.data.LissafiDatabase
import com.lissafi.app.data.entity.Client
import com.lissafi.app.data.entity.DebtTransaction
import com.lissafi.app.data.entity.Product
import com.lissafi.app.data.entity.Sale
import com.lissafi.app.data.entity.SaleAuditEntry
import com.lissafi.app.data.entity.SaleItem
import com.lissafi.app.data.remote.SupabaseApi
import com.lissafi.app.service.FormatUtils
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class LissafiRepository(
    private val db: LissafiDatabase,
    private val api: SupabaseApi,
    private val onDataChanged: () -> Unit = {}
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        /** Clé locale du hash du PIN gérant (exclue de la synchro — voir SyncManager.pushSettings). */
        const val KEY_MANAGER_PIN = "manager_pin_hash"

        private fun hashPin(pin: String): String =
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(pin.trim().toByteArray())
                .joinToString("") { "%02x".format(it) }
    }

    /**
     * Fournit le userId courant pour toutes les opérations.
     * Fourni par le ViewModel/Screen via le callback.
     */
    var currentUserIdProvider: () -> String = { "" }

    private val currentUserId: String get() = currentUserIdProvider()

    fun withUserId(id: String): LissafiRepository {
        currentUserIdProvider = { id }
        return this
    }

    // --- Produits ---

    val productsFlow: Flow<List<Product>> get() = db.productsFlow

    suspend fun getAllProducts(): List<Product> = db.getAllProducts(currentUserId)
    suspend fun getProduct(barcode: String): Product? = db.getProduct(barcode, currentUserId)
    suspend fun searchProducts(query: String): List<Product> = db.searchProducts(query, currentUserId)
    suspend fun getProductCount(): Int = db.getProductCount(currentUserId)
    suspend fun getRecentProducts(limit: Int = 8): List<Product> = db.getRecentProducts(limit, currentUserId)
    suspend fun getLowStockProducts(): List<Product> = db.getLowStockProducts(currentUserId)

    suspend fun upsertProduct(product: Product) {
        val p = product.copy(userId = currentUserId)
        db.upsertProduct(p)
        logFunnelOnce("first_product")
        syncToRemote()
    }

    /**
     * Émet un événement de funnel une seule fois par compte (flag local `evt_*`),
     * fire-and-forget vers le back-office. Ne déclenche pas de synchro.
     */
    private suspend fun logFunnelOnce(event: String) {
        val key = "evt_$event"
        if (db.getSetting(key) != null) return
        db.setSetting(key, "1")
        api.logEvent(event, "info", "")
    }

    suspend fun deleteProduct(product: Product) {
        // Soft delete local ; la suppression (deleted=1) est poussée par SyncManager.pushProducts.
        db.deleteProduct(product.copy(userId = currentUserId))
        syncToRemote()
    }

    // --- Ventes ---

    suspend fun insertSale(sale: Sale, items: List<SaleItem>): Long {
        val s = sale.copy(userId = currentUserId)
        val itms = items.map { it.copy(userId = currentUserId) }
        val saleId = db.insertSale(s, itms)
        // Trace de création (Journal des ventes / anti-fraude).
        db.addSaleAudit(
            SaleAuditEntry(
                saleId = saleId,
                action = "created",
                details = "Vente enregistrée · ${FormatUtils.formatFCFA(s.total)}" + if (s.isCredit) " (crédit)" else "",
                date = System.currentTimeMillis(),
                userId = currentUserId
            )
        )
        logFunnelOnce("first_sale")
        // Le push (api.insertSale + finalizeSalePush) est fait par SyncManager.pushSales,
        // sous le mutex → une seule source de push, pas de doublons.
        syncToRemote()
        return saleId
    }

    suspend fun getSalesBetween(start: Long, end: Long): List<Sale> = db.getSalesBetween(start, end, currentUserId)
    suspend fun getSaleItems(saleId: Long): List<SaleItem> = db.getSaleItems(saleId)

    // --- Journal des ventes (consultation / modification / annulation) ---

    suspend fun getSalesJournal(limit: Int = 500): List<Sale> = db.getSalesJournal(currentUserId, limit)
    suspend fun getSaleById(id: Long): Sale? = db.getSaleById(id, currentUserId)
    suspend fun getSaleAudit(saleId: Long): List<SaleAuditEntry> = db.getSaleAudit(saleId)

    /**
     * Modifie une vente : remplace les articles, met à jour le total/mode de
     * paiement, ajuste le stock (restaure l'ancien, applique le nouveau) et la
     * dette (si crédit), et écrit une entrée d'audit. Rien n'est perdu : la
     * modification est tracée en base, locale et distante.
     */
    suspend fun modifySale(
        oldSale: Sale,
        oldItems: List<SaleItem>,
        newSale: Sale,
        newItems: List<SaleItem>,
        auditDetails: String
    ) {
        val uid = currentUserId
        val now = System.currentTimeMillis()
        val itemsWithUser = newItems.map { it.copy(saleId = oldSale.id, userId = uid) }
        val saleToStore = newSale.copy(id = oldSale.id, userId = uid, cancelled = false)
        db.modifySaleAtomic(
            saleToStore,
            itemsWithUser,
            SaleAuditEntry(saleId = oldSale.id, action = "modified", details = auditDetails, date = now, userId = uid)
        )
        adjustStockForChange(oldItems, newItems, uid, now)
        adjustDebtForChange(oldSale, newSale, now, uid)
        syncToRemote()
    }

    /**
     * Annule (soft) une vente : elle reste en base marquée annulée (jamais
     * supprimée), le stock est restauré et la dette annulée si c'était un crédit.
     * Un motif obligatoire est consigné dans la trace d'audit.
     */
    suspend fun cancelSale(sale: Sale, items: List<SaleItem>, reason: String) {
        val uid = currentUserId
        val now = System.currentTimeMillis()
        val details = "Vente annulée" + if (reason.isNotBlank()) " · $reason" else ""
        db.cancelSaleAtomic(
            sale.id,
            SaleAuditEntry(saleId = sale.id, action = "cancelled", details = details, date = now, userId = uid)
        )
        // Restaure le stock : les articles n'ont finalement pas été vendus.
        for (item in items) {
            try {
                val product = db.getProduct(item.barcode, uid) ?: continue
                db.upsertProduct(product.copy(stock = maxOf(0, product.stock + item.quantity.roundToInt()), updatedAt = now))
            } catch (_: Exception) {}
        }
        // Annule la dette si c'était un crédit.
        if (sale.isCredit && sale.clientId != null) {
            db.addDebtTransaction(
                DebtTransaction(
                    clientId = sale.clientId, saleId = sale.id, amount = -sale.total,
                    date = now, note = "Annulation vente N°${sale.id}", userId = uid
                )
            )
        }
        syncToRemote()
    }

    /** Ajuste le stock au changement d'articles : delta = ancienne qté − nouvelle qté (par code-barres). */
    private suspend fun adjustStockForChange(oldItems: List<SaleItem>, newItems: List<SaleItem>, uid: String, now: Long) {
        val delta = HashMap<String, Int>()
        for (it in oldItems) delta[it.barcode] = (delta[it.barcode] ?: 0) + it.quantity.roundToInt()
        for (it in newItems) delta[it.barcode] = (delta[it.barcode] ?: 0) - it.quantity.roundToInt()
        for ((barcode, d) in delta) {
            if (d == 0) continue
            try {
                val product = db.getProduct(barcode, uid) ?: continue
                db.upsertProduct(product.copy(stock = maxOf(0, product.stock + d), updatedAt = now))
            } catch (_: Exception) {}
        }
    }

    /** Ajuste la dette du/des client(s) au changement (crédit ↔ comptant, montant, client). */
    private suspend fun adjustDebtForChange(oldSale: Sale, newSale: Sale, now: Long, uid: String) {
        val oldClient = oldSale.clientId
        val newClient = newSale.clientId
        val oldCredit = oldSale.isCredit && oldClient != null
        val newCredit = newSale.isCredit && newClient != null

        if (oldCredit && newCredit && oldClient == newClient) {
            val diff = newSale.total - oldSale.total
            if (diff != 0) {
                db.addDebtTransaction(
                    DebtTransaction(
                        clientId = oldClient!!, saleId = oldSale.id, amount = diff,
                        date = now, note = "Modification vente N°${oldSale.id}", userId = uid
                    )
                )
            }
            return
        }
        // Client changé, ou bascule crédit/comptant : on défait l'ancien effet…
        if (oldCredit) {
            db.addDebtTransaction(
                DebtTransaction(
                    clientId = oldClient!!, saleId = oldSale.id, amount = -oldSale.total,
                    date = now, note = "Annulation crédit vente N°${oldSale.id}", userId = uid
                )
            )
        }
        // …et on applique le nouveau.
        if (newCredit) {
            db.addDebtTransaction(
                DebtTransaction(
                    clientId = newClient!!, saleId = oldSale.id, amount = newSale.total,
                    date = now, note = "Vente N°${oldSale.id} (modifiée)", userId = uid
                )
            )
        }
    }
    suspend fun getTopProducts(start: Long, end: Long) = db.getTopProducts(start, end, currentUserId)
    suspend fun countSalesBetween(start: Long, end: Long): Int = db.countSalesBetween(start, end, currentUserId)
    suspend fun sumTotalBetween(start: Long, end: Long): Int = db.sumTotalBetween(start, end, currentUserId)
    suspend fun sumCreditBetween(start: Long, end: Long): Int = db.sumCreditBetween(start, end, currentUserId)

    // --- Clients ---

    val clientsFlow: Flow<List<Client>> get() = db.clientsFlow

    suspend fun getAllClients(): List<Client> = db.getAllClients(currentUserId)
    suspend fun getClient(id: String): Client? = db.getClient(id, currentUserId)
    suspend fun searchClients(query: String): List<Client> = db.searchClients(query, currentUserId)
    suspend fun getClientCount(): Int = db.getClientCount(currentUserId)

    suspend fun upsertClient(client: Client) {
        val c = client.copy(userId = currentUserId)
        db.upsertClient(c)
        syncToRemote()
    }

    // --- Dettes ---

    suspend fun getDebtTransactions(clientId: String): List<DebtTransaction> =
        db.getDebtTransactions(clientId, currentUserId)

    suspend fun addDebtTransaction(transaction: DebtTransaction) {
        val t = transaction.copy(userId = currentUserId)
        db.addDebtTransaction(t)
        syncToRemote()
    }

    suspend fun getTotalDebt(clientId: String): Int = db.getTotalDebt(clientId, currentUserId)

    // --- Paramètres ---

    suspend fun getSetting(key: String): String? = db.getSetting(key)

    suspend fun setSetting(key: String, value: String) {
        db.setSetting(key, value)
        syncToRemote()
    }

    // --- Verrou gérant (PIN local anti-fraude) ---
    // Le hash reste LOCAL (jamais poussé — exclu de pushSettings) : il protège la
    // modification/annulation d'une vente contre un employé, sur cet appareil.

    suspend fun hasManagerPin(): Boolean = !getSetting(KEY_MANAGER_PIN).isNullOrBlank()

    /** Pose/écrase le PIN gérant (haché). Écriture LOCALE uniquement (pas de sync). */
    suspend fun setManagerPin(pin: String) = db.setSetting(KEY_MANAGER_PIN, hashPin(pin))

    /** Désactive le verrou. Écriture LOCALE uniquement. */
    suspend fun clearManagerPin() = db.setSetting(KEY_MANAGER_PIN, "")

    /** Vrai si le PIN correspond, ou si aucun PIN n'est configuré (pas de verrou). */
    suspend fun verifyManagerPin(pin: String): Boolean {
        val stored = getSetting(KEY_MANAGER_PIN)
        if (stored.isNullOrBlank()) return true
        return stored == hashPin(pin)
    }

    suspend fun isPremium(): Boolean = getSetting("is_premium") == "true"
    suspend fun getPremiumExpiry(): Long? = getSetting("premium_expiry")?.toLongOrNull()
    suspend fun getShopName(): String = getSetting("shop_name") ?: ""
    suspend fun getShopPhone(): String = getSetting("shop_phone") ?: ""

    private fun syncToRemote() {
        scope.launch {
            // Session invalide (user_id non-UUID ou token absent) : on n'écrit rien
            // sur Supabase — les INSERT seraient rejetés par le typage UUID + RLS.
            if (!api.isConfigured || !api.hasValidSession) return@launch
            try {
                // Rafraîchit le token s'il est expiré (sinon la synchro échoue en 401).
                api.ensureFreshSession()
                // Déclenche la synchro via SyncManager (source de push UNIQUE sous mutex) :
                // élimine la course de double-push qui pouvait dupliquer les ventes.
                onDataChanged()
            } catch (e: Exception) {
                // Silencieux : les échecs réseau seront retentés par SyncManager.
            }
        }
    }
}
