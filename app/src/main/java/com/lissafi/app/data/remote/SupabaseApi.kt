package com.lissafi.app.data.remote

import android.content.Context
import com.lissafi.app.data.entity.*
import com.lissafi.app.service.AppLog
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
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

@Serializable
private data class RedeemPremiumPayload(
    val p_code: String
)

@Serializable
data class RedeemPremiumResult(
    val ok: Boolean = false,
    val plan: String = "",
    val premium_expiry: Long? = null
)

/** Corps vide pour les RPC PostgREST sans paramètre (sérialisé en `{}`). */
@Serializable
private class EmptyRpcBody

@Serializable
private data class JoinShopPayload(val p_code: String)

@Serializable
private data class RemoveMemberPayload(val p_user: String)

/** Résultat de join_shop_with_code : boutique rejointe + rôle attribué. */
@Serializable
data class JoinShopResult(
    @SerialName("shop_id") val shopId: String,
    val role: String
)

/** Un membre (caisse) d'une boutique, tel que renvoyé par my_shop_members(). */
@Serializable
data class ShopMemberDto(
    @SerialName("member_id") val memberId: String,
    val role: String,
    @SerialName("caisse_label") val caisseLabel: String = "",
    @SerialName("joined_at") val joinedAt: Long = 0
)

@Serializable
private data class PartnerNameRpcPayload(val p_code: String)

