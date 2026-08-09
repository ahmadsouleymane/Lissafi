package com.lissafi.app.data

import android.content.Context

/**
 * Mémorise si l'onboarding a déjà été affiché sur ce téléphone.
 *
 * Flag LOCAL au téléphone, jamais synchronisé : chaque nouvel appareil
 * d'un même compte reverra la présentation (comportement souhaité).
 */
object OnboardingManager {
    private const val PREFS_NAME = "lissafi_prefs"
    private const val KEY_ONBOARDING_SEEN = "onboarding_seen"

    fun isCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_SEEN, false)

    fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply()
    }
}
