package com.lissafi.app.data.remote

import android.content.Context
import com.lissafi.app.data.entity.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import android.util.Log
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Client REST Supabase (PostgREST).
 *
 * IMPORTANT : chaque méthode lève une [SupabaseException] en cas d'échec
 * (réseau ou réponse non-2xx). Cela permet au SyncManager de détecter les
 * vraies erreurs et de réessayer plus tard — aucune erreur n'est avalée.
 */
class SupabaseApi(private val context: Context) {

    private val http get() = SupabaseManager.getHttpClient()
    private val baseUrl get() = SupabaseManager.getUrl(context)
    private val anonKey get() = SupabaseManager.getAnonKey(context)
    private val token get() = SupabaseManager.getAccessToken(context)
    val isConfigured: Boolean get() = true
    val currentUserId: String get() = SupabaseManager.currentUserId(context) ?: ""

    private fun restUrl(table: String) = "$baseUrl/rest/v1/$table"

    /** Lève une exception si la réponse Supabase n'est pas un succès. */
    private fun ensureSuccess(response: HttpResponse, action: String) {
        if (response.status.value !in 200..299) {
            throw SupabaseException("$action : HTTP ${response.status.value}")
        }
    }

    // ==================== PRODUITS ====================

    suspend fun getAllProducts(): List<Product> = withContext(Dispatchers.IO) {
        val response = http.get(restUrl("products")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            parameter("select", "*")
        }
        ensureSuccess(response, "getAllProducts")
        response.body<List<Product>>()
    }

    suspend fun getProduct(barcode: String): Product? = withContext(Dispatchers.IO) {
        val response = http.get(restUrl("products")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            parameter("barcode", "eq.$barcode")
            parameter("select", "*")
        }
        ensureSuccess(response, "getProduct")
        response.body<List<Product>>().firstOrNull()
    }

    suspend fun upsertProduct(product: Product) = withContext(Dispatchers.IO) {
        val response = http.post(restUrl("products")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            header("Prefer", "resolution=merge-duplicates")
            contentType(ContentType.Application.Json)
            setBody(product)
        }
        ensureSuccess(response, "upsertProduct")
    }

    suspend fun upsertProducts(products: List<Product>) = withContext(Dispatchers.IO) {
        if (products.isEmpty()) return@withContext
        val response = http.post(restUrl("products")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            header("Prefer", "resolution=merge-duplicates")
            contentType(ContentType.Application.Json)
            setBody(products)
        }
        ensureSuccess(response, "upsertProducts")
    }

    suspend fun deleteProduct(barcode: String) = withContext(Dispatchers.IO) {
        val response = http.delete(restUrl("products")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            parameter("barcode", "eq.$barcode")
        }
        ensureSuccess(response, "deleteProduct")
    }

    // ==================== VENTES ====================

    suspend fun insertSale(sale: Sale, items: List<SaleItem>): Long = withContext(Dispatchers.IO) {
        val t = token
        Log.d("LissafiSupabase", "Insert sale - userId: ${sale.userId}, total: ${sale.total}")
        val response = http.post(restUrl("sales")) {
            header("apikey", anonKey)
            t?.let { header("Authorization", "Bearer $it") }
            header("Prefer", "return=representation")
            contentType(ContentType.Application.Json)
            // id = 0 → champ omis par le sérialiseur → le serveur génère l'id (BIGSERIAL)
            setBody(sale.copy(id = 0))
        }
        ensureSuccess(response, "insertSale")
        val inserted = response.body<List<Sale>>().firstOrNull()
        val saleId = inserted?.id ?: 0L
        if (saleId > 0 && items.isNotEmpty()) {
            val itemsWithSaleId = items.map { it.copy(saleId = saleId) }
            val itemsResponse = http.post(restUrl("sale_items")) {
                header("apikey", anonKey)
                token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody(itemsWithSaleId)
            }
            ensureSuccess(itemsResponse, "insertSaleItems")
        }
        saleId
    }

    suspend fun getSalesBetween(start: Long, end: Long): List<Sale> = withContext(Dispatchers.IO) {
        val response = http.get(restUrl("sales")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            parameter("select", "*")
            parameter("date", "gte.$start")
            parameter("date", "lte.$end")
        }
        ensureSuccess(response, "getSalesBetween")
        response.body<List<Sale>>()
    }

