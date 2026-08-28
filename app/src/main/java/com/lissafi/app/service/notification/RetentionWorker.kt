package com.lissafi.app.service.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lissafi.app.LissafiApp
import com.lissafi.app.data.repository.LissafiRepository
import com.lissafi.app.service.AppLog
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.service.PremiumManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Worker quotidien (~20 h) qui pose les notifications locales de rétention :
 * - clôture de caisse du jour,
 * - rappel des dettes anciennes,
 * - compte à rebours de fin d'essai.
 * Local et hors-ligne : ne dépend d'aucun serveur.
 */
class RetentionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "RetentionWorker"
        private const val UNIQUE_WORK_NAME = "lissafi_retention_daily"
        private const val TARGET_HOUR = 20
        private const val DEBT_STALE_DAYS = 7
        private const val DEBT_THROTTLE_MS = 3L * 24 * 60 * 60 * 1000

        /** Planifie le worker quotidien, premier passage au prochain 20 h. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RetentionWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayToNextTargetHourMs(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        private fun delayToNextTargetHourMs(): Long {
            val now = Calendar.getInstance()
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, TARGET_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (next.timeInMillis <= now.timeInMillis) {
                next.add(Calendar.DAY_OF_YEAR, 1)
            }
            return next.timeInMillis - now.timeInMillis
        }
    }

    override suspend fun doWork(): androidx.work.ListenableWorker.Result = withContext(Dispatchers.IO) {
        try {
            val app = applicationContext as LissafiApp
            val userId = app.authManager.currentUserId() ?: return@withContext success()
            val repo = LissafiRepository(app.database, app.supabaseApi).withUserId(userId)
            val premium = PremiumManager(repo, app.supabaseApi)

            notifyCloture(repo)
            notifyDettes(repo)
            notifyEssai(repo, premium)

            success()
        } catch (e: Exception) {
            AppLog.w(TAG, "Notifications de rétention ignorées : ${e.message}")
            success() // jamais de retry bruyant pour de la notif
        }
    }

    private fun success() = androidx.work.ListenableWorker.Result.success()

    /** Clôture du jour : nombre de ventes et chiffre encaissé. */
    private suspend fun notifyCloture(repo: LissafiRepository) {
        val (start, end) = FormatUtils.todayRange()
        val sales = repo.getSalesBetween(start, end)
        if (sales.isEmpty()) return
        val total = sales.sumOf { it.total }
        val clientsDus = repo.getAllClients().count { it.totalDebt > 0 }
        val debtSuffix = if (clientsDus > 0) " · $clientsDus client${if (clientsDus > 1) "s" else ""} te doi${if (clientsDus > 1) "vent" else "t"} de l'argent" else ""
        RetentionNotifier.notify(
            applicationContext,
            RetentionNotifier.CHANNEL_CLOTURE,
            "Clôture du jour",
            RetentionNotifier.ID_CLOTURE,
            title = "Ta journée en un coup d'œil",
            body = "${sales.size} vente${if (sales.size > 1) "s" else ""} · ${FormatUtils.formatFCFA(total)} encaissé$debtSuffix"
        )
    }

    /** Rappel des dettes anciennes, throttlé à un passage tous les 3 jours. */
    private suspend fun notifyDettes(repo: LissafiRepository) {
        val staleBefore = System.currentTimeMillis() - DEBT_STALE_DAYS * 24L * 60 * 60 * 1000
        val stale = repo.getAllClients().filter { it.totalDebt > 0 && it.updatedAt < staleBefore }
        if (stale.isEmpty()) return

        val last = repo.getSetting("last_debt_reminder_ms")?.toLongOrNull() ?: 0
        if (System.currentTimeMillis() - last < DEBT_THROTTLE_MS) return
        repo.setSetting("last_debt_reminder_ms", System.currentTimeMillis().toString())

        val totalDu = stale.sumOf { it.totalDebt }
        val body = if (stale.size == 1) {
            "${stale[0].name} te doit ${FormatUtils.formatFCFA(stale[0].totalDebt)} depuis un moment."
        } else {
            "${stale.size} clients te doivent ${FormatUtils.formatFCFA(totalDu)} au total depuis plus d'une semaine."
        }
        RetentionNotifier.notify(
            applicationContext,
            RetentionNotifier.CHANNEL_DETTES,
            "Rappels de dettes",
            RetentionNotifier.ID_DETTES,
            title = "Pense à récupérer ton argent",
            body = body
        )
    }

    /** Compte à rebours d'essai : à J-3, J-2 et J-1, une fois par jour. */
    private suspend fun notifyEssai(repo: LissafiRepository, premium: PremiumManager) {
        if (premium.isPremium()) return
        val daysLeft = premium.trialDaysLeft()
        if (daysLeft !in 1..3) return

        val today = System.currentTimeMillis() / (24L * 60 * 60 * 1000)
        val lastDay = repo.getSetting("last_trial_notif_day")?.toLongOrNull() ?: -1
        if (lastDay == today) return
        repo.setSetting("last_trial_notif_day", today.toString())

        RetentionNotifier.notify(
            applicationContext,
            RetentionNotifier.CHANNEL_ESSAI,
            "Fin d'essai",
            RetentionNotifier.ID_ESSAI,
            title = "Ton essai finit dans $daysLeft jour${if (daysLeft > 1) "s" else ""}",
            body = "Garde l'accès à tes produits, tes clients et leurs dettes — choisis ta formule.",
            openPaywall = true
        )
    }
}
