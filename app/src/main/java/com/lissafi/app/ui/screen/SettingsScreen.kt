package com.lissafi.app.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.LissafiApp
import com.lissafi.app.data.auth.AuthManager
import com.lissafi.app.data.sync.SyncStatus
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.components.InfoRow
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.components.PrimaryActionButton
import com.lissafi.app.ui.components.SecondaryActionButton
import com.lissafi.app.ui.components.SectionHeader
import com.lissafi.app.ui.components.StatusBadge
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.Border
import com.lissafi.app.ui.theme.Error
import com.lissafi.app.ui.theme.OnBackground
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.Secondary
import com.lissafi.app.ui.theme.Surface
import com.lissafi.app.ui.theme.TextSecondary
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
    var showReportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val app = remember { context.applicationContext as LissafiApp }
    val syncStatus by app.syncManager.status.collectAsState()
    val syncLabel = when (syncStatus) {
        SyncStatus.SYNCING -> "Synchronisation…"
        SyncStatus.SUCCESS -> "À jour"
        SyncStatus.ERROR -> "Erreur réseau"
        SyncStatus.IDLE -> "En attente"
        SyncStatus.NOT_CONFIGURED -> "Supabase non configuré"
        SyncStatus.NO_SESSION -> "Connecte-toi pour synchroniser"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        LissafiHeader(
            title = "Réglages",
            subtitle = "Ta boutique et ton compte",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── STATUT PREMIUM — CTA clair pour la version gratuite ──
            LissafiCard(
                containerColor = if (state.isPremium) Primary.copy(alpha = 0.08f) else Secondary.copy(alpha = 0.08f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.isPremium)
                                    "Premium actif"
                                else
                                    "Version gratuite — limitée à 10 produits et 10 clients",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (state.isPremium && state.premiumExpiry != null) {
                                Text(
                                    text = "Expire le ${FormatUtils.formatDate(state.premiumExpiry!!)}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    // Version gratuite → bouton d'upgrade vers Admin
                    if (!state.isPremium) {
                        Spacer(Modifier.height(12.dp))
                        SecondaryActionButton(
                            text = "Passer à Premium",
                            onClick = onNavigateToAdmin,
                            height = 48
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── MA BOUTIQUE ──
            SectionHeader(
                text = "BOUTIQUE",
                icon = LissafiIcons.Boutique,
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
                            .background(Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = LissafiIcons.Boutique,
                            contentDescription = null,
                            tint = Primary,
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
                            text = state.shopPhone.ifBlank { "—" },
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = LissafiIcons.Modifier,
                        contentDescription = "Modifier la boutique",
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── MON COMPTE ──
            SectionHeader(
                text = "COMPTE",
                icon = LissafiIcons.Client,
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
                            .background(Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = LissafiIcons.Email,
                            contentDescription = null,
                            tint = Primary,
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
                            color = TextSecondary
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
                            text = "Déconnexion",
                            color = Error,
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
                icon = LissafiIcons.Motdepasse,
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
                            .background(Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = LissafiIcons.Motdepasse,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Zone du gérant",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Premium, synchronisation, réglages avancés",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = LissafiIcons.Retour,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(180f) // flèche vers l'avant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── SUPPORT — signalement direct vers le back-office ──
            SectionHeader(
                text = "SUPPORT",
                icon = LissafiIcons.Alerte,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
            LissafiCard(onClick = { showReportDialog = true }) {
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
                            .background(Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = LissafiIcons.Alerte,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Signaler un problème",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Bug, question, demande d'aide — écris-nous",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = LissafiIcons.Retour,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(180f) // flèche vers l'avant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── PERSONNALISATION REÇU (Premium) ──
            SectionHeader(
                text = "REÇU",
                icon = LissafiIcons.Imprimer,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
            LissafiCard(
                onClick = if (state.isPremium) {
                    { showShopDialog = true }
                } else null
            ) {
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
                            .background(
                                if (state.isPremium) Primary.copy(alpha = 0.1f)
                                else Secondary.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = LissafiIcons.Imprimer,
                            contentDescription = null,
                            tint = if (state.isPremium) Primary else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Nom et téléphone sur le reçu",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (state.isPremium) OnBackground else TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.isPremium) {
                        Icon(
                            imageVector = LissafiIcons.Retour,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(180f) // flèche vers l'avant
                        )
                    } else {
                        // Réservé aux abonnés
                        StatusBadge(text = "Premium", color = Secondary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── DONNÉES ──
            SectionHeader(
                text = "DONNÉES",
                icon = LissafiIcons.Sync,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
            LissafiCard {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    InfoRow(
                        icon = LissafiIcons.Sync,
                        label = "Synchronisation",
                        value = syncLabel,
                        valueColor = when (syncStatus) {
                            SyncStatus.ERROR, SyncStatus.NOT_CONFIGURED -> Secondary
                            SyncStatus.NO_SESSION -> TextSecondary
                            else -> OnBackground
                        }
                    )
                    InfoRow(
                        icon = LissafiIcons.Version,
                        label = "Version",
                        value = "1.0.0"
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

    if (showReportDialog) {
        ReportIssueDialog(
            onDismiss = { showReportDialog = false },
            onSend = { subject, message ->
                showReportDialog = false
                app.supabaseApi.reportSupportTicket(subject, message)
                Toast.makeText(context, "Message envoyé ! Merci.", Toast.LENGTH_LONG).show()
            }
        )
    }
}

// ============================================================
// DIALOGUE SIGNALEMENT DE PROBLÈME
// ============================================================
@Composable
private fun ReportIssueDialog(
    onDismiss: () -> Unit,
    onSend: (subject: String, message: String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        icon = {
            Icon(
                imageVector = LissafiIcons.Alerte,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Column {
                Text(text = "Signaler un problème", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Ton message arrive directement à l'équipe Lissafi.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Sujet") },
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
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Décris le problème…") },
                    minLines = 3,
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
            }
        },
        confirmButton = {
            PrimaryActionButton(
                text = "ENVOYER",
                onClick = { onSend(subject.trim(), message.trim()) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = TextSecondary) }
        }
    )
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
                imageVector = LissafiIcons.Boutique,
                contentDescription = null,
                tint = Primary,
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
                    color = TextSecondary
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
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } },
                    label = { Text("Téléphone") },
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
            }
        },
        confirmButton = {
            PrimaryActionButton(
                text = "ENREGISTRER",
                onClick = { onSave(name.trim(), phone.trim()) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = TextSecondary) }
        }
    )
}