    suspend fun getSaleItems(saleId: Long): List<SaleItem> = withContext(Dispatchers.IO) {
        val response = http.get(restUrl("sale_items")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            parameter("sale_id", "eq.$saleId")
            parameter("select", "*")
        }
        ensureSuccess(response, "getSaleItems")
        response.body<List<SaleItem>>()
    }

    // ==================== CLIENTS ====================

    suspend fun getAllClients(): List<Client> = withContext(Dispatchers.IO) {
        val response = http.get(restUrl("clients")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            parameter("select", "*")
        }
        ensureSuccess(response, "getAllClients")
        response.body<List<Client>>()
    }

    suspend fun getClient(id: String): Client? = withContext(Dispatchers.IO) {
        val response = http.get(restUrl("clients")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            parameter("id", "eq.$id")
            parameter("select", "*")
        }
        ensureSuccess(response, "getClient")
        response.body<List<Client>>().firstOrNull()
    }

    suspend fun upsertClient(c: Client) = withContext(Dispatchers.IO) {
        val response = http.post(restUrl("clients")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            header("Prefer", "resolution=merge-duplicates")
            contentType(ContentType.Application.Json)
            setBody(c)
        }
        ensureSuccess(response, "upsertClient")
    }

    suspend fun upsertClients(clients: List<Client>) = withContext(Dispatchers.IO) {
        if (clients.isEmpty()) return@withContext
        val response = http.post(restUrl("clients")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            header("Prefer", "resolution=merge-duplicates")
            contentType(ContentType.Application.Json)
            setBody(clients)
        }
        ensureSuccess(response, "upsertClients")
    }

    // ==================== DETTES ====================

    suspend fun getDebtTransactions(clientId: String): List<DebtTransaction> = withContext(Dispatchers.IO) {
        val response = http.get(restUrl("debt_transactions")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            parameter("client_id", "eq.$clientId")
            parameter("select", "*")
        }
        ensureSuccess(response, "getDebtTransactions")
        response.body<List<DebtTransaction>>()
    }

    suspend fun addDebtTransaction(transaction: DebtTransaction) = withContext(Dispatchers.IO) {
        // Dédoublonnage : si une transaction identique (client + montant + date) existe
        // déjà sur le serveur, on ne la réinsère pas (re-push idempotent).
        val existing = getDebtTransactions(transaction.clientId)
        if (existing.any { it.amount == transaction.amount && it.date == transaction.date }) {
            try {
                refreshClientDebt(transaction.clientId)
            } catch (e: SupabaseException) {
                Log.w("LissafiSupabase", "Refresh dette échoué: ${e.message}")
            }
            return@withContext
        }

        val response = http.post(restUrl("debt_transactions")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            contentType(ContentType.Application.Json)
            // id = 0 → champ omis → le serveur génère l'id (BIGSERIAL)
            setBody(transaction.copy(id = 0))
        }
        ensureSuccess(response, "addDebtTransaction")
        try {
            refreshClientDebt(transaction.clientId)
        } catch (e: SupabaseException) {
            // La transaction est enregistrée ; le calcul de la dette sera refait au prochain sync
            Log.w("LissafiSupabase", "Refresh dette échoué: ${e.message}")
        }
    }

    private suspend fun refreshClientDebt(clientId: String) {
        val txns = getDebtTransactions(clientId)
        val total = txns.sumOf { it.amount }
        val existing = getClient(clientId)
        if (existing != null) {
            upsertClient(existing.copy(totalDebt = total, updatedAt = System.currentTimeMillis()))
        }
    }

    // ==================== PARAMÈTRES ====================

    suspend fun getSetting(key: String): String? = withContext(Dispatchers.IO) {
        val response = http.get(restUrl("app_settings")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            parameter("key", "eq.$key")
            parameter("select", "*")
        }
        ensureSuccess(response, "getSetting")
        response.body<List<AppSetting>>().firstOrNull()?.value
    }

    suspend fun setSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        val response = http.post(restUrl("app_settings")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            header("Prefer", "resolution=merge-duplicates")
            contentType(ContentType.Application.Json)
            setBody(AppSetting(key = key, value = value, userId = currentUserId))
        }
        ensureSuccess(response, "setSetting")
    }

    suspend fun getAllSettings(): List<AppSetting> = withContext(Dispatchers.IO) {
        val response = http.get(restUrl("app_settings")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            parameter("select", "*")
        }
        ensureSuccess(response, "getAllSettings")
        response.body<List<AppSetting>>()
    }
}

class SupabaseException(message: String, cause: Throwable? = null) : Exception(message, cause)