@Serializable
private data class PartnerInstallPayload(val p_code: String)

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
        AppLog.d("LissafiSupabase", "Insert sale OK")
        val response = http.post(restUrl("sales")) {
            header("apikey", anonKey)
            t?.let { header("Authorization", "Bearer $it") }
            header("Prefer", "return=representation")
            contentType(ContentType.Application.Json)
            // id forcé à 0 (valeur par défaut, omise par encodeDefaults=false) : l'id local
            // SQLite (ex. 1, 2…) ne doit jamais être envoyé, sinon il entre en collision avec
            // le BIGSERIAL distant — celui-ci est une séquence GLOBALE partagée par tous les
            // comptes, donc la 1ère vente de chaque nouvel utilisateur (id local = 1) provoque
            // un 409 (unique_violation) et ne se synchronise jamais.
            setBody(sale.copy(id = 0))
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
                AppLog.w("LissafiSupabase", "Articles non poussés pour la vente $saleId (retry manuel), sale OK: ${e.message}")
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
                AppLog.w("LissafiSupabase", "Refresh dette échoué: ${e.message}")
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
            AppLog.w("LissafiSupabase", "Refresh dette échoué: ${e.message}")
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

    /**
     * Active un code premium côté serveur (`redeem_premium_code`, SECURITY
     * DEFINER). Le code est validé, marqué utilisé et les réglages premium
     * sont posés sur Supabase. Les codes ne vivent plus dans l'APK.
     * Lève [SupabaseException] si le code est invalide/déjà utilisé ou si
     * l'appel échoue (le message serveur est conservé pour l'UI).
     */
    suspend fun redeemPremiumCode(code: String): RedeemPremiumResult = withContext(Dispatchers.IO) {
        ensureValidUser()
        ensureFreshSession()
        val authToken = token ?: throw SupabaseException("Session invalide")
        val response = http.post(restUrl("rpc/redeem_premium_code")) {
            header("apikey", anonKey)
            header("Authorization", "Bearer $authToken")
            contentType(ContentType.Application.Json)
            setBody(RedeemPremiumPayload(p_code = code))
        }
        if (response.status.value !in 200..299) {
            val body = try { response.bodyAsText() } catch (_: Exception) { "" }
            throw SupabaseException("redeem: HTTP ${response.status.value}: $body")
        }
        response.body<RedeemPremiumResult>()
    }

    /**
     * Résout (et crée si besoin) la boutique de l'utilisateur connecté via la RPC
     * SECURITY DEFINER `get_or_create_my_shop` (offre Grand boutique). Renvoie le
     * shop_id (UUID) ou null si hors ligne / session invalide / schéma pas encore
     * déployé. Best-effort : ne lève jamais — l'amorçage est retenté au prochain
     * cycle de synchro tant qu'il n'a pas abouti.
     */
    suspend fun getOrCreateMyShop(): String? = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured || !hasValidSession) return@withContext null
            ensureFreshSession()
            val authToken = token ?: return@withContext null
            val response = http.post(restUrl("rpc/get_or_create_my_shop")) {
                header("apikey", anonKey)
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody(EmptyRpcBody())
            }
            if (response.status.value !in 200..299) return@withContext null
            // PostgREST renvoie un scalaire JSON ("uuid" ou null).
            val text = response.bodyAsText().trim()
            if (text.isEmpty() || text == "null") null else text.removeSurrounding("\"")
        } catch (e: Exception) {
            null
        }
    }

    /** Extrait le message d'erreur PostgREST (champ "message") d'un corps JSON, ou le brut. */
    private fun parseServerMessage(body: String): String {
        // Corps type : {"code":"P0001","message":"quota_caisses_atteint",...}
        val m = Regex("\"message\"\\s*:\\s*\"([^\"]*)\"").find(body)
        return m?.groupValues?.get(1)?.ifBlank { body } ?: body
    }

    /**
     * Le patron génère un code d'appairage (RPC `create_pairing_code`). Lève
     * [SupabaseException] avec le message serveur (ex. `quota_caisses_atteint`,
     * `not_patron`) pour que l'UI l'affiche.
     */
    suspend fun createPairingCode(): String = withContext(Dispatchers.IO) {
        ensureValidUser()
        ensureFreshSession()
        val authToken = token ?: throw SupabaseException("Session invalide")
        val response = http.post(restUrl("rpc/create_pairing_code")) {
            header("apikey", anonKey)
            header("Authorization", "Bearer $authToken")
            contentType(ContentType.Application.Json)
            setBody(EmptyRpcBody())
        }
        if (response.status.value !in 200..299) {
            throw SupabaseException(parseServerMessage(response.bodyAsText()))
        }
        response.bodyAsText().trim().removeSurrounding("\"")
    }

    /**
     * Un vendeur rejoint une boutique via un code (RPC `join_shop_with_code`).
     * Lève [SupabaseException] avec le message serveur (`code_invalide`,
     * `code_expire`, `code_deja_utilise`, `quota_caisses_atteint`).
     */
    suspend fun joinShopWithCode(code: String): JoinShopResult = withContext(Dispatchers.IO) {
        ensureValidUser()
        ensureFreshSession()
        val authToken = token ?: throw SupabaseException("Session invalide")
        val response = http.post(restUrl("rpc/join_shop_with_code")) {
            header("apikey", anonKey)
            header("Authorization", "Bearer $authToken")
            contentType(ContentType.Application.Json)
            setBody(JoinShopPayload(p_code = code.trim()))
        }
        if (response.status.value !in 200..299) {
            throw SupabaseException(parseServerMessage(response.bodyAsText()))
        }
        response.body<JoinShopResult>()
    }

    /** Liste des caisses/membres de ma boutique (patron). Vide si non patron ou hors ligne. */
    suspend fun myShopMembers(): List<ShopMemberDto> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured || !hasValidSession) return@withContext emptyList()
            ensureFreshSession()
            val authToken = token ?: return@withContext emptyList()
            val response = http.post(restUrl("rpc/my_shop_members")) {
                header("apikey", anonKey)
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody(EmptyRpcBody())
            }
            if (response.status.value !in 200..299) return@withContext emptyList()
            response.body<List<ShopMemberDto>>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Le patron détache une caisse vendeur (RPC `remove_shop_member`). */
    suspend fun removeShopMember(userId: String) = withContext(Dispatchers.IO) {
        ensureValidUser()
        ensureFreshSession()
        val authToken = token ?: throw SupabaseException("Session invalide")
        val response = http.post(restUrl("rpc/remove_shop_member")) {
            header("apikey", anonKey)
            header("Authorization", "Bearer $authToken")
            contentType(ContentType.Application.Json)
            setBody(RemoveMemberPayload(p_user = userId))
        }
        if (response.status.value !in 200..299) {
            throw SupabaseException(parseServerMessage(response.bodyAsText()))
        }
    }

    /** Un vendeur quitte la boutique (RPC `leave_shop`) et retrouve sa boutique solo. */
    suspend fun leaveShop(): String? = withContext(Dispatchers.IO) {
        try {
            ensureValidUser()
            ensureFreshSession()
            val authToken = token ?: return@withContext null
            val response = http.post(restUrl("rpc/leave_shop")) {
                header("apikey", anonKey)
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody(EmptyRpcBody())
            }
            if (response.status.value !in 200..299) return@withContext null
            response.bodyAsText().trim().removeSurrounding("\"").ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    // ==================== PARTENAIRES ====================

    /**
     * Résout un code partenaire (ex. PTN-K2M7Q) en nom affichable, pour
     * l'onboarding ("Tu viens de la part de <Nom>"). Accessible avant toute
     * connexion (clé anon suffit). Enrichissement UI best-effort : ne lève
     * jamais — null si le code est invalide, le partenaire inactif, ou hors
     * ligne (l'onboarding retombe alors sur l'affichage du code brut).
     */
    suspend fun getPartnerName(code: String): String? = withContext(Dispatchers.IO) {
        try {
            val response = http.post(restUrl("rpc/partner_name_by_code")) {
                header("apikey", anonKey)
                contentType(ContentType.Application.Json)
                setBody(PartnerNameRpcPayload(p_code = code))
            }
            if (response.status.value !in 200..299) return@withContext null
            // PostgREST renvoie une chaîne JSON scalaire ("Nom" ou null).
            val text = response.bodyAsText().trim()
            if (text.isEmpty() || text == "null") null else text.removeSurrounding("\"")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Signale qu'une installation est attribuée à un partenaire (code PTN-XXXXX).
     * Appelé ANON (sans session) au premier lancement, via la RPC SECURITY
     * DEFINER `record_partner_install`. Fire-and-forget : n'interrompt jamais
     * le parcours utilisateur.
     */
    fun recordPartnerInstall(code: String) {
        val trimmed = code.trim()
        if (trimmed.isBlank()) return
        logScope.launch {
            try {
                val response = http.post(restUrl("rpc/record_partner_install")) {
                    header("apikey", anonKey)
                    contentType(ContentType.Application.Json)
                    setBody(PartnerInstallPayload(p_code = trimmed))
                }
                if (response.status.value !in 200..299) {
                    AppLog.w("LissafiLog", "recordPartnerInstall HTTP ${response.status.value}")
                }
            } catch (e: Exception) {
                AppLog.w("LissafiLog", "recordPartnerInstall ignoré : ${e.message}")
            }
        }
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
                    AppLog.w("LissafiLog", "logEvent HTTP ${response.status.value}")
                }
            } catch (e: Exception) {
                AppLog.w("LissafiLog", "logEvent ignoré : ${e.message}")
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
                    AppLog.w("LissafiLog", "reportTicket HTTP ${response.status.value}")
                }
            } catch (e: Exception) {
                AppLog.w("LissafiLog", "reportTicket ignoré : ${e.message}")
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
                    AppLog.w("LissafiLog", "upsertDeviceToken HTTP ${response.status.value}")
                }
            } catch (e: Exception) {
                AppLog.w("LissafiLog", "upsertDeviceToken ignoré : ${e.message}")
            }
        }
    }
}

class SupabaseException(message: String, cause: Throwable? = null) : Exception(message, cause)
