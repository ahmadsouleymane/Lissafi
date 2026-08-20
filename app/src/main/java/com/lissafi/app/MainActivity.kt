package com.lissafi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lissafi.app.service.SignatureVerifier
import com.lissafi.app.service.UpdateManager
import com.lissafi.app.ui.navigation.LissafiNavHost
import com.lissafi.app.ui.screen.MandatoryUpdateScreen
import com.lissafi.app.ui.theme.LissafiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LissafiTheme {
                // Vérification de mise à jour AVANT tout accès à l'app : voir
                // MandatoryUpdateScreen. Un échec de vérification (pas de réseau,
                // landing indisponible) laisse passer — on ne bloque jamais sur ça.
                var updateChecked by remember { mutableStateOf(false) }
                var pendingUpdate by remember { mutableStateOf<UpdateManager.LatestRelease?>(null) }
                LaunchedEffect(Unit) {
                    pendingUpdate = UpdateManager.checkForUpdate(this@MainActivity)
                    updateChecked = true
                }

                when {
                    !updateChecked -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    pendingUpdate != null -> {
                        MandatoryUpdateScreen(pendingUpdate!!)
                    }
                    SignatureVerifier.isGenuine(this) -> {
                        LissafiNavHost(modifier = Modifier.fillMaxSize())
                    }
                    else -> {
                        // APK re-packagé / re-signé : on bloque l'usage pour éviter
                        // le vol d'identifiants via un serveur détourné.
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Installation non vérifiée",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Cette copie de Lissafi ne provient pas d'une source officielle. " +
                                    "Télécharge-la depuis le site officiel ou contacte l'équipe sur WhatsApp.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
