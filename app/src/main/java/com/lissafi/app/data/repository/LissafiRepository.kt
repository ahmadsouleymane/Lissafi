package com.lissafi.app.data.repository

import com.lissafi.app.data.LissafiDatabase
import com.lissafi.app.data.entity.Client
import com.lissafi.app.data.entity.DebtTransaction
import com.lissafi.app.data.entity.Product
import com.lissafi.app.data.entity.Sale
import com.lissafi.app.data.entity.SaleItem
import com.lissafi.app.data.remote.SupabaseApi
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

    suspend fun upsertProduct(product: Product) {
        val p = product.copy(userId = currentUserId)
        db.upsertProduct(p)
        syncToRemote()
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
        // Le push (api.insertSale + reassignSaleId + markSaleSynced) est fait par
        // SyncManager.pushSales, sous le mutex → une seule source de push, pas de doublons.
        syncToRemote()
        return saleId
    }

    suspend fun getSalesBetween(start: Long, end: Long): List<Sale> = db.getSalesBetween(start, end, currentUserId)
    suspend fun getSaleItems(saleId: Long): List<SaleItem> = db.getSaleItems(saleId)
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

    suspend fun isPremium(): Boolean = getSetting("is_premium") == "true"
    suspend fun getPremiumExpiry(): Long? = getSetting("premium_expiry")?.toLongOrNull()
    suspend fun getAdminPin(): String = getSetting("admin_pin") ?: "0000"
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
