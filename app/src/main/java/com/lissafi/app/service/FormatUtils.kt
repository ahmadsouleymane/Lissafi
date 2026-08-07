package com.lissafi.app.service

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    /**
     * Formate un montant FCFA avec des espaces de groupement ASCII (U+0020),
     * pas l'espace insécable étroite U+202F de Locale.FRENCH : cette dernière
     * s'imprime en mojibake sur les tickets thermiques 58 mm en Latin-1.
     */
    fun formatFCFA(amount: Int): String {
        val sign = if (amount < 0) "-" else ""
        val abs = kotlin.math.abs(amount).toString()
        val grouped = abs.reversed().chunked(3).joinToString(" ").reversed()
        return "$sign$grouped FCFA"
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
        return sdf.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "à l'instant"
            diff < 3_600_000 -> "il y a ${diff / 60_000} min"
            diff < 86_400_000 -> "il y a ${diff / 3_600_000} h"
            diff < 172_800_000 -> "hier"
            diff < 604_800_000 -> "il y a ${diff / 86_400_000} jours"
            else -> {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
                sdf.format(Date(timestamp))
            }
        }
    }
}
