package com.lissafi.app.data.auth

import android.content.Context
import com.lissafi.app.data.remote.SupabaseManager
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
private data class SignUpRequest(val email: String, val password: String)

@Serializable
private data class SignInRequest(val email: String, val password: String)

@Serializable
private data class ResetPasswordRequest(val email: String)

@Serializable
private data class AuthUser(val id: String, val email: String)

@Serializable
private data class TokenResponse(val access_token: String, val refresh_token: String, val user: AuthUser)

@Serializable
private data class SignUpResponse(val id: String, val email: String, val access_token: String = "", val refresh_token: String = "")

class AuthManager(private val context: Context) {

    private val http get() = SupabaseManager.getHttpClient()

    suspend fun signUp(email: String, password: String, shopName: String = ""): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val cleanEmail = email.trim().lowercase()
                val url = "${SupabaseManager.getUrl(context)}/auth/v1/signup"
                val anonKey = SupabaseManager.getAnonKey(context)

                val response = http.post(url) {
                    header("apikey", anonKey)
                    contentType(ContentType.Application.Json)
                    setBody(SignUpRequest(cleanEmail, password))
                }

                when {
                    response.status.value in 200..299 -> {
                        val auth = response.body<SignUpResponse>()
                        if (auth.access_token.isNotEmpty()) {
                            SupabaseManager.saveSession(context, auth.access_token, auth.refresh_token, auth.id, auth.email)
                        }
                        AuthResult.Success("Inscription réussie ! Vérifie tes emails.")
                    }
                    response.status.value == 429 -> AuthResult.Error("Trop de tentatives. Réessaie plus tard.")
                    else -> AuthResult.Error("Erreur d'inscription. Vérifie tes informations.")
                }
            } catch (e: Exception) {
                AuthResult.Error("Pas de connexion Internet. Vérifie ton réseau.")
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
                    response.status.value == 400 -> AuthResult.Error("Email ou mot de passe incorrect.")
                    response.status.value == 429 -> AuthResult.Error("Trop de tentatives. Réessaie plus tard.")
                    else -> AuthResult.Error("Erreur de connexion.")
                }
            } catch (e: Exception) {
                AuthResult.Error("Pas de connexion Internet. Vérifie ton réseau.")
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

                http.post(url) {
                    header("apikey", anonKey)
                    contentType(ContentType.Application.Json)
                    setBody(ResetPasswordRequest(cleanEmail))
                }
                AuthResult.Success("Email envoyé à $cleanEmail. Vérifie ta boîte mail.")
            } catch (e: Exception) {
                AuthResult.Error("Pas de connexion Internet.")
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
