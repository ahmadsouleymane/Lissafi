package com.lissafi.app.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private const val PREFS_NAME = "lissafi_prefs"
private const val KEY_DARK_MODE = "dark_mode"

// État global du thème (clair/sombre), lu par les propriétés de couleur ci-dessous.
// mutableStateOf déclenche la recomposition partout où une couleur est lue en
// composition, même via un getter — pas besoin de passer le thème en paramètre.
object ThemeManager {
    var isDark by mutableStateOf(false)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDark = prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDark(context: Context, value: Boolean) {
        isDark = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE, value)
            .apply()
    }
}
