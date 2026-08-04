package com.lissafi.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.data.auth.AuthManager
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.PrimaryActionButton
import com.lissafi.app.ui.components.SectionHeader
import com.lissafi.app.ui.components.StatusBadge
import com.lissafi.app.ui.theme.Danger
import com.lissafi.app.ui.theme.LissafiCream
import com.lissafi.app.ui.theme.LissafiGreen
import com.lissafi.app.ui.theme.LissafiOrange
import com.lissafi.app.ui.theme.LissafiWhite
import com.lissafi.app.ui.theme.Neutral400
import com.lissafi.app.ui.theme.Neutral500
import com.lissafi.app.ui.theme.White
import com.lissafi.app.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    authManager: AuthManager,
    onBack: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onSignOut: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showShopDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LissafiCream)
    ) {
        LissafiHeader(
            title = "Paramètres",
            subtitle = "Ta boutique et ton compte",
            leadingIcon = Icons.Filled.Settings,
            onBack = onBack
        )

        Column(modifier = Modifier.padding(16.dp)) {
            // ── STATUT PREMIUM ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isPremium) LissafiOrange.copy(alpha = 0.12f) else White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
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
                                if (state.isPremium) LissafiOrange.copy(alpha = 0.15f)
                                else LissafiGreen.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isPremium) Icons.Filled.Star else Icons.Filled.Shield,
                            contentDescription = null,
                            tint = if (state.isPremium) LissafiOrange else LissafiGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.isPremium) "Premium actif" else "Version gratuite",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (state.isPremium && state.premiumExpiry != null)
                                "Expire le ${FormatUtils.formatDate(state.premiumExpiry!!)}"
                            else
                                "10 produits · 10 clients · historique 30 jours",
                            fontSize = 12.sp,
                            color = Neutral400
                        )
                    }
                    if (!state.isPremium) {
                        StatusBadge(text = "Gratuit", color = LissafiOrange)
                    } else {
                        StatusBadge(text = "Premium", color = LissafiOrange)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── MA BOUTIQUE ──
            SectionHeader(
                text = "MA BOUTIQUE",
                icon = Icons.Filled.Store,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
            LissafiCard(onClick = { showShopDialog = true }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(LissafiGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Store,
                            contentDescription = null,
                            tint = LissafiGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.shopName.ifBlank { "Donne un nom à ta boutique" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = state.shopPhone.ifBlank { "Téléphone non renseigné" },
                            fontSize = 12.sp,
                            color = Neutral400
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Modifier la boutique",
                        tint = LissafiGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── MON COMPTE ──
            SectionHeader(
                text = "MON COMPTE",
                icon = Icons.Filled.Person,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
            LissafiCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(LissafiGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            tint = LissafiGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = authManager.currentUserEmail() ?: "Compte local",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "Connecté à Lissafi",
                            fontSize = 12.sp,
                            color = Neutral400
                        )
                    }
                    TextButton(
                        onClick = {
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                authManager.signOut()
                            }
                            onSignOut()
                        }
                    ) {
                        Text(
                            text = "Quitter",
                            color = Danger,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── ADMINISTRATION ──
            SectionHeader(
                text = "ADMINISTRATION",
                icon = Icons.Filled.AdminPanelSettings,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
            LissafiCard(onClick = onNavigateToAdmin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(LissafiGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            tint = LissafiGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Zone réservée au gérant",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Premium, synchronisation, réglages avancés",
                            fontSize = 12.sp,
                            color = Neutral400
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = LissafiGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showShopDialog) {
        ShopInfoDialog(
            currentName = state.shopName,
            currentPhone = state.shopPhone,
            onDismiss = { showShopDialog = false },
            onSave = { name, phone ->
                viewModel.saveShopInfo(name, phone)
                showShopDialog = false
            }
        )
    }
}

// ============================================================
// DIALOGUE INFORMATIONS BOUTIQUE
// ============================================================
@Composable
private fun ShopInfoDialog(
    currentName: String,
    currentPhone: String,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var phone by remember { mutableStateOf(currentPhone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        icon = {
            Icon(
                imageVector = Icons.Filled.Store,
                contentDescription = null,
                tint = LissafiGreen,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Column {
                Text(text = "Ta boutique", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Ces informations apparaîtront sur tes reçus.",
                    fontSize = 12.sp,
                    color = Neutral400
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de la boutique") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } },
                    label = { Text("Téléphone") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            PrimaryActionButton(
                text = "ENREGISTRER",
                onClick = { onSave(name.trim(), phone.trim()) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = Neutral500) }
        }
    )
}
