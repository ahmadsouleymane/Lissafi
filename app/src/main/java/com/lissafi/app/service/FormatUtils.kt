package com.lissafi.app.service

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val fcfaFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.FRENCH)

    fun formatFCFA(amount: Int): String {
        return "${fcfaFormat.format(amount)} F"
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
