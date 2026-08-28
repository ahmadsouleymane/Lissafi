package com.lissafi.app.data.auth

import android.content.Context
import com.lissafi.app.data.remote.SupabaseManager
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
private data class SignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String>? = null
)

@Serializable
private data class SignInRequest(val email: String, val password: String)

@Serializable
private data class ResetPasswordRequest(val email: String)

@Serializable
private data class IdTokenRequest(val provider: String, val id_token: String)

@Serializable
private data class AuthUser(val id: String, val email: String)

@Serializable
private data class TokenResponse(val access_token: String, val refresh_token: String, val user: AuthUser)

@Serializable
private data class SignUpResponse(
    val id: String = "",
    val email: String = "",
    val access_token: String = "",
    val refresh_token: String = "",
    val user: AuthUser? = null
)

class AuthManager(private val context: Context) {

    private val http get() = SupabaseManager.getHttpClient()

    suspend fun signUp(
        email: String,
        password: String,
        shopName: String = "",
        ownerName: String = "",
        phone: String = "",
        market: String = ""
    ): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val cleanEmail = email.trim().lowercase()
                val url = "${SupabaseManager.getUrl(context)}/auth/v1/signup"
                val anonKey = SupabaseManager.getAnonKey(context)

                // Métadonnées du compte → raw_user_meta_data (lisible par le back-office).
                val meta = buildMap {
                    if (shopName.isNotBlank()) put("shop_name", shopName.trim())
                    if (ownerName.isNotBlank()) put("owner_name", ownerName.trim())
                    if (phone.isNotBlank()) put("phone", phone.trim())
                    if (market.isNotBlank()) put("market", market.trim())
                }.ifEmpty { null }

                val response = http.post(url) {
                    header("apikey", anonKey)
                    contentType(ContentType.Application.Json)
                    setBody(SignUpRequest(cleanEmail, password, meta))
                }

