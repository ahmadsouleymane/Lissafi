package com.lissafi.app

import android.app.Application
import com.lissafi.app.data.LissafiDatabase
import com.lissafi.app.data.auth.AuthManager
import com.lissafi.app.data.remote.SupabaseApi
import com.lissafi.app.data.remote.SupabaseManager
import com.lissafi.app.data.sync.SyncManager
import com.lissafi.app.data.sync.SyncWorker

class LissafiApp : Application() {
    val database: LissafiDatabase by lazy { LissafiDatabase.getInstance(this) }
    val supabaseApi: SupabaseApi by lazy { SupabaseApi(this) }
    val authManager: AuthManager by lazy { AuthManager(this) }
    val syncManager: SyncManager by lazy { SyncManager(this, database, supabaseApi) }

    override fun onCreate() {
        super.onCreate()

        // Initialise le client HTTP Supabase
        SupabaseManager.getHttpClient()

        // Démarre l'écoute réseau pour synchro automatique
        syncManager.startNetworkObserver()

        // Planifie la synchronisation périodique
        SyncWorker.schedule(this)

        // Synchronisation automatique au démarrage si déjà connecté :
        // on récupère immédiatement les données Supabase dans la base locale.
        if (authManager.isLoggedIn()) {
            syncManager.syncInBackground()
        }
    }
}
