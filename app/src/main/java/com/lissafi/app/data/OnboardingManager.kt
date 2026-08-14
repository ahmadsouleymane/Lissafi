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
    private const val KEY_INSTALL_BEACON_SENT = "install_beacon_sent"

    fun isCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_SEEN, false)

    fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply()
    }

    /** Le beacon d'installation a-t-il déjà été envoyé sur cet appareil ? */
    fun isInstallBeaconSent(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_INSTALL_BEACON_SENT, false)

    /** Marque le beacon d'installation comme envoyé (une seule fois par appareil). */
    fun markInstallBeaconSent(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_INSTALL_BEACON_SENT, true).apply()
    }
}
