package com.lissafi.app.data.remote

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
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
                    })
                }
            }.also { httpClient = it }
        }
    }

    fun getUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
    }

    fun getAnonKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ANON_KEY, DEFAULT_ANON_KEY) ?: DEFAULT_ANON_KEY
    }

    // ==================== Session ====================

    fun saveSession(context: Context, accessToken: String, refreshToken: String, userId: String, email: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            apply()
        }
    }

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            apply()
        }
    }

    fun getAccessToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ACCESS_TOKEN, null)
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
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_USER_ID, null)
    }

    fun currentUserEmail(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_USER_EMAIL, null)
    }

    fun configure(context: Context, url: String, anonKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(KEY_URL, url)
            putString(KEY_ANON_KEY, anonKey)
            apply()
        }
    }
}
