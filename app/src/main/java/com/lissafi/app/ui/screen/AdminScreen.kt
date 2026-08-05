package com.lissafi.app.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.data.remote.SupabaseManager
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.service.PremiumManager
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.components.PrimaryActionButton
import com.lissafi.app.ui.components.SectionHeader
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.Border
import com.lissafi.app.ui.theme.Error
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.Secondary
import com.lissafi.app.ui.theme.Surface
import com.lissafi.app.ui.theme.TextSecondary
import com.lissafi.app.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

private const val ADMIN_PIN = "1234"

@OptIn(ExperimentalMaterial3Api::class)
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

    var pinVerified by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    if (!pinVerified) {
        PinGate(
            pinInput = pinInput,
            pinError = pinError,
            onPinChange = { value ->
                pinInput = value.filter { c -> c.isDigit() }.take(4)
                pinError = false
                if (pinInput.length == 4) {
                    if (pinInput == ADMIN_PIN) {
                        pinVerified = true
                    } else {
                        pinError = true
                        pinInput = ""
                    }
                }
            },
            onBack = onBack
        )
        return
    }

    // ── ADMIN (PIN vérifié) ──
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        LissafiHeader(
            title = "Administration",
            subtitle = "Zone du gérant",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── STATUT ──
            LissafiCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.isPremium) Primary.copy(alpha = 0.1f)
                                else Secondary.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isPremium) LissafiIcons.Boutique else LissafiIcons.Alerte,
                            contentDescription = null,
                            tint = if (state.isPremium) Primary else Secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (state.isPremium) "Premium actif" else "Version gratuite",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        if (state.isPremium && state.premiumExpiry != null) {
                            Text(
                                text = "Expire le ${FormatUtils.formatDate(state.premiumExpiry!!)}",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        } else {
                            Text(
                                text = "Produits et clients limités à 10",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── ACTIVER PREMIUM ──
            SectionHeader(
                text = "PREMIUM",
                icon = LissafiIcons.Boutique,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            LissafiCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Activer Premium",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Entre le code d'activation pour débloquer les limites.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = codeText,
                        onValueChange = { codeText = it.uppercase() },
                        placeholder = { Text("Code d'activation") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Border,
                            focusedContainerColor = Surface,
                            unfocusedContainerColor = Surface,
                            cursorColor = Primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    PrimaryActionButton(
                        text = "ACTIVER",
                        onClick = {
                            scope.launch {
                                val ok = premiumManager.activateWithCode(codeText.trim())
                                Toast.makeText(
                                    context,
                                    if (ok) { viewModel.loadSettings(); "Premium activé ! 365 jours." }
                                    else "Code invalide.",
                                    Toast.LENGTH_LONG
                                ).show()
                                if (ok) codeText = ""
                            }
                        },
                        height = 48
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── DÉMO ──
            LissafiCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Essayer Premium (7 jours)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Secondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Teste toutes les fonctionnalités gratuitement.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    PrimaryActionButton(
                        text = "ACTIVER LA DÉMO",
                        onClick = {
                            scope.launch {
                                val ok = premiumManager.activateDemo()
                                Toast.makeText(
                                    context,
                                    if (ok) { viewModel.loadSettings(); "Démo activée ! 7 jours." }
                                    else "Démo déjà utilisée.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        containerColor = Secondary,
                        height = 48
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── SYNCHRONISATION ──
            SectionHeader(
                text = "SYNCHRONISATION",
                icon = LissafiIcons.Sync,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            LissafiCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Configuration Supabase",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Permet de sauvegarder tes données sur le cloud.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    var supabaseUrl by remember { mutableStateOf("") }
                    var supabaseKey by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = supabaseUrl,
                        onValueChange = { supabaseUrl = it },
                        placeholder = { Text("URL Supabase") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Border,
                            focusedContainerColor = Surface,
                            unfocusedContainerColor = Surface,
                            cursorColor = Primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = supabaseKey,
                        onValueChange = { supabaseKey = it },
                        placeholder = { Text("Clé anonyme") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Border,
                            focusedContainerColor = Surface,
                            unfocusedContainerColor = Surface,
                            cursorColor = Primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    PrimaryActionButton(
                        text = "CONFIGURER",
                        onClick = {
                            if (supabaseUrl.isNotBlank() && supabaseKey.isNotBlank()) {
                                SupabaseManager.configure(context, supabaseUrl.trim(), supabaseKey.trim())
                                Toast.makeText(context, "Supabase configuré !", Toast.LENGTH_SHORT).show()
                            }
                        },
                        height = 48
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── INFOS ──
            SectionHeader(
                text = "INFORMATIONS",
                icon = LissafiIcons.Info,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            LissafiCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Version : 1.0.0", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Lissafi — Caisse intelligente pour petits commerçants",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ============================================================
// ÉCRAN PIN — verrouillage de la zone admin
// ============================================================
@Composable
private fun PinGate(
    pinInput: String,
    pinError: Boolean,
    onPinChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Icon(
                imageVector = LissafiIcons.Retour,
                contentDescription = "Retour",
                tint = TextSecondary
            )
        }

        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = LissafiIcons.Motdepasse,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Zone du gérant",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Entre le code PIN pour continuer",
            fontSize = 13.sp,
            color = TextSecondary
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = pinInput,
            onValueChange = onPinChange,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 26.sp,
                textAlign = TextAlign.Center,
                letterSpacing = 10.sp
            ),
            isError = pinError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = TextSecondary,
                errorBorderColor = Error,
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface,
                cursorColor = Primary
            ),
            modifier = Modifier.width(200.dp)
        )

        AnimatedVisibility(pinError) {
            Text(
                text = "Code incorrect, réessaie.",
                color = Error,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