                when {
                    response.status.value in 200..299 -> {
                        val auth = response.body<SignUpResponse>()
                        // Auto-confirm activé → le serveur retourne une session (user imbriqué).
                        // Auto-confirm désactivé → il ne retourne que id/email à la racine.
                        val uid = auth.user?.id ?: auth.id
                        val mail = auth.user?.email ?: auth.email
                        val loggedIn = auth.access_token.isNotEmpty()
                        if (loggedIn) {
                            SupabaseManager.saveSession(context, auth.access_token, auth.refresh_token, uid, mail)
                        }
                        val message = if (loggedIn) {
                            "Compte créé, bienvenue !"
                        } else {
                            "Compte créé ! Vérifie tes emails puis connecte-toi."
                        }
                        AuthResult.Success(message)
                    }
                    response.status.value == 400 -> {
                        // Message d'erreur réel (ex. "User already registered")
                        val body = try { response.bodyAsText() } catch (_: Exception) { "" }
                        if (body.contains("already registered", ignoreCase = true)) {
                            AuthResult.Error("Ce compte existe déjà. Connecte-toi.")
                        } else {
                            AuthResult.Error("Erreur d'inscription. Vérifie tes informations.")
                        }
                    }
                    response.status.value == 429 -> AuthResult.Error("Trop de tentatives. Réessaie plus tard.")
                    else -> AuthResult.Error("Erreur d'inscription. Vérifie tes informations.")
                }
            } catch (e: kotlinx.serialization.SerializationException) {
                AuthResult.Error("Réponse du serveur invalide. Réessaie.")
            } catch (e: java.io.IOException) {
                AuthResult.Error("Pas de connexion Internet. Vérifie ton réseau.")
            } catch (e: Exception) {
                AuthResult.Error("Erreur inattendue. Réessaie.")
            }
        }

    /**
     * Connexion/inscription via Google : échange l'`id_token` Google contre une
     * session Supabase (`grant_type=id_token`). Supabase crée le compte s'il
     * n'existe pas encore. Le numéro WhatsApp est complété après coup (écran de
     * complétion) car Google ne le fournit pas.
     */
    suspend fun signInWithGoogle(idToken: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val url = "${SupabaseManager.getUrl(context)}/auth/v1/token?grant_type=id_token"
                val anonKey = SupabaseManager.getAnonKey(context)

                val response = http.post(url) {
                    header("apikey", anonKey)
                    contentType(ContentType.Application.Json)
                    setBody(IdTokenRequest(provider = "google", id_token = idToken))
                }

                if (response.status.value in 200..299) {
                    val auth = response.body<TokenResponse>()
                    SupabaseManager.saveSession(context, auth.access_token, auth.refresh_token, auth.user.id, auth.user.email)
                    AuthResult.Success("Connecté avec Google !")
                } else {
                    AuthResult.Error("Connexion Google refusée. Réessaie.")
                }
            } catch (e: kotlinx.serialization.SerializationException) {
                AuthResult.Error("Réponse du serveur invalide. Réessaie.")
            } catch (e: java.io.IOException) {
                AuthResult.Error("Pas de connexion Internet. Vérifie ton réseau.")
            } catch (e: Exception) {
                AuthResult.Error("Erreur de connexion Google. Réessaie.")
            }
        }

    suspend fun signIn(email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val cleanEmail = email.trim().lowercase()
                val url = "${SupabaseManager.getUrl(context)}/auth/v1/token?grant_type=password"
                val anonKey = SupabaseManager.getAnonKey(context)

                val response = http.post(url) {
                    header("apikey", anonKey)
                    contentType(ContentType.Application.Json)
                    setBody(SignInRequest(cleanEmail, password))
                }

                when {
                    response.status.value in 200..299 -> {
                        val auth = response.body<TokenResponse>()
                        SupabaseManager.saveSession(context, auth.access_token, auth.refresh_token, auth.user.id, auth.user.email)
                        AuthResult.Success("Connecté avec succès !")
                    }
                    response.status.value == 400 -> {
                        // Distinguer "email non confirmé" de "mauvais mot de passe"
                        val body = try { response.bodyAsText() } catch (_: Exception) { "" }
                        if (body.contains("not confirmed", ignoreCase = true)) {
                            AuthResult.Error("Valide ton email avant de te connecter.")
                        } else {
                            AuthResult.Error("Email ou mot de passe incorrect.")
                        }
                    }
                    response.status.value == 429 -> AuthResult.Error("Trop de tentatives. Réessaie plus tard.")
                    else -> AuthResult.Error("Erreur de connexion.")
                }
            } catch (e: kotlinx.serialization.SerializationException) {
                AuthResult.Error("Réponse du serveur invalide. Réessaie.")
            } catch (e: java.io.IOException) {
                AuthResult.Error("Pas de connexion Internet. Vérifie ton réseau.")
            } catch (e: Exception) {
                AuthResult.Error("Erreur inattendue. Réessaie.")
            }
        }

    suspend fun signOut(): AuthResult = withContext(Dispatchers.IO) {
        try {
            val tok = SupabaseManager.getAccessToken(context)
            if (tok != null) {
                val url = "${SupabaseManager.getUrl(context)}/auth/v1/logout"
                val anonKey = SupabaseManager.getAnonKey(context)
                http.post(url) {
                    header("apikey", anonKey)
                    header("Authorization", "Bearer $tok")
                }
            }
            SupabaseManager.clearSession(context)
            AuthResult.Success("Déconnecté.")
        } catch (e: Exception) {
            SupabaseManager.clearSession(context)
            AuthResult.Success("Déconnecté.")
        }
    }

    suspend fun resetPassword(email: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val cleanEmail = email.trim().lowercase()
                val url = "${SupabaseManager.getUrl(context)}/auth/v1/recover"
                val anonKey = SupabaseManager.getAnonKey(context)

                val resp = http.post(url) {
                    header("apikey", anonKey)
                    contentType(ContentType.Application.Json)
                    setBody(ResetPasswordRequest(cleanEmail))
                }
                if (resp.status.value in 200..299) {
                    AuthResult.Success("Email envoyé à $cleanEmail. Vérifie ta boîte mail.")
                } else {
                    AuthResult.Error("Impossible d'envoyer l'email. Réessaie plus tard.")
                }
            } catch (e: java.io.IOException) {
                AuthResult.Error("Pas de connexion Internet. Vérifie ton réseau.")
            } catch (e: Exception) {
                AuthResult.Error("Erreur inattendue. Réessaie.")
            }
        }

    /**
     * Rafraîchit la session avec le refresh token (les tokens d'accès GoTrue
     * expirent après ~1 h). Retourne true si la session est redevenue valide.
     */
    suspend fun refreshSession(): Boolean = withContext(Dispatchers.IO) {
        val refreshToken = SupabaseManager.getRefreshToken(context) ?: return@withContext false
        val anonKey = SupabaseManager.getAnonKey(context)
        try {
            val url = "${SupabaseManager.getUrl(context)}/auth/v1/token?grant_type=refresh_token"
            val response = http.post(url) {
                header("apikey", anonKey)
                contentType(ContentType.Application.Json)
                setBody(mapOf("refresh_token" to refreshToken))
            }
            if (response.status.value !in 200..299) return@withContext false
            val auth = response.body<TokenResponse>()
            SupabaseManager.saveSession(context, auth.access_token, auth.refresh_token, auth.user.id, auth.user.email)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isLoggedIn(): Boolean = SupabaseManager.isLoggedIn(context)
    fun currentUserId(): String? = SupabaseManager.currentUserId(context)
    fun currentUserEmail(): String? = SupabaseManager.currentUserEmail(context)
}

sealed class AuthResult {
    data class Success(val message: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
