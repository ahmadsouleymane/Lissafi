package com.lissafi.app.data

import android.content.Context
import java.io.RandomAccessFile

/**
 * Lit le code partenaire embarqué dans le commentaire ZIP de l'APK installé.
 *
 * La landing ne sert plus un .apk figé : la route `/api/download?p=CODE` du
 * portail inscrit `lissafi.partner=CODE` dans le commentaire de fin du ZIP —
 * le seul endroit du fichier que la signature Android (APK Signature Scheme
 * v2/v3) ne couvre pas. Au premier lancement, l'app relit ses propres octets
 * (`ApplicationInfo.sourceDir`) pour retrouver qui l'a référée, même si le
 * fichier .apk a été repartagé (WhatsApp, Bluetooth, clé USB) — contrairement
 * au presse-papiers.
 */
object ApkAttribution {

    private val CODE_REGEX = Regex("PTN-[A-Z0-9]{3,}")

    /** Extrait le code partenaire de l'APK, ou null (aucun code, échec de lecture…). */
    fun readPartnerCode(context: Context): String? {
        return try {
            val path = context.applicationInfo.sourceDir ?: return null
            val file = java.io.File(path)
            if (!file.exists()) return null
            val comment = readZipComment(file) ?: return null
            CODE_REGEX.find(comment)?.value
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Lit le commentaire global du ZIP (fin de fichier) en localisant la
     * signature « End of Central Directory » (PK\x05\x06). Retourne null si le
     * fichier n'est pas un ZIP lisible ou s'il n'a pas de commentaire.
     */
    private fun readZipComment(file: java.io.File): String? {
        val raf = RandomAccessFile(file, "r")
        try {
            val fileLen = raf.length()
            if (fileLen < 22) return null

            // Le commentaire fait au plus 65 535 octets, juste avant l'EOCD (22 octets).
            val readLen = minOf(fileLen, 65557L).toInt()
            val buf = ByteArray(readLen)
            raf.seek(fileLen - readLen)
            raf.readFully(buf)

            // Cherche la signature EOCD depuis la fin (0x50 0x4B 0x05 0x06).
            var eocd = -1
            var i = buf.size - 22
            while (i >= 0) {
                if (buf[i].toInt() == 0x50 && buf[i + 1].toInt() == 0x4B &&
                    buf[i + 2].toInt() == 0x05 && buf[i + 3].toInt() == 0x06
                ) {
                    eocd = i
                    break
                }
                i--
            }
            if (eocd == -1) return null

            val commentLen = (buf[eocd + 20].toInt() and 0xFF) or
                ((buf[eocd + 21].toInt() and 0xFF) shl 8)
            if (commentLen == 0) return null

            val commentStart = buf.size - commentLen
            if (commentStart < 0 || commentStart >= buf.size) return null
            return String(buf, commentStart, commentLen, Charsets.UTF_8)
        } finally {
            raf.close()
        }
    }
}
