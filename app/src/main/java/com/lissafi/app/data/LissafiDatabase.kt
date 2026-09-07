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
        // v3 = PK composite (barcode, user_id) sur products + colonne `deleted` (soft delete).
        // Le schéma distant supabase-schema.sql a déjà la PK composite — on aligne la DB locale.
        // v4 = suppression du PIN admin (fonctionnalité retirée, gérée par le back-office) :
        // purge le hash résiduel éventuellement stocké dans app_settings.
        // v5 = colonne `synced` sur debt_transactions (local uniquement, pas mirroré sur
        // Supabase) : évite de repousser tout l'historique des dettes à chaque synchro.
        // v6 = colonne `shop_id` sur les 5 tables métier (offre Grand boutique, boutique
        // partagée). Additif : shop_id = user_id pour l'existant ; les lectures restent
        // filtrées par user_id dans ce lot (bascule sur shop_id au lot suivant).
        const val DATABASE_VERSION = 6

        @Volatile
        private var INSTANCE: LissafiDatabase? = null

        fun getInstance(context: Context): LissafiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LissafiDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // PK composite (barcode, user_id) : deux comptes peuvent avoir le même EAN.
        // `deleted` (soft delete) : la suppression est propagée à Supabase, pas perdue.
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS products (
                barcode TEXT NOT NULL,
                name TEXT NOT NULL,
                sell_price INTEGER NOT NULL DEFAULT 0,
                buy_price INTEGER NOT NULL DEFAULT 0,
                stock INTEGER NOT NULL DEFAULT 0,
                min_stock INTEGER NOT NULL DEFAULT 5,
                category TEXT NOT NULL DEFAULT '',
                has_barcode INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                user_id TEXT NOT NULL DEFAULT '',
                deleted INTEGER NOT NULL DEFAULT 0,
                shop_id TEXT NOT NULL DEFAULT '',
                PRIMARY KEY (barcode, user_id)
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
                user_id TEXT NOT NULL DEFAULT '',
                shop_id TEXT NOT NULL DEFAULT ''
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
                user_id TEXT NOT NULL DEFAULT '',
                shop_id TEXT NOT NULL DEFAULT ''
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
                user_id TEXT NOT NULL DEFAULT '',
                shop_id TEXT NOT NULL DEFAULT ''
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
                user_id TEXT NOT NULL DEFAULT '',
                synced INTEGER NOT NULL DEFAULT 0,
                shop_id TEXT NOT NULL DEFAULT ''
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

        // V2 → V3 : PK composite (barcode, user_id) + colonne deleted sur products.
        // SQLite ne permet pas de modifier la PK : on recrée la table (pattern standard).
        if (oldVersion < 3) {
            db.execSQL("""
                CREATE TABLE products_v3 (
                    barcode TEXT NOT NULL,
                    name TEXT NOT NULL,
                    sell_price INTEGER NOT NULL DEFAULT 0,
                    buy_price INTEGER NOT NULL DEFAULT 0,
                    stock INTEGER NOT NULL DEFAULT 0,
                    min_stock INTEGER NOT NULL DEFAULT 5,
                    category TEXT NOT NULL DEFAULT '',
                    has_barcode INTEGER NOT NULL DEFAULT 1,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    user_id TEXT NOT NULL DEFAULT '',
                    deleted INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (barcode, user_id)
                )
            """)
            db.execSQL("""
                INSERT INTO products_v3
                    (barcode, name, sell_price, buy_price, stock, min_stock, category,
                     has_barcode, created_at, updated_at, user_id, deleted)
                SELECT barcode, name, sell_price, buy_price, stock, min_stock, category,
                       has_barcode, created_at, updated_at, user_id, 0
                FROM products
            """)
            db.execSQL("DROP TABLE products")
            db.execSQL("ALTER TABLE products_v3 RENAME TO products")
        }

        // V3 → V4 : suppression du PIN admin, purge du hash résiduel.
        if (oldVersion < 4) {
            try { db.execSQL("DELETE FROM app_settings WHERE key = 'admin_pin'") } catch (_: Exception) {}
        }

        // V4 → V5 : colonne `synced` sur debt_transactions. Les transactions déjà
        // présentes ont forcément déjà été poussées par l'ancien mécanisme exhaustif
        // (qui repoussait tout l'historique à chaque cycle) : on les marque synced=1
        // pour éviter un re-push massif au premier cycle post-migration.
        if (oldVersion < 5) {
            try { db.execSQL("ALTER TABLE debt_transactions ADD COLUMN synced INTEGER NOT NULL DEFAULT 0") } catch (_: Exception) {}
            try { db.execSQL("UPDATE debt_transactions SET synced = 1") } catch (_: Exception) {}
        }

        // V5 → V6 : colonne shop_id (boutique partagée, offre Grand boutique).
        // Additif : chaque compte devient sa boutique solo → shop_id = user_id pour l'existant.
        // Les lectures restent filtrées par user_id dans ce lot (bascule sur shop_id au lot suivant).
        if (oldVersion < 6) {
            for (t in listOf("products", "sales", "sale_items", "clients", "debt_transactions")) {
                try { db.execSQL("ALTER TABLE $t ADD COLUMN shop_id TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("UPDATE $t SET shop_id = user_id WHERE shop_id = ''") } catch (_: Exception) {}
            }
        }
    }

    // ==================== PRODUCTS ====================
    // NB (Grand boutique) : le paramètre `userId` des méthodes de lecture porte
    // désormais l'ID DE BOUTIQUE (shop_id). Pour un compte solo, shop_id = user_id,
    // donc le comportement est identique. Le repository passe `currentShopId`.

    private val _productsFlow = MutableStateFlow<List<Product>>(emptyList())
    val productsFlow: Flow<List<Product>> = _productsFlow.asStateFlow()

    suspend fun getAllProducts(userId: String = ""): List<Product> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Product>()
        val where = if (userId.isNotEmpty()) "WHERE deleted = 0 AND shop_id = ?" else "WHERE deleted = 0"
        val args = if (userId.isNotEmpty()) arrayOf(userId) else null
        readableDatabase.rawQuery("SELECT * FROM products $where ORDER BY updated_at DESC", args).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toProduct())
        }
        _productsFlow.value = list
        list
    }

    /** Tous les produits, y compris supprimés — utilisé pour pousser l'état complet vers Supabase. */
    suspend fun getAllProductsIncludingDeleted(userId: String = ""): List<Product> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Product>()
        val where = if (userId.isNotEmpty()) "WHERE shop_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(userId) else null
        readableDatabase.rawQuery("SELECT * FROM products $where ORDER BY updated_at DESC", args).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toProduct())
        }
        list
    }

    suspend fun getProduct(barcode: String, userId: String = ""): Product? = withContext(Dispatchers.IO) {
        val where = if (userId.isNotEmpty()) " AND shop_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(barcode, userId) else arrayOf(barcode)
        readableDatabase.rawQuery("SELECT * FROM products WHERE barcode = ? AND deleted = 0$where", args).use { cursor ->
            if (cursor.moveToFirst()) cursor.toProduct() else null
        }
    }

    suspend fun searchProducts(query: String, userId: String = ""): List<Product> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Product>()
        val where = if (userId.isNotEmpty()) "AND shop_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf("%$query%", userId) else arrayOf("%$query%")
        readableDatabase.rawQuery("SELECT * FROM products WHERE name LIKE ? AND deleted = 0 $where ORDER BY name ASC", args).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toProduct())
        }
        list
    }

    suspend fun getProductCount(userId: String = ""): Int = withContext(Dispatchers.IO) {
        val where = if (userId.isNotEmpty()) "WHERE deleted = 0 AND shop_id = ?" else "WHERE deleted = 0"
        val args = if (userId.isNotEmpty()) arrayOf(userId) else null
        readableDatabase.rawQuery("SELECT COUNT(*) FROM products $where", args).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    suspend fun getRecentProducts(limit: Int = 8, userId: String = ""): List<Product> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Product>()
        val where = if (userId.isNotEmpty()) "WHERE deleted = 0 AND shop_id = ?" else "WHERE deleted = 0"
        val args = if (userId.isNotEmpty()) arrayOf(userId, limit.toString()) else arrayOf(limit.toString())
        readableDatabase.rawQuery("SELECT * FROM products $where ORDER BY updated_at DESC LIMIT ?", args).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toProduct())
        }
        list
    }

    /** Produits dont le stock est descendu au niveau ou en dessous du seuil d'alerte. */
    suspend fun getLowStockProducts(userId: String = ""): List<Product> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Product>()
        val where = if (userId.isNotEmpty()) "AND shop_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(userId) else null
        readableDatabase.rawQuery(
            "SELECT * FROM products WHERE deleted = 0 AND stock <= min_stock $where ORDER BY stock ASC",
            args
        ).use { cursor ->
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
            put("deleted", if (product.deleted) 1 else 0)
            put("shop_id", product.shopId.ifBlank { product.userId })
        }
        writableDatabase.insertWithOnConflict("products", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        getAllProducts(product.shopId.ifBlank { product.userId })
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        // Soft delete : le flag `deleted` est poussé vers Supabase, pour que la
        // suppression survive à une coupure réseau et soit propagée aux autres appareils.
        val cv = ContentValues().apply {
            put("deleted", 1)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.update(
            "products", cv,
            "barcode = ? AND shop_id = ?",
            arrayOf(product.barcode, product.shopId.ifBlank { product.userId })
        )
        getAllProducts(product.shopId.ifBlank { product.userId })
    }

    // ==================== SALES ====================

    suspend fun insertSale(sale: Sale, items: List<SaleItem>): Long = withContext(Dispatchers.IO) {
        val db = writableDatabase
        // Transaction : la vente et ses articles sont insérés atomiquement.
        // Un crash entre les deux laisserait une vente sans articles, poussée
        // telle quelle sur Supabase (données corrompues).
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put("date", sale.date)
                put("total", sale.total)
                put("amount_paid", sale.amountPaid)
                put("change_given", sale.changeGiven)
                put("is_credit", if (sale.isCredit) 1 else 0)
                put("client_id", sale.clientId)
                put("synced", if (sale.synced) 1 else 0)
                put("user_id", sale.userId)
                put("shop_id", sale.shopId.ifBlank { sale.userId })
            }
            val saleId = db.insert("sales", null, cv)
            for (item in items) {
                val itemCv = ContentValues().apply {
                    put("sale_id", saleId)
                    put("barcode", item.barcode)
                    put("name", item.name)
                    put("price", item.price)
                    put("quantity", item.quantity)
                    put("user_id", item.userId)
                    put("shop_id", item.shopId.ifBlank { item.userId })
                }
                db.insert("sale_items", null, itemCv)
            }
            db.setTransactionSuccessful()
            saleId
        } finally {
            db.endTransaction()
        }
    }

    suspend fun getSalesBetween(start: Long, end: Long, userId: String = ""): List<Sale> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Sale>()
        val where = if (userId.isNotEmpty()) "AND shop_id = ?" else ""
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
        val where = if (userId.isNotEmpty()) "AND s.shop_id = ?" else ""
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

    /** Élément du journal d'activité (Grand boutique) : vente ou mouvement de crédit. */
    data class ActivityItem(val type: String, val amount: Int, val date: Long, val userId: String)

    /**
     * Journal d'activité de la boutique : ventes + mouvements de crédit, triés du plus
     * récent au plus ancien. Lecture seule, construit à partir des données existantes
     * (aucune table dédiée). Sert au patron pour suivre l'activité de chaque caisse.
     */
    suspend fun getActivityLog(start: Long, end: Long, shopId: String = "", limit: Int = 200): List<ActivityItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ActivityItem>()
        val shopFilter = if (shopId.isNotEmpty()) "AND shop_id = ?" else ""
        val sql = """
            SELECT type, amount, date, user_id FROM (
                SELECT 'vente' AS type, total AS amount, date, user_id FROM sales
                    WHERE date >= ? AND date < ? $shopFilter
                UNION ALL
                SELECT 'credit' AS type, amount, date, user_id FROM debt_transactions
                    WHERE date >= ? AND date < ? $shopFilter
            ) ORDER BY date DESC LIMIT ?
        """
        val args = if (shopId.isNotEmpty())
            arrayOf(start.toString(), end.toString(), shopId, start.toString(), end.toString(), shopId, limit.toString())
        else
            arrayOf(start.toString(), end.toString(), start.toString(), end.toString(), limit.toString())
        readableDatabase.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) {
                list.add(ActivityItem(c.getString(0), c.getInt(1), c.getLong(2), c.getString(3) ?: ""))
            }
        }
        list
    }

    /** CA + nombre de ventes agrégés par caisse (auteur = user_id) sur la boutique. */
    data class SellerTotal(val userId: String, val total: Int, val count: Int)

    suspend fun getSellerTotals(start: Long, end: Long, shopId: String = ""): List<SellerTotal> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SellerTotal>()
        val where = if (shopId.isNotEmpty()) "AND shop_id = ?" else ""
        val args = if (shopId.isNotEmpty()) arrayOf(start.toString(), end.toString(), shopId) else arrayOf(start.toString(), end.toString())
        readableDatabase.rawQuery(
            "SELECT user_id, COALESCE(SUM(total), 0) AS t, COUNT(*) AS c FROM sales WHERE date >= ? AND date < ? $where GROUP BY user_id ORDER BY t DESC",
            args
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(SellerTotal(cursor.getString(0) ?: "", cursor.getInt(1), cursor.getInt(2)))
            }
        }
        list
    }

    suspend fun countSalesBetween(start: Long, end: Long, userId: String = ""): Int = withContext(Dispatchers.IO) {
        val where = if (userId.isNotEmpty()) "AND shop_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(start.toString(), end.toString(), userId) else arrayOf(start.toString(), end.toString())
        readableDatabase.rawQuery("SELECT COUNT(*) FROM sales WHERE date >= ? AND date < ? $where", args).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    suspend fun sumTotalBetween(start: Long, end: Long, userId: String = ""): Int = withContext(Dispatchers.IO) {
        val where = if (userId.isNotEmpty()) "AND shop_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(start.toString(), end.toString(), userId) else arrayOf(start.toString(), end.toString())
        readableDatabase.rawQuery("SELECT COALESCE(SUM(total), 0) FROM sales WHERE date >= ? AND date < ? $where", args).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    suspend fun sumCreditBetween(start: Long, end: Long, userId: String = ""): Int = withContext(Dispatchers.IO) {
        val where = if (userId.isNotEmpty()) "AND shop_id = ?" else ""
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
        val where = if (userId.isNotEmpty()) "WHERE shop_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(userId) else null
        readableDatabase.rawQuery("SELECT * FROM clients $where ORDER BY total_debt DESC", args).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toClient())
        }
        _clientsFlow.value = list
        list
    }

    suspend fun getClient(id: String, userId: String = ""): Client? = withContext(Dispatchers.IO) {
        val where = if (userId.isNotEmpty()) " AND shop_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(id, userId) else arrayOf(id)
        readableDatabase.rawQuery("SELECT * FROM clients WHERE id = ?$where", args).use { cursor ->
            if (cursor.moveToFirst()) cursor.toClient() else null
        }
    }

    suspend fun searchClients(query: String, userId: String = ""): List<Client> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Client>()
        val where = if (userId.isNotEmpty()) "AND shop_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf("%$query%", userId) else arrayOf("%$query%")
        readableDatabase.rawQuery("SELECT * FROM clients WHERE name LIKE ? $where ORDER BY name ASC", args).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toClient())
        }
        list
    }

    suspend fun getClientCount(userId: String = ""): Int = withContext(Dispatchers.IO) {
        val where = if (userId.isNotEmpty()) "WHERE shop_id = ?" else ""
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
            put("shop_id", client.shopId.ifBlank { client.userId })
        }
        writableDatabase.insertWithOnConflict("clients", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        getAllClients(client.shopId.ifBlank { client.userId })
    }

    suspend fun updateClient(client: Client) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("name", client.name)
            put("phone", client.phone)
            put("total_debt", client.totalDebt)
            put("updated_at", client.updatedAt)
            put("user_id", client.userId)
            put("shop_id", client.shopId.ifBlank { client.userId })
        }
        writableDatabase.update("clients", cv, "id = ?", arrayOf(client.id))
        getAllClients(client.shopId.ifBlank { client.userId })
    }

    // ==================== DEBT TRANSACTIONS ====================

    suspend fun getDebtTransactions(clientId: String, userId: String = ""): List<DebtTransaction> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DebtTransaction>()
        val where = if (userId.isNotEmpty()) " AND shop_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(clientId, userId) else arrayOf(clientId)
        readableDatabase.rawQuery("SELECT * FROM debt_transactions WHERE client_id = ?$where ORDER BY date DESC", args).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toDebtTransaction())
        }
        list
    }

    suspend fun addDebtTransaction(transaction: DebtTransaction) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        // Transaction : l'INSERT + le recalcul de total_debt sont atomiques
        // (deux appels concurrents ne peuvent pas écraser total_debt avec une somme partielle).
        db.beginTransaction()
        try {
            val sid = transaction.shopId.ifBlank { transaction.userId }
            val cv = ContentValues().apply {
                put("client_id", transaction.clientId)
                if (transaction.saleId != null) put("sale_id", transaction.saleId)
                put("amount", transaction.amount)
                put("date", transaction.date)
                put("note", transaction.note)
                put("user_id", transaction.userId)
                put("shop_id", sid)
            }
            db.insert("debt_transactions", null, cv)

            // Recalcul du total de la dette AU NIVEAU BOUTIQUE (toutes les caisses),
            // pas seulement les transactions de l'auteur.
            val where = if (sid.isNotEmpty()) " AND shop_id = ?" else ""
            val args = if (sid.isNotEmpty()) arrayOf(transaction.clientId, sid) else arrayOf(transaction.clientId)
            db.rawQuery("SELECT COALESCE(SUM(amount), 0) FROM debt_transactions WHERE client_id = ?$where", args).use { cursor ->
                if (cursor.moveToFirst()) {
                    val total = cursor.getInt(0)
                    val cv2 = ContentValues().apply {
                        put("total_debt", total)
                        put("updated_at", System.currentTimeMillis())
                    }
                    db.update("clients", cv2, "id = ?", arrayOf(transaction.clientId))
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        getAllClients(transaction.shopId.ifBlank { transaction.userId })
    }

    /** Transactions de dette pas encore poussées vers Supabase (voir SYNC HELPERS pour les ventes). */
    suspend fun getUnsyncedDebtTransactions(userId: String = ""): List<DebtTransaction> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DebtTransaction>()
        val where = if (userId.isNotEmpty()) "AND shop_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(userId) else null
        readableDatabase.rawQuery("SELECT * FROM debt_transactions WHERE synced = 0 $where ORDER BY date ASC", args).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toDebtTransaction())
        }
        list
    }

    suspend fun markDebtTransactionSynced(id: Long) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply { put("synced", 1) }
        writableDatabase.update("debt_transactions", cv, "id = ?", arrayOf(id.toString()))
    }

    suspend fun getTotalDebt(clientId: String, userId: String = ""): Int = withContext(Dispatchers.IO) {
        val where = if (userId.isNotEmpty()) " AND shop_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(clientId, userId) else arrayOf(clientId)
        readableDatabase.rawQuery("SELECT COALESCE(SUM(amount), 0) FROM debt_transactions WHERE client_id = ?$where", args).use { cursor ->
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
        val where = if (userId.isNotEmpty()) "AND shop_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(userId) else null
        readableDatabase.rawQuery("SELECT * FROM sales WHERE synced = 0 $where ORDER BY date ASC", args).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toSale())
        }
        list
    }

    /**
     * Remplace l'id local d'une vente par l'id généré par Supabase (BIGSERIAL),
     * met à jour les références (articles vendus, transactions de dette), et marque
     * la vente comme synchronisée — le TOUT dans une seule transaction SQLite.
     *
     * L'atomicité est essentielle : si ces étapes étaient séparées et que le process
     * était tué entre la ré-assignation d'id et le marquage `synced=1`, la vente
     * garderait `synced=0` avec un id déjà distant → le prochain cycle de synchro la
     * repousserait vers Supabase, qui génère un NOUVEL id (POST forcé à id=0), créant
     * une vente en double côté serveur (argent compté deux fois dans les rapports).
     *
     * Si `newId` est déjà occupé par une AUTRE vente locale (cas d'un 2e appareil
     * qui a généré localement le même id AUTOINCREMENT), cette vente est d'abord
     * déplacée vers un id libre pour libérer `newId`, sinon l'UPDATE violerait
     * la PRIMARY KEY et planterait la synchro.
     */
    suspend fun finalizeSalePush(oldId: Long, newId: Long) = withContext(Dispatchers.IO) {
        if (newId <= 0) return@withContext
        writableDatabase.beginTransaction()
        try {
            val db = writableDatabase
            if (oldId > 0 && oldId != newId) {
                // Une autre vente occupe-t-elle déjà newId ?
                val occupied = readableDatabase.rawQuery(
                    "SELECT 1 FROM sales WHERE id = ? AND id != ?",
                    arrayOf(newId.toString(), oldId.toString())
                ).use { it.moveToFirst() }

                if (occupied) {
                    // Trouver un id libre pour déplacer la vente qui occupe newId.
                    val maxId = readableDatabase.rawQuery(
                        "SELECT COALESCE(MAX(id), 0) FROM sales", null
                    ).use { if (it.moveToFirst()) it.getLong(0) else newId }
                    val freeId = maxOf(maxId + 1, newId + 1)
                    db.execSQL(
                        "UPDATE sale_items SET sale_id = ? WHERE sale_id = ?",
                        arrayOf(freeId.toString(), newId.toString())
                    )
                    db.execSQL(
                        "UPDATE debt_transactions SET sale_id = ? WHERE sale_id = ?",
                        arrayOf(freeId.toString(), newId.toString())
                    )
                    db.execSQL(
                        "UPDATE sales SET id = ? WHERE id = ?",
                        arrayOf(freeId.toString(), newId.toString())
                    )
                }

                db.execSQL(
                    "UPDATE sale_items SET sale_id = ? WHERE sale_id = ?",
                    arrayOf(newId.toString(), oldId.toString())
                )
                db.execSQL(
                    "UPDATE debt_transactions SET sale_id = ? WHERE sale_id = ?",
                    arrayOf(newId.toString(), oldId.toString())
                )
                db.execSQL(
                    "UPDATE sales SET id = ? WHERE id = ?",
                    arrayOf(newId.toString(), oldId.toString())
                )
            }

            db.execSQL("UPDATE sales SET synced = 1 WHERE id = ?", arrayOf(newId.toString()))
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
                put("shop_id", sale.shopId.ifBlank { sale.userId })
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
                    put("shop_id", item.shopId.ifBlank { item.userId })
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
                    put("shop_id", txn.shopId.ifBlank { txn.userId })
                    put("synced", 1) // vient du serveur, déjà synchronisée
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
    userId = if (columnCount > 10) getString(10) else "",
    deleted = if (columnCount > 11) getInt(11) == 1 else false,
    shopId = if (columnCount > 12) getString(12) else ""
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
    userId = if (columnCount > 8) getString(8) else "",
    shopId = if (columnCount > 9) getString(9) else ""
)

private fun Cursor.toSaleItem() = SaleItem(
    id = getLong(0),
    saleId = getLong(1),
    barcode = getString(2),
    name = getString(3),
    price = getInt(4),
    quantity = getDouble(5),
    userId = if (columnCount > 6) getString(6) else "",
    shopId = if (columnCount > 7) getString(7) else ""
)

private fun Cursor.toClient() = Client(
    id = getString(0),
    name = getString(1),
    phone = getString(2),
    totalDebt = getInt(3),
    createdAt = getLong(4),
    updatedAt = getLong(5),
    userId = if (columnCount > 6) getString(6) else "",
    shopId = if (columnCount > 7) getString(7) else ""
)

private fun Cursor.toDebtTransaction() = DebtTransaction(
    id = getLong(0),
    clientId = getString(1),
    saleId = if (isNull(2)) null else getLong(2),
    amount = getInt(3),
    date = getLong(4),
    note = getString(5),
    userId = if (columnCount > 6) getString(6) else "",
    // synced est en colonne 7 (non mappé) ; shop_id est la dernière colonne.
    shopId = if (columnCount > 8) getString(8) else ""
)
