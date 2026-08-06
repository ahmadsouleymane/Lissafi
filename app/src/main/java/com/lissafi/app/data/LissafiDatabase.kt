package com.lissafi.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.lissafi.app.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class LissafiDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "lissafi.db"
        const val DATABASE_VERSION = 2

        @Volatile
        private var INSTANCE: LissafiDatabase? = null

        fun getInstance(context: Context): LissafiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LissafiDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS products (
                barcode TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                sell_price INTEGER NOT NULL DEFAULT 0,
                buy_price INTEGER NOT NULL DEFAULT 0,
                stock INTEGER NOT NULL DEFAULT 0,
                min_stock INTEGER NOT NULL DEFAULT 5,
                category TEXT NOT NULL DEFAULT '',
                has_barcode INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                user_id TEXT NOT NULL DEFAULT ''
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS sales (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date INTEGER NOT NULL,
                total INTEGER NOT NULL,
                amount_paid INTEGER NOT NULL DEFAULT 0,
                change_given INTEGER NOT NULL DEFAULT 0,
                is_credit INTEGER NOT NULL DEFAULT 0,
                client_id TEXT,
                synced INTEGER NOT NULL DEFAULT 0,
                user_id TEXT NOT NULL DEFAULT ''
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS sale_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sale_id INTEGER NOT NULL,
                barcode TEXT NOT NULL,
                name TEXT NOT NULL,
                price INTEGER NOT NULL,
                quantity REAL NOT NULL DEFAULT 1.0,
                user_id TEXT NOT NULL DEFAULT ''
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS clients (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                phone TEXT NOT NULL DEFAULT '',
                total_debt INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                user_id TEXT NOT NULL DEFAULT ''
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS debt_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                client_id TEXT NOT NULL,
                sale_id INTEGER,
                amount INTEGER NOT NULL,
                date INTEGER NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                user_id TEXT NOT NULL DEFAULT ''
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS app_settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // V1 → V2 : ajout colonne user_id
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE products ADD COLUMN user_id TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE sales ADD COLUMN user_id TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE sale_items ADD COLUMN user_id TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE clients ADD COLUMN user_id TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE debt_transactions ADD COLUMN user_id TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
        }
    }

    // ==================== PRODUCTS ====================

    private val _productsFlow = MutableStateFlow<List<Product>>(emptyList())
    val productsFlow: Flow<List<Product>> = _productsFlow.asStateFlow()

    suspend fun getAllProducts(userId: String = ""): List<Product> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Product>()
        val where = if (userId.isNotEmpty()) "WHERE user_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(userId) else null
        readableDatabase.rawQuery("SELECT * FROM products $where ORDER BY updated_at DESC", args).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toProduct())
        }
        _productsFlow.value = list
        list
    }

    suspend fun getProduct(barcode: String): Product? = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT * FROM products WHERE barcode = ?", arrayOf(barcode)).use { cursor ->
            if (cursor.moveToFirst()) cursor.toProduct() else null
        }
    }

    suspend fun searchProducts(query: String, userId: String = ""): List<Product> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Product>()
        val where = if (userId.isNotEmpty()) "AND user_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf("%$query%", userId) else arrayOf("%$query%")
        readableDatabase.rawQuery("SELECT * FROM products WHERE name LIKE ? $where ORDER BY name ASC", args).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toProduct())
        }
        list
    }

    suspend fun getProductCount(userId: String = ""): Int = withContext(Dispatchers.IO) {
        val where = if (userId.isNotEmpty()) "WHERE user_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(userId) else null
        readableDatabase.rawQuery("SELECT COUNT(*) FROM products $where", args).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    suspend fun getRecentProducts(limit: Int = 8, userId: String = ""): List<Product> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Product>()
        val where = if (userId.isNotEmpty()) "WHERE user_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(userId, limit.toString()) else arrayOf(limit.toString())
        readableDatabase.rawQuery("SELECT * FROM products $where ORDER BY updated_at DESC LIMIT ?", args).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toProduct())
        }
        list
    }

    suspend fun upsertProduct(product: Product) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("barcode", product.barcode)
            put("name", product.name)
            put("sell_price", product.sellPrice)
            put("buy_price", product.buyPrice)
            put("stock", product.stock)
            put("min_stock", product.minStock)
            put("category", product.category)
            put("has_barcode", if (product.hasBarcode) 1 else 0)
            put("created_at", product.createdAt)
            put("updated_at", product.updatedAt)
            put("user_id", product.userId)
        }
        writableDatabase.insertWithOnConflict("products", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        getAllProducts(product.userId)
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        writableDatabase.delete("products", "barcode = ?", arrayOf(product.barcode))
        getAllProducts(product.userId)
    }

    // ==================== SALES ====================

    suspend fun insertSale(sale: Sale, items: List<SaleItem>): Long = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("date", sale.date)
            put("total", sale.total)
            put("amount_paid", sale.amountPaid)
            put("change_given", sale.changeGiven)
            put("is_credit", if (sale.isCredit) 1 else 0)
            put("client_id", sale.clientId)
            put("synced", if (sale.synced) 1 else 0)
            put("user_id", sale.userId)
        }
        val saleId = writableDatabase.insert("sales", null, cv)
        for (item in items) {
            val itemCv = ContentValues().apply {
                put("sale_id", saleId)
                put("barcode", item.barcode)
                put("name", item.name)
                put("price", item.price)
                put("quantity", item.quantity)
                put("user_id", item.userId)
            }
            writableDatabase.insert("sale_items", null, itemCv)
        }
        saleId
    }

    suspend fun getSalesBetween(start: Long, end: Long, userId: String = ""): List<Sale> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Sale>()
        val where = if (userId.isNotEmpty()) "AND user_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(start.toString(), end.toString(), userId) else arrayOf(start.toString(), end.toString())
        readableDatabase.rawQuery("SELECT * FROM sales WHERE date >= ? AND date < ? $where ORDER BY date DESC", args).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toSale())
        }
        list
    }

    suspend fun getSaleItems(saleId: Long): List<SaleItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SaleItem>()
        readableDatabase.rawQuery("SELECT * FROM sale_items WHERE sale_id = ?", arrayOf(saleId.toString())).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toSaleItem())
        }
        list
    }

    data class TopProduct(val name: String, val totalQty: Double, val count: Int)

    suspend fun getTopProducts(start: Long, end: Long, userId: String = ""): List<TopProduct> = withContext(Dispatchers.IO) {
        val list = mutableListOf<TopProduct>()
        val where = if (userId.isNotEmpty()) "AND s.user_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(start.toString(), end.toString(), userId) else arrayOf(start.toString(), end.toString())
        readableDatabase.rawQuery("""
            SELECT si.name, SUM(si.quantity) as total_qty, COUNT(DISTINCT si.sale_id) as cnt
            FROM sale_items si JOIN sales s ON si.sale_id = s.id
            WHERE s.date >= ? AND s.date < ? $where
            GROUP BY si.name ORDER BY total_qty DESC
        """, args).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(TopProduct(cursor.getString(0), cursor.getDouble(1), cursor.getInt(2)))
            }
        }
        list
    }

    suspend fun countSalesBetween(start: Long, end: Long, userId: String = ""): Int = withContext(Dispatchers.IO) {
        val where = if (userId.isNotEmpty()) "AND user_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(start.toString(), end.toString(), userId) else arrayOf(start.toString(), end.toString())
        readableDatabase.rawQuery("SELECT COUNT(*) FROM sales WHERE date >= ? AND date < ? $where", args).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    suspend fun sumTotalBetween(start: Long, end: Long, userId: String = ""): Int = withContext(Dispatchers.IO) {
        val where = if (userId.isNotEmpty()) "AND user_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(start.toString(), end.toString(), userId) else arrayOf(start.toString(), end.toString())
        readableDatabase.rawQuery("SELECT COALESCE(SUM(total), 0) FROM sales WHERE date >= ? AND date < ? $where", args).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    suspend fun sumCreditBetween(start: Long, end: Long, userId: String = ""): Int = withContext(Dispatchers.IO) {
        val where = if (userId.isNotEmpty()) "AND user_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(start.toString(), end.toString(), userId) else arrayOf(start.toString(), end.toString())
        readableDatabase.rawQuery("SELECT COALESCE(SUM(total), 0) FROM sales WHERE date >= ? AND date < ? AND is_credit = 1 $where", args).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    // ==================== CLIENTS ====================

    private val _clientsFlow = MutableStateFlow<List<Client>>(emptyList())
    val clientsFlow: Flow<List<Client>> = _clientsFlow.asStateFlow()

    suspend fun getAllClients(userId: String = ""): List<Client> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Client>()
        val where = if (userId.isNotEmpty()) "WHERE user_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(userId) else null
        readableDatabase.rawQuery("SELECT * FROM clients $where ORDER BY total_debt DESC", args).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toClient())
        }
        _clientsFlow.value = list
        list
    }

    suspend fun getClient(id: String): Client? = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT * FROM clients WHERE id = ?", arrayOf(id)).use { cursor ->
            if (cursor.moveToFirst()) cursor.toClient() else null
        }
    }

    suspend fun searchClients(query: String, userId: String = ""): List<Client> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Client>()
        val where = if (userId.isNotEmpty()) "AND user_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf("%$query%", userId) else arrayOf("%$query%")
        readableDatabase.rawQuery("SELECT * FROM clients WHERE name LIKE ? $where ORDER BY name ASC", args).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toClient())
        }
        list
    }

    suspend fun getClientCount(userId: String = ""): Int = withContext(Dispatchers.IO) {
        val where = if (userId.isNotEmpty()) "WHERE user_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(userId) else null
        readableDatabase.rawQuery("SELECT COUNT(*) FROM clients $where", args).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    suspend fun upsertClient(client: Client) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("id", client.id)
            put("name", client.name)
            put("phone", client.phone)
            put("total_debt", client.totalDebt)
            put("created_at", client.createdAt)
            put("updated_at", client.updatedAt)
            put("user_id", client.userId)
        }
        writableDatabase.insertWithOnConflict("clients", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        getAllClients(client.userId)
    }

    suspend fun updateClient(client: Client) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("name", client.name)
            put("phone", client.phone)
            put("total_debt", client.totalDebt)
            put("updated_at", client.updatedAt)
            put("user_id", client.userId)
        }
        writableDatabase.update("clients", cv, "id = ?", arrayOf(client.id))
        getAllClients(client.userId)
    }

    // ==================== DEBT TRANSACTIONS ====================

    suspend fun getDebtTransactions(clientId: String): List<DebtTransaction> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DebtTransaction>()
        readableDatabase.rawQuery("SELECT * FROM debt_transactions WHERE client_id = ? ORDER BY date DESC", arrayOf(clientId)).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toDebtTransaction())
        }
        list
    }

    suspend fun addDebtTransaction(transaction: DebtTransaction) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("client_id", transaction.clientId)
            if (transaction.saleId != null) put("sale_id", transaction.saleId)
            put("amount", transaction.amount)
            put("date", transaction.date)
            put("note", transaction.note)
            put("user_id", transaction.userId)
        }
        writableDatabase.insert("debt_transactions", null, cv)

        writableDatabase.rawQuery("SELECT COALESCE(SUM(amount), 0) FROM debt_transactions WHERE client_id = ?", arrayOf(transaction.clientId)).use { cursor ->
            if (cursor.moveToFirst()) {
                val total = cursor.getInt(0)
                val cv2 = ContentValues().apply {
                    put("total_debt", total)
                    put("updated_at", System.currentTimeMillis())
                }
                writableDatabase.update("clients", cv2, "id = ?", arrayOf(transaction.clientId))
            }
        }
        getAllClients(transaction.userId)
    }

    suspend fun getTotalDebt(clientId: String): Int = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT COALESCE(SUM(amount), 0) FROM debt_transactions WHERE client_id = ?", arrayOf(clientId)).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    // ==================== SETTINGS ====================

    suspend fun getSetting(key: String): String? = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT value FROM app_settings WHERE key = ?", arrayOf(key)).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    suspend fun setSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("key", key)
            put("value", value)
        }
        writableDatabase.insertWithOnConflict("app_settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun getAllSettings(): List<AppSetting> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AppSetting>()
        readableDatabase.rawQuery("SELECT key, value FROM app_settings", null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(AppSetting(cursor.getString(0), cursor.getString(1)))
            }
        }
        list
    }

    // ==================== SYNC HELPERS ====================

    suspend fun getUnsyncedSales(userId: String = ""): List<Sale> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Sale>()
        val where = if (userId.isNotEmpty()) "AND user_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(userId) else null
        readableDatabase.rawQuery("SELECT * FROM sales WHERE synced = 0 $where ORDER BY date ASC", args).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toSale())
        }
        list
    }

    suspend fun markSaleSynced(saleId: Long) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply { put("synced", 1) }
        writableDatabase.update("sales", cv, "id = ?", arrayOf(saleId.toString()))
    }

    /**
     * Remplace l'id local d'une vente par l'id généré par Supabase (BIGSERIAL),
     * et met à jour les références (articles vendus, transactions de dette).
     * Évite les doublons lors du pull après une réinstallation ou sur un 2e appareil.
     */
    suspend fun reassignSaleId(oldId: Long, newId: Long) = withContext(Dispatchers.IO) {
        if (oldId <= 0 || newId <= 0 || oldId == newId) return@withContext
        writableDatabase.beginTransaction()
        try {
            writableDatabase.execSQL(
                "UPDATE sale_items SET sale_id = ? WHERE sale_id = ?",
                arrayOf(newId.toString(), oldId.toString())
            )
            writableDatabase.execSQL(
                "UPDATE debt_transactions SET sale_id = ? WHERE sale_id = ?",
                arrayOf(newId.toString(), oldId.toString())
            )
            writableDatabase.execSQL(
                "UPDATE sales SET id = ? WHERE id = ?",
                arrayOf(newId.toString(), oldId.toString())
            )
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    suspend fun saleExists(saleId: Long): Boolean = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT 1 FROM sales WHERE id = ?", arrayOf(saleId.toString())).use { cursor ->
            cursor.moveToFirst()
        }
    }

    suspend fun insertSaleIfNotExists(sale: Sale) = withContext(Dispatchers.IO) {
        if (!saleExists(sale.id)) {
            val cv = ContentValues().apply {
                put("id", sale.id)
                put("date", sale.date)
                put("total", sale.total)
                put("amount_paid", sale.amountPaid)
                put("change_given", sale.changeGiven)
                put("is_credit", if (sale.isCredit) 1 else 0)
                put("client_id", sale.clientId)
                put("synced", 1)
                put("user_id", sale.userId)
            }
            writableDatabase.insert("sales", null, cv)
        }
    }

    suspend fun insertSaleItemIfNotExists(item: SaleItem) = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT 1 FROM sale_items WHERE sale_id = ? AND barcode = ?",
            arrayOf(item.saleId.toString(), item.barcode)).use { cursor ->
            if (!cursor.moveToFirst()) {
                val cv = ContentValues().apply {
                    put("sale_id", item.saleId)
                    put("barcode", item.barcode)
                    put("name", item.name)
                    put("price", item.price)
                    put("quantity", item.quantity)
                    put("user_id", item.userId)
                }
                writableDatabase.insert("sale_items", null, cv)
            }
        }
    }

    suspend fun insertDebtTransactionIfNotExists(txn: DebtTransaction) = withContext(Dispatchers.IO) {
        // Dédoublonnage par clé naturelle (client + montant + date) plutôt que par id :
        // l'id local et l'id Supabase diffèrent (BIGSERIAL), on compare les vraies données.
        readableDatabase.rawQuery(
            "SELECT 1 FROM debt_transactions WHERE client_id = ? AND amount = ? AND date = ?",
            arrayOf(txn.clientId, txn.amount.toString(), txn.date.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                val cv = ContentValues().apply {
                    put("client_id", txn.clientId)
                    if (txn.saleId != null) put("sale_id", txn.saleId)
                    put("amount", txn.amount)
                    put("date", txn.date)
                    put("note", txn.note)
                    put("user_id", txn.userId)
                }
                writableDatabase.insert("debt_transactions", null, cv)
            }
        }
    }
}

