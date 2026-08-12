package com.lissafi.app.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Vérifie que l'APK installé est bien signé avec le certificat release officiel.
 * Bloque les APK re-packagés (re-signés) qui redirigeraient les connexions vers
 * un serveur attaquant. Empreinte SHA-256 du certificat calculée via keytool :
 *   keytool -exportcert -alias lissafi -keystore <store> | shasum -a 256
 */
object SignatureVerifier {

    // SHA-256 du certificat de signature release (alias "lissafi").
    private const val EXPECTED_SHA256 = "b0f6ab2b6b58987f1c84abe6936afb3d472bfb3fd1bbf4c82a1c2e65ad6f288b"

    /** true si l'APK installé est signé avec le certificat officiel. */
    fun isGenuine(context: Context): Boolean {
        return try {
            val cert = signingCertBytes(context) ?: return false
            val digest = MessageDigest.getInstance("SHA-256").digest(cert)
            val hex = digest.joinToString("") { "%02x".format(it) }
            hex.equals(EXPECTED_SHA256, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    private fun signingCertBytes(context: Context): ByteArray? {
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        )
        val signer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.firstOrNull()
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.firstOrNull()
        }
        return signer?.toByteArray()
    }
}
