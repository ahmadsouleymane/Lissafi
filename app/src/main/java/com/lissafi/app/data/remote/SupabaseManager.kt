package com.lissafi.app.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object SupabaseManager {

    private const val PREFS_NAME = "supabase_config"
    private const val KEY_URL = "supabase_url"
    private const val KEY_ANON_KEY = "supabase_anon_key"
    private const val KEY_ACCESS_TOKEN = "supabase_access_token"
    private const val KEY_REFRESH_TOKEN = "supabase_refresh_token"
    private const val KEY_USER_ID = "supabase_user_id"
    private const val KEY_USER_EMAIL = "supabase_user_email"

    /** Format UUID attendu par les colonnes user_id de Supabase. */
    private val UUID_REGEX = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    )

    /**
     * Préférences CHIFFRÉES (clé Android Keystore, AES256-GCM).
     * Les tokens de session GoTrue (dont le refresh token à longue durée)
     * ne doivent plus vivre en clair sur disque : volés sur un appareil
     * compromis, ils donnent un accès permanent au compte.
     *
     * Instance mise en cache : EncryptedSharedPreferences est recréée à
     * partir du Keystore par Tink en interne (fichier de keyset séparé).
     * En recréer une à chaque lecture — ce qui arrivait sur chaque
     * recomposition Compose (isLoggedIn, currentUserId…) en même temps que
     * la synchro en tâche de fond lit/écrit le token sur un autre thread —
     * exposait à une réinitialisation concurrente du keyset, qui pouvait
     * faire échouer silencieusement une lecture (getString → null) et
     * déconnecter l'utilisateur à tort. Une seule instance thread-safe
     * élimine la course.
     */
    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences {
        return cachedPrefs ?: synchronized(this) {
            cachedPrefs ?: createPrefs(context.applicationContext).also { cachedPrefs = it }
        }
    }

    /**
     * Crée les préférences chiffrées. Le Keystore Android peut être corrompu ou
     * inaccessible (reset OEM, restauration cloud depuis un autre appareil, ROM
     * custom) : `EncryptedSharedPreferences.create` lève alors une exception au
     * tout premier lancement, avant même l'affichage de l'UI — un crash immédiat
     * et irrécupérable pour l'utilisateur. On retente une fois après avoir purgé
     * le fichier de préférences (le keyset corrompu est souvent la seule cause),
     * puis on se rabat sur des SharedPreferences en clair plutôt que de crasher :
     * l'utilisateur devra simplement se reconnecter, ce qui est très préférable
     * à une app qui ne démarre jamais.
     */
    private fun createPrefs(appContext: Context): SharedPreferences {
        fun build(): SharedPreferences {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                appContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
        return try {
            build()
        } catch (_: Exception) {
            try {
                appContext.deleteSharedPreferences(PREFS_NAME)
                build()
            } catch (_: Exception) {
                appContext.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    const val DEFAULT_URL = "https://fnyuhpfzkvunscuylvqv.supabase.co"
    const val DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZueXVocGZ6a3Z1bnNjdXlsdnF2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODU4Mjk3NDgsImV4cCI6MjEwMTQwNTc0OH0.A1lcEsXv8FDsYb4yd3lohd7CtyQy9K5VMXl7QsBuY1Q"

    @Volatile
    private var httpClient: HttpClient? = null

    fun getHttpClient(): HttpClient {
        return httpClient ?: synchronized(this) {
            httpClient ?: HttpClient(Android) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        coerceInputValues = true
                        encodeDefaults = false  // ne sérialise pas id=0, laisse le BIGSERIAL du serveur générer l'id
                    })
                }
                // Timeouts : sans eux, une connexion morte bloquait la synchro sans fin.
                install(HttpTimeout) {
                    connectTimeoutMillis = 10_000
                    requestTimeoutMillis = 25_000
                    socketTimeoutMillis = 25_000
                }
            }.also { httpClient = it }
        }
    }

    fun getUrl(context: Context): String {
        val prefs = prefs(context)
        return prefs.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
    }

    fun getAnonKey(context: Context): String {
        val prefs = prefs(context)
        return prefs.getString(KEY_ANON_KEY, DEFAULT_ANON_KEY) ?: DEFAULT_ANON_KEY
    }

    // ==================== Session ====================

    fun saveSession(context: Context, accessToken: String, refreshToken: String, userId: String, email: String) {
        prefs(context).edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            apply()
        }
    }

    fun clearSession(context: Context) {
        prefs(context).edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            apply()
        }
    }

    fun getAccessToken(context: Context): String? {
        return prefs(context).getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(context: Context): String? {
        return prefs(context).getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * Date d'expiration (epoch secondes) du token d'accès GoTrue, déduite du
     * champ `exp` du JWT. Retourne null si le token n'est pas un JWT décodable.
     */
    fun getAccessTokenExpiry(context: Context): Long? {
        val token = getAccessToken(context) ?: return null
        val parts = token.split(".")
        if (parts.size < 2) return null
        return try {
            val payload = android.util.Base64.decode(
                parts[1].replace('-', '+').replace('_', '/'),
                android.util.Base64.DEFAULT
            )
            val json = org.json.JSONObject(String(payload))
            val exp = json.optLong("exp")
            if (exp > 0) exp else null
        } catch (_: Exception) {
            null
        }
    }

    fun isLoggedIn(context: Context): Boolean = getAccessToken(context) != null

    /**
     * Vrai si l'identifiant stocké est un UUID valide (le format exigé par
     * les colonnes `user_id` de Supabase). Filtre les sessions corrompues
     * comme "test-user-1" qui font échouer les INSERT (typé UUID) et la RLS.
     */
    fun isValidUserId(id: String?): Boolean =
        id != null && UUID_REGEX.matches(id)

    /**
     * Une session n'est exploitable que si elle porte un token ET un user_id
     * au format UUID. Empêche d'envoyer des écritures invalides quand la
     * session locale est corrompue ou obsolète.
     */
    fun hasValidSession(context: Context): Boolean =
        getAccessToken(context) != null && isValidUserId(currentUserId(context))

    fun currentUserId(context: Context): String? {
        return prefs(context).getString(KEY_USER_ID, null)
    }

    fun currentUserEmail(context: Context): String? {
        return prefs(context).getString(KEY_USER_EMAIL, null)
    }

    fun configure(context: Context, url: String, anonKey: String) {
        prefs(context).edit().apply {
            putString(KEY_URL, url)
            putString(KEY_ANON_KEY, anonKey)
            apply()
        }
    }
}
