package com.lissafi.app.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.service.PremiumManager
import com.lissafi.app.ui.theme.LissafiDanger
import com.lissafi.app.ui.theme.LissafiGreen
import com.lissafi.app.ui.theme.LissafiOrange
import com.lissafi.app.ui.theme.LissafiCream
import com.lissafi.app.ui.theme.LissafiWhite
import com.lissafi.app.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun AdminScreen(
    viewModel: SettingsViewModel,
    premiumManager: PremiumManager,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var codeText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LissafiCream)
    ) {
        // En-tête
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LissafiGreen)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← ADMINISTRATION", color = LissafiWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Statut
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LissafiWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Statut : ${if (state.isPremium) "● Premium" else "○ Gratuit"}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (state.isPremium && state.premiumExpiry != null) {
                        Text("Premium jusqu'au : ${FormatUtils.formatDate(state.premiumExpiry)}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Activation Premium
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LissafiWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Activer Premium",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = LissafiGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = codeText,
                        onValueChange = { codeText = it.uppercase() },
                        label = { Text("Code d'activation") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val ok = premiumManager.activateWithCode(codeText.trim())
                                val msg = if (ok) {
                                    viewModel.loadSettings()
                                    "Bienvenue en Premium ! Tout est débloqué pour 365 jours. Merci pour ta confiance."
                                } else "Code invalide. Contacte Lissafi."
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (ok) codeText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = LissafiGreen)
                    ) {
                        Text("ACTIVER")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Démo Premium
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LissafiWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Démo Premium (7 jours)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = LissafiOrange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Teste toutes les fonctionnalités gratuitement pendant 7 jours.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val ok = premiumManager.activateDemo()
                                val msg = if (ok) {
                                    viewModel.loadSettings()
                                    "Démo Premium activée pour 7 jours !"
                                } else "Démo déjà utilisée ou Premium déjà actif."
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = LissafiOrange)
                    ) {
                        Text("ACTIVER LA DÉMO")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Informations
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LissafiWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Informations", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LissafiGreen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Version : 1.0.0", fontSize = 13.sp)
                    Text("Nom de l'app : Lissafi", fontSize = 13.sp)
                }
            }
        }
    }
}
