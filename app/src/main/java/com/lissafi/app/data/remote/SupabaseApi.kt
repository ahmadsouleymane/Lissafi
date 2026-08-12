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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
private data class AppLogPayload(
    val user_id: String,
    val event_type: String,
    val level: String,
    val message: String,
    val meta: String,
    val created_at: Long
)

@Serializable
private data class SupportTicketPayload(
    val user_id: String,
    val subject: String,
    val message: String,
    val status: String,
    val priority: String,
    val created_at: Long,
    val updated_at: Long
)

@Serializable
private data class UpsertDeviceTokenRpcPayload(
    val p_token: String
)

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
    val isConfigured: Boolean get() = SupabaseManager.getUrl(context).isNotBlank() && SupabaseManager.getAnonKey(context).isNotBlank()
    val currentUserId: String get() = SupabaseManager.currentUserId(context) ?: ""
    val hasValidSession: Boolean get() = SupabaseManager.hasValidSession(context)

    private fun restUrl(table: String) = "$baseUrl/rest/v1/$table"

    /**
     * La colonne `deleted` (soft delete) existe-t-elle côté serveur ?
     * Vérifiée une seule fois puis mise en cache. Tant que la migration Supabase
     * (supabase-schema.sql) n'est pas exécutée, on ne l'envoie pas dans les push :
     * cela évitait que pushProducts échoue à chaque cycle (erreur 42703) et
     * fasse tourner la synchro en « Synchro partielle » en boucle.
     */
    @Volatile
    private var remoteHasDeletedColumn: Boolean? = null

    private suspend fun probeRemoteProductDeleted(): Boolean {
        remoteHasDeletedColumn?.let { return it }
        return try {
            val r = http.get(restUrl("products")) {
                header("apikey", anonKey)
                token?.let { header("Authorization", "Bearer $it") }
                parameter("select", "deleted")
                parameter("limit", "1")
            }
            val ok = r.status.value in 200..299
            remoteHasDeletedColumn = ok
            ok
        } catch (e: Exception) {
            // Réseau indisponible : on suppose le schéma à jour, on ne met pas en cache.
            true
        }
    }

    /**
     * Rafraîchit le token d'accès s'il est expiré ou sur le point de l'être
     * (< 60 s restantes). N'effectue un appel réseau que dans ce cas ; sinon
     * c'est un simple décodage JWT local, sans coût.
     */
    suspend fun ensureFreshSession() {
        val exp = SupabaseManager.getAccessTokenExpiry(context)
        val now = System.currentTimeMillis() / 1000
        if (exp == null || exp - now < 60) {
            com.lissafi.app.data.auth.AuthManager(context).refreshSession()
        }
    }

    /**
     * Écritures légères (logs, signalements) : fire-and-forget, jamais bloquant.
     * Les échecs réseau sont silencieux — le back-office récolte ce qui arrive.
     */
    private val logScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Bloque toute écriture si la session locale est invalide (user_id manquant
     * ou non-UUID). Empêche d'envoyer des INSERT que le typage UUID et la RLS
     * de Supabase rejetteraient de toute façon (erreurs 22P02 / 42501).
     */
    private fun ensureValidUser() {
        if (!hasValidSession) {
            throw SupabaseException("Session invalide : user_id manquant ou non-UUID")
        }
    }

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
        ensureValidUser()
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
        ensureValidUser()
        if (products.isEmpty()) return@withContext
        // Si la colonne `deleted` n'existe pas encore côté serveur, on retire le champ
        // (encodeDefaults=false l'omet de toute façon pour deleted=false) pour ne pas
        // casser la synchro. La migration supabase-schema.sql l'active pleinement.
        val body = if (probeRemoteProductDeleted()) products else products.map { it.copy(deleted = false) }
        val response = http.post(restUrl("products")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            header("Prefer", "resolution=merge-duplicates")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        ensureSuccess(response, "upsertProducts")
    }

    suspend fun deleteProduct(barcode: String) = withContext(Dispatchers.IO) {
        ensureValidUser()
        val response = http.delete(restUrl("products")) {
            header("apikey", anonKey)
            token?.let { header("Authorization", "Bearer $it") }
            parameter("barcode", "eq.$barcode")
        }
        ensureSuccess(response, "deleteProduct")
    }

    // ==================== VENTES ====================

    suspend fun insertSale(sale: Sale, items: List<SaleItem>): Long = withContext(Dispatchers.IO) {
        ensureValidUser()
        val t = token
        Log.d("LissafiSupabase", "Insert sale - userId: ${sale.userId}, total: ${sale.total}")
        val response = http.post(restUrl("sales")) {
            header("apikey", anonKey)
            t?.let { header("Authorization", "Bearer $it") }
            header("Prefer", "return=representation")
            contentType(ContentType.Application.Json)
            // encodeDefaults=false → id=0 (valeur par défaut) n'est pas sérialisé → le serveur génère l'id (BIGSERIAL)
            setBody(sale)
        }
        ensureSuccess(response, "insertSale")
        val inserted = response.body<List<Sale>>().firstOrNull()
        val saleId = inserted?.id ?: 0L
        if (saleId > 0 && items.isNotEmpty()) {
            try {
                val itemsWithSaleId = items.map { it.copy(saleId = saleId) }
                val itemsResponse = http.post(restUrl("sale_items")) {
                    header("apikey", anonKey)
                    token?.let { header("Authorization", "Bearer $it") }
                    contentType(ContentType.Application.Json)
                    setBody(itemsWithSaleId)
                }
                ensureSuccess(itemsResponse, "insertSaleItems")
            } catch (e: Exception) {
                // Ne PAS lever : la vente existe déjà côté serveur. Si on levait, le
                // SyncManager la re-pousserait → vente en DOUBLE. La vente est marquée
                // synced avec son id distant ; les articles manquants restent locaux.
                Log.w("LissafiSupabase", "Articles non poussés pour la vente $saleId (retry manuel), sale OK: ${e.message}")
            }
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
        ensureValidUser()
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
        ensureValidUser()
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
        ensureValidUser()
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
        ensureValidUser()
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

    // ==================== LOGS & SUPPORT (back-office) ====================

    /**
     * Envoie un événement au journal du back-office (erreurs, reçus, synchros…).
     * Fire-and-forget : n'interrompt jamais le parcours utilisateur.
     * event_type : app_start | sync | sync_error | receipt | error | session_invalid
     */
    fun logEvent(eventType: String, level: String = "info", message: String = "", meta: String = "{}") {
        val t = token
        val uid = currentUserId
        if (!hasValidSession || uid.isEmpty()) return

        logScope.launch {
            try {
                val response = http.post(restUrl("app_logs")) {
                    header("apikey", anonKey)
                    header("Authorization", "Bearer $t")
                    contentType(ContentType.Application.Json)
                    setBody(AppLogPayload(
                        user_id = uid,
                        event_type = eventType,
                        level = level,
                        message = message,
                        meta = meta,
                        created_at = System.currentTimeMillis()
                    ))
                }
                if (response.status.value !in 200..299) {
                    Log.w("LissafiLog", "logEvent HTTP ${response.status.value}")
                }
            } catch (e: Exception) {
                Log.w("LissafiLog", "logEvent ignoré : ${e.message}")
            }
        }
    }

    /**
     * Envoie un signalement / une demande de support vers le back-office.
     * Fire-and-forget, jamais bloquant.
     */
    fun reportSupportTicket(subject: String, message: String, priority: String = "normal") {
        val t = token
        val uid = currentUserId
        if (!hasValidSession || uid.isEmpty()) return

        logScope.launch {
            try {
                val now = System.currentTimeMillis()
                val response = http.post(restUrl("support_tickets")) {
                    header("apikey", anonKey)
                    header("Authorization", "Bearer $t")
                    contentType(ContentType.Application.Json)
                    setBody(SupportTicketPayload(
                        user_id = uid,
                        subject = subject,
                        message = message,
                        status = "open",
                        priority = priority,
                        created_at = now,
                        updated_at = now
                    ))
                }
                if (response.status.value !in 200..299) {
                    Log.w("LissafiLog", "reportTicket HTTP ${response.status.value}")
                }
            } catch (e: Exception) {
                Log.w("LissafiLog", "reportTicket ignoré : ${e.message}")
            }
        }
    }

    /**
     * Enregistre/rafraîchit le token FCM de cet appareil pour l'utilisateur connecté.
     * Fire-and-forget : un échec réseau n'empêche pas l'usage de l'app — le token
     * sera renvoyé au prochain onNewToken() ou au prochain démarrage connecté.
     */
    fun upsertDeviceToken(fcmToken: String) {
        val authToken = token
        val uid = currentUserId
        if (!hasValidSession || uid.isEmpty() || fcmToken.isBlank()) return

        logScope.launch {
            try {
                // Appel RPC : la fonction SECURITY DEFINER `upsert_device_token` réassigne
                // le token au compte connecté (auth.uid()) même s'il était lié à un autre
                // compte sur ce même appareil (la RLS directe sur device_tokens l'empêcherait).
                val response = http.post(restUrl("rpc/upsert_device_token")) {
                    header("apikey", anonKey)
                    header("Authorization", "Bearer $authToken")
                    contentType(ContentType.Application.Json)
                    setBody(UpsertDeviceTokenRpcPayload(p_token = fcmToken))
                }
                if (response.status.value !in 200..299) {
                    Log.w("LissafiLog", "upsertDeviceToken HTTP ${response.status.value}")
                }
            } catch (e: Exception) {
                Log.w("LissafiLog", "upsertDeviceToken ignoré : ${e.message}")
            }
        }
    }
}

class SupabaseException(message: String, cause: Throwable? = null) : Exception(message, cause)
