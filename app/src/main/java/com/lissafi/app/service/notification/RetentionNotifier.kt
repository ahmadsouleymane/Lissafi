package com.lissafi.app.service.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lissafi.app.MainActivity
import com.lissafi.app.R

/**
 * Cible de navigation demandée par un tap sur notification.
 * Lu par [MainActivity] (extra d'intent) puis consommé par le NavHost.
 */
object NotificationNav {
    /** Extra d'intent : ouvrir le paywall au démarrage. */
    const val EXTRA_OPEN_PAYWALL = "open_paywall"

    /** Observable par Compose ; le NavHost navigue puis remet à false. */
    val openPaywall = mutableStateOf(false)
}

/**
 * Notifications locales de rétention (clôture quotidienne, rappels de dettes,
 * compte à rebours d'essai). Fonctionnent hors-ligne, sans serveur.
 */
object RetentionNotifier {

    const val CHANNEL_CLOTURE = "lissafi_cloture"
    const val CHANNEL_DETTES = "lissafi_dettes"
    const val CHANNEL_ESSAI = "lissafi_essai"

    const val ID_CLOTURE = 2001
    const val ID_DETTES = 2002
    const val ID_ESSAI = 2003

    /**
     * Poste une notification. Sur Android 13+, ne fait rien si la permission
     * POST_NOTIFICATIONS n'est pas accordée. [openPaywall] ouvre l'écran
     * d'abonnement au tap (utilisé pour le compte à rebours d'essai).
     */
    fun notify(
        context: Context,
        channelId: String,
        channelName: String,
        notifId: Int,
        title: String,
        body: String,
        openPaywall: Boolean = false
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (openPaywall) putExtra(NotificationNav.EXTRA_OPEN_PAYWALL, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(notifId, notification)
    }
}
