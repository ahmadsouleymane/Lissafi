package com.lissafi.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.service.UpdateManager
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.components.PrimaryActionButton
import com.lissafi.app.ui.theme.Error
import com.lissafi.app.ui.theme.OnBackground
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Écran plein-écran, sans échappatoire : affiché tant que la version
 * installée est plus ancienne que `landing/public/latest.json`. L'usage
 * normal de l'app ne reprend qu'après une installation réussie et un
 * redémarrage (le prochain lancement repasse la vérification, qui devient
 * alors négative).
 */
@Composable
fun MandatoryUpdateScreen(latest: UpdateManager.LatestRelease) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = LissafiIcons.Info,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Mise à jour obligatoire",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = OnBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (latest.versionName.isNotBlank())
                "Une nouvelle version de Lissafi (${latest.versionName}) est disponible. " +
                    "Installe-la pour continuer à utiliser l'application."
            else
                "Une nouvelle version de Lissafi est disponible. Installe-la pour continuer à utiliser l'application.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(32.dp))

        if (downloading) {
            CircularProgressIndicator(color = Primary)
            Spacer(Modifier.height(16.dp))
            Text(text = "Téléchargement en cours…", fontSize = 13.sp, color = TextSecondary)
        } else {
            if (failed) {
                Text(
                    text = "Le téléchargement a échoué. Vérifie ta connexion et réessaie.",
                    fontSize = 13.sp,
                    color = Error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
            }
            PrimaryActionButton(
                text = "Mettre à jour maintenant",
                onClick = {
                    failed = false
                    downloading = true
                    scope.launch {
                        val ok = UpdateManager.downloadAndInstall(context, latest)
                        downloading = false
                        if (!ok) failed = true
                        // En cas de succès, le dialogue d'installation système s'ouvre ;
                        // cet écran reste affiché en dessous jusqu'au relancement de l'app.
                    }
                }
            )
        }
    }
}
