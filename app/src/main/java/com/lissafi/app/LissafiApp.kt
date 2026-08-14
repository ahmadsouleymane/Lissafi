package com.lissafi.app

import android.app.Application
import com.lissafi.app.data.LissafiDatabase
import com.lissafi.app.data.auth.AuthManager
import com.lissafi.app.data.remote.SupabaseApi
import com.lissafi.app.data.remote.SupabaseManager
import com.lissafi.app.data.sync.SyncManager
import com.lissafi.app.data.sync.SyncWorker
import com.lissafi.app.service.UpdateManager
import com.lissafi.app.ui.theme.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LissafiApp : Application() {
    val database: LissafiDatabase by lazy { LissafiDatabase.getInstance(this) }
    val supabaseApi: SupabaseApi by lazy { SupabaseApi(this) }
    val authManager: AuthManager by lazy { AuthManager(this) }
    val syncManager: SyncManager by lazy { SyncManager(this, database, supabaseApi) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Charge la préférence de thème clair/sombre avant le premier rendu Compose
        ThemeManager.init(this)

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

        // Remonte le démarrage au back-office (fire-and-forget, jamais bloquant)
        supabaseApi.logEvent("app_start", "info", "Application démarrée")

        // Vérification de mise à jour en arrière-plan (auto-update). Petit délai
        // pour laisser l'app se lancer avant l'éventuel dialogue d'installation.
        appScope.launch {
            delay(4000)
            UpdateManager.checkForUpdate(this@LissafiApp)
        }
    }
}
