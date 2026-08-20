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
 * Auto-update OBLIGATOIRE : vérifie la dernière version publiée sur la landing
 * au démarrage de l'app. Si une version plus récente existe, l'usage de l'app
 * est bloqué (voir `MandatoryUpdateScreen`) tant que l'utilisateur ne l'a pas
 * installée.
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
    data class LatestRelease(
        val versionCode: Int = 0,
        val versionName: String = "",
        val apkUrl: String = "",
        val sha256: String = ""
    )

    /**
     * Renvoie la dernière version publiée si elle est plus récente que celle
     * installée, sinon `null` (déjà à jour, ou vérification impossible — pas
     * de réseau, landing indisponible : on ne bloque jamais l'usage sur un
     * échec de vérification).
     */
    suspend fun checkForUpdate(context: Context): LatestRelease? = withContext(Dispatchers.IO) {
        try {
            val latest = json.decodeFromString<LatestRelease>(
                String(httpGet("$LANDING_URL/latest.json") ?: return@withContext null, Charsets.UTF_8)
            )
            if (latest.versionCode <= currentVersionCode(context)) null else latest
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Télécharge l'APK de `latest` et lance son installation. Renvoie `false`
     * si le téléchargement échoue ou si l'intégrité (sha256) ne correspond pas
     * — dans ce cas, rien n'est installé.
     */
    suspend fun downloadAndInstall(context: Context, latest: LatestRelease): Boolean = withContext(Dispatchers.IO) {
        try {
            val apkBytes = httpGet("$LANDING_URL${latest.apkUrl}") ?: return@withContext false
            // Vérifie l'intégrité avant d'installer : si latest.json publie un checksum
            // (release-apk.sh en écrit un depuis 2026-08) et qu'il ne correspond pas au
            // fichier reçu (page d'erreur du serveur, fichier tronqué, altération), on
            // n'installe rien plutôt que de risquer un APK invalide ou compromis.
            if (latest.sha256.isNotBlank() && sha256Of(apkBytes) != latest.sha256.lowercase()) {
                return@withContext false
            }
            // Sous-dossier dédié "updates/" : c'est le seul chemin exposé par le
            // FileProvider (voir res/xml/file_paths.xml), pas la racine du cache.
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val file = File(updatesDir, "lissafi-update-${latest.versionCode}.apk")
            file.writeBytes(apkBytes)
            install(context, file)
            true
        } catch (e: Exception) {
            false
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
