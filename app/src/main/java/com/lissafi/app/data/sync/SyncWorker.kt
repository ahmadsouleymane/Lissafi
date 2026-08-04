package com.lissafi.app.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.lissafi.app.data.LissafiDatabase
import com.lissafi.app.data.remote.SupabaseApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker de synchronisation périodique.
 *
 * Exécuté par WorkManager :
 * - Périodiquement (toutes les 15 minutes minimum)
 * - Quand le réseau devient disponible (contrainte NetworkType.CONNECTED)
 * - Avec backoff exponentiel en cas d'échec
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val UNIQUE_WORK_NAME = "lissafi_sync_periodic"

        /**
         * Planifie la synchronisation périodique.
         * À appeler une fois au démarrage de l'appli.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Déclenche une synchro immédiate (one-shot).
         */
        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }

        /**
         * Annule la synchro périodique.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Démarrage synchro périodique...")
        try {
            val db = LissafiDatabase.getInstance(applicationContext)
            val api = SupabaseApi(applicationContext)
            val syncManager = SyncManager(applicationContext, db, api)
            syncManager.syncAll()
            Log.d(TAG, "Synchro périodique terminée avec succès")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Synchro périodique échouée", e)
            Result.retry()
        }
    }
}