// Cursor extensions
private fun Cursor.toProduct() = Product(
    barcode = getString(0),
    name = getString(1),
    sellPrice = getInt(2),
    buyPrice = getInt(3),
    stock = getInt(4),
    minStock = getInt(5),
    category = getString(6),
    hasBarcode = getInt(7) == 1,
    createdAt = getLong(8),
    updatedAt = getLong(9),
    userId = if (columnCount > 10) getString(10) else ""
)

private fun Cursor.toSale() = Sale(
    id = getLong(0),
    date = getLong(1),
    total = getInt(2),
    amountPaid = getInt(3),
    changeGiven = getInt(4),
    isCredit = getInt(5) == 1,
    clientId = getString(6),
    synced = getInt(7) == 1,
    userId = if (columnCount > 8) getString(8) else ""
)

private fun Cursor.toSaleItem() = SaleItem(
    id = getLong(0),
    saleId = getLong(1),
    barcode = getString(2),
    name = getString(3),
    price = getInt(4),
    quantity = getDouble(5),
    userId = if (columnCount > 6) getString(6) else ""
)

private fun Cursor.toClient() = Client(
    id = getString(0),
    name = getString(1),
    phone = getString(2),
    totalDebt = getInt(3),
    createdAt = getLong(4),
    updatedAt = getLong(5),
    userId = if (columnCount > 6) getString(6) else ""
)

private fun Cursor.toDebtTransaction() = DebtTransaction(
    id = getLong(0),
    clientId = getString(1),
    saleId = if (isNull(2)) null else getLong(2),
    amount = getInt(3),
    date = getLong(4),
    note = getString(5),
    userId = if (columnCount > 6) getString(6) else ""
)
