package com.lissafi.app.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Auto-update : vérifie la dernière version publiée sur la landing, télécharge
 * l'APK et lance l'installation si le code de version est plus récent.
 *
 * Hors Play Store, on ne peut pas installer en silence : l'installation passe
 * par le dialogue système d'Android (une confirmation de l'utilisateur). Si
 * l'app n'a pas encore le droit « Installer des applications inconnues », on
 * ouvre le réglage correspondant pour que l'utilisateur l'accorde une fois.
 *
 * La version publiée est lue dans `latest.json` (écrit par `release-apk.sh`).
 * L'URL de la landing doit correspondre au domaine déployé.
 */
object UpdateManager {

    /** Domaine de la landing (source de latest.json et de l'APK). */
    private const val LANDING_URL = "https://lissafi-one.vercel.app"

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class LatestRelease(
        val versionCode: Int = 0,
        val versionName: String = "",
        val apkUrl: String = "",
        val sha256: String = ""
    )

    /** Vérifie la version, télécharge et lance l'installation si plus récente. */
    suspend fun checkForUpdate(context: Context) = withContext(Dispatchers.IO) {
        try {
            val latest = json.decodeFromString<LatestRelease>(
                String(httpGet("$LANDING_URL/latest.json") ?: return@withContext, Charsets.UTF_8)
            )
            if (latest.versionCode <= currentVersionCode(context)) return@withContext

            val apkBytes = httpGet("$LANDING_URL${latest.apkUrl}") ?: return@withContext
            // Vérifie l'intégrité avant d'installer : si latest.json publie un checksum
            // (release-apk.sh en écrit un depuis 2026-08) et qu'il ne correspond pas au
            // fichier reçu (page d'erreur du serveur, fichier tronqué, altération), on
            // n'installe rien plutôt que de risquer un APK invalide ou compromis.
            if (latest.sha256.isNotBlank() && sha256Of(apkBytes) != latest.sha256.lowercase()) {
                return@withContext
            }
            // Sous-dossier dédié "updates/" : c'est le seul chemin exposé par le
            // FileProvider (voir res/xml/file_paths.xml), pas la racine du cache.
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val file = File(updatesDir, "lissafi-update-${latest.versionCode}.apk")
            file.writeBytes(apkBytes)
            install(context, file)
        } catch (e: Exception) {
            // Auto-update silencieux : un échec n'interrompt jamais l'usage normal.
        }
    }

    private fun sha256Of(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun currentVersionCode(context: Context): Long = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
    } catch (e: Exception) {
        0L
    }

    private fun install(context: Context, file: File) {
        // API < 26 : pas de réglage « inconnues » (installation toujours permise).
        val canInstall = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
        if (!canInstall) {
            // Ouvre le réglage « Installer des applications inconnues » pour Lissafi.
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    private fun httpGet(url: String): ByteArray? {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        return try {
            conn.inputStream.use { it.readBytes() }
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}
