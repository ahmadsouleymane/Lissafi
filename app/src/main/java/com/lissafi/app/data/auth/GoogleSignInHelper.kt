package com.lissafi.app.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.lissafi.app.service.AppLog

/**
 * Récupère un `id_token` Google via Credential Manager (compte sélectionné par
 * l'utilisateur). Le jeton est ensuite échangé contre une session Supabase par
 * [AuthManager.signInWithGoogle].
 */
object GoogleSignInHelper {

    /**
     * Client OAuth **Web** (serverClientId). Ce n'est pas un secret : l'audience
     * du jeton, validée côté Supabase (« Authorized Client IDs »).
     */
    const val WEB_CLIENT_ID =
        "1044459055127-bl7n508auiqt4l542s483rv0lc33f4qc.apps.googleusercontent.com"

    /**
     * Ouvre le sélecteur de compte Google et renvoie l'`id_token`, ou `null` si
     * l'utilisateur annule ou en cas d'erreur (Google Play Services absent,
     * mauvaise config…). L'appel doit se faire avec un contexte d'Activity.
     */
    suspend fun getIdToken(context: Context): String? {
        return try {
            val option = GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val response = CredentialManager.create(context).getCredential(context, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } else {
                AppLog.w("GoogleSignIn", "Type de credential inattendu : ${credential.type}")
                null
            }
        } catch (e: GetCredentialException) {
            // Inclut l'annulation utilisateur et l'absence de compte : silencieux.
            AppLog.w("GoogleSignIn", "Connexion Google interrompue : ${e.message}")
            null
        } catch (e: Exception) {
            AppLog.w("GoogleSignIn", "Erreur Google Sign-In : ${e.message}")
            null
        }
    }
}
