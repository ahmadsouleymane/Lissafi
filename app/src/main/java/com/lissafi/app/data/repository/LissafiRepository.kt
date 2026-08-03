package com.lissafi.app.data.repository

import com.lissafi.app.data.LissafiDatabase
import com.lissafi.app.data.entity.AppSetting
import com.lissafi.app.data.entity.Client
import com.lissafi.app.data.entity.DebtTransaction
import com.lissafi.app.data.entity.Product
import com.lissafi.app.data.entity.Sale
import com.lissafi.app.data.entity.SaleItem
import kotlinx.coroutines.flow.Flow

class LissafiRepository(private val db: LissafiDatabase) {

    // --- Produits ---
    fun getAllProducts(): Flow<List<Product>> = db.productDao().getAll()
    fun searchProducts(query: String): Flow<List<Product>> = db.productDao().search(query)
    suspend fun getProduct(barcode: String): Product? = db.productDao().getByBarcode(barcode)
    suspend fun getProductCount(): Int = db.productDao().count()
    suspend fun getRecentProducts(limit: Int = 8): List<Product> = db.productDao().getRecent(limit)
    suspend fun upsertProduct(product: Product) = db.productDao().insert(product)
    suspend fun deleteProduct(product: Product) = db.productDao().delete(product)

    // --- Ventes ---
    suspend fun insertSale(sale: Sale, items: List<SaleItem>): Long {
        val saleId = db.saleDao().insertSale(sale)
        items.forEach { item -> db.saleDao().insertSaleItem(item.copy(saleId = saleId)) }
        return saleId
    }

    fun getAllSales(): Flow<List<Sale>> = db.saleDao().getAllSales()
    suspend fun getSalesBetween(start: Long, end: Long): List<Sale> = db.saleDao().getSalesBetween(start, end)
    suspend fun getSaleItems(saleId: Long): List<SaleItem> = db.saleDao().getItemsForSale(saleId)
    suspend fun getTopProducts(start: Long, end: Long) = db.saleDao().getTopProducts(start, end)
    suspend fun countSalesBetween(start: Long, end: Long): Int = db.saleDao().countSalesBetween(start, end)
    suspend fun sumTotalBetween(start: Long, end: Long): Int = db.saleDao().sumTotalBetween(start, end)
    suspend fun sumCreditBetween(start: Long, end: Long): Int = db.saleDao().sumCreditBetween(start, end)

    // --- Clients ---
    fun getAllClients(): Flow<List<Client>> = db.clientDao().getAll()
    fun searchClients(query: String): Flow<List<Client>> = db.clientDao().search(query)
    suspend fun getClient(id: String): Client? = db.clientDao().getById(id)
    suspend fun getClientCount(): Int = db.clientDao().count()
    suspend fun upsertClient(client: Client) = db.clientDao().insert(client)
    suspend fun updateClient(client: Client) = db.clientDao().update(client)

    // --- Dettes ---
    fun getDebtTransactions(clientId: String): Flow<List<DebtTransaction>> =
        db.debtTransactionDao().getForClient(clientId)

    suspend fun addDebtTransaction(transaction: DebtTransaction) {
        db.debtTransactionDao().insert(transaction)
        val total = db.debtTransactionDao().getTotalDebt(transaction.clientId)
        db.clientDao().getById(transaction.clientId)?.let { client ->
            db.clientDao().update(client.copy(totalDebt = total, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun getTotalDebt(clientId: String): Int = db.debtTransactionDao().getTotalDebt(clientId)
    suspend fun getLastTransactionDate(clientId: String): Long? =
        db.debtTransactionDao().getLastTransactionDate(clientId)

    // --- Paramètres ---
    suspend fun getSetting(key: String): String? = db.appSettingDao().get(key)?.value
    suspend fun setSetting(key: String, value: String) = db.appSettingDao().set(AppSetting(key, value))

    suspend fun isPremium(): Boolean = getSetting("is_premium") == "true"
    suspend fun getPremiumExpiry(): Long? = getSetting("premium_expiry")?.toLongOrNull()
    suspend fun getAdminPin(): String = getSetting("admin_pin") ?: "0000"
    suspend fun getShopName(): String = getSetting("shop_name") ?: ""
    suspend fun getShopPhone(): String = getSetting("shop_phone") ?: ""
}
