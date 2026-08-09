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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.LissafiApp
import com.lissafi.app.data.auth.AuthManager
import com.lissafi.app.data.sync.SyncStatus
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.components.CapsuleTextField
import com.lissafi.app.ui.components.IconCircle
import com.lissafi.app.ui.components.InfoRow
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.components.PrimaryActionButton
import com.lissafi.app.ui.components.SectionHeader
import com.lissafi.app.ui.components.StatusBadge
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.Border
import com.lissafi.app.ui.theme.Error
import com.lissafi.app.ui.theme.OnBackground
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.PrimaryContainer
import com.lissafi.app.ui.theme.Secondary
import com.lissafi.app.ui.theme.Surface
import com.lissafi.app.ui.theme.SurfaceAlt
import com.lissafi.app.ui.theme.TextSecondary
import com.lissafi.app.ui.theme.TextTertiary
import com.lissafi.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    authManager: AuthManager,
    onBack: () -> Unit,
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── STATUT PREMIUM — bannière verte ──
            LissafiCard(
                containerColor = if (state.isPremium) PrimaryContainer else Secondary.copy(alpha = 0.06f),
                cornerRadius = 20,
                elevation = 0
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconCircle(
                        icon = if (state.isPremium) LissafiIcons.Boutique else LissafiIcons.Alerte,
                        backgroundColor = if (state.isPremium) Primary.copy(alpha = 0.15f) else Secondary.copy(alpha = 0.12f),
                        iconTint = if (state.isPremium) Primary else Secondary,
                        size = 48,
                        iconSize = 24
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.isPremium) "Premium actif" else "Version gratuite",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = OnBackground
                        )
                        Text(
                            text = if (state.isPremium && state.premiumExpiry != null)
                                "Expire le ${FormatUtils.formatDate(state.premiumExpiry!!)}"
                            else "Limitée à 10 produits et 10 clients",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── MA BOUTIQUE ──
            SectionHeader(
                text = "BOUTIQUE",
                icon = LissafiIcons.Boutique,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            LissafiCard(onClick = { showShopDialog = true }, cornerRadius = 20) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconCircle(
                        icon = LissafiIcons.Boutique,
                        size = 44,
                        iconSize = 22
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.shopName.ifBlank { "Donne un nom à ta boutique" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = OnBackground
                        )
                        Text(
                            text = state.shopPhone.ifBlank { "—" },
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = LissafiIcons.Modifier,
                        contentDescription = "Modifier",
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── MON COMPTE ──
            SectionHeader(
                text = "COMPTE",
                icon = LissafiIcons.Client,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            LissafiCard(cornerRadius = 20) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconCircle(
                        icon = LissafiIcons.Email,
                        size = 44,
                        iconSize = 22
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = authManager.currentUserEmail() ?: "Compte local",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = OnBackground,
                            maxLines = 1
                        )
                        Text(
                            text = "Connecté à Lissafi",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    TextButton(onClick = { onSignOut() }) {
                        Text(
                            text = "Déconnexion",
                            color = Error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── SUPPORT ──
            SectionHeader(
                text = "SUPPORT",
                icon = LissafiIcons.Alerte,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            LissafiCard(onClick = { showReportDialog = true }, cornerRadius = 20) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconCircle(
                        icon = LissafiIcons.Alerte,
                        size = 44,
                        iconSize = 22
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Signaler un problème",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = OnBackground
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
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── REÇU ──
            SectionHeader(
                text = "REÇU",
                icon = LissafiIcons.Imprimer,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            LissafiCard(
                onClick = if (state.isPremium) {{ showShopDialog = true }} else null,
                cornerRadius = 20
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconCircle(
                        icon = LissafiIcons.Imprimer,
                        backgroundColor = if (state.isPremium) Primary.copy(alpha = 0.08f) else SurfaceAlt,
                        iconTint = if (state.isPremium) Primary else TextSecondary,
                        size = 44,
                        iconSize = 22
                    )
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
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        StatusBadge(text = "Premium", color = Secondary)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── DONNÉES ──
            SectionHeader(
                text = "DONNÉES",
                icon = LissafiIcons.Sync,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            LissafiCard(cornerRadius = 20) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
                    HorizontalDivider(color = Border, thickness = 0.5.dp)
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
// DIALOGUE SIGNALEMENT DE PROBLÈME — modale redesign
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
        shape = RoundedCornerShape(24.dp),
        containerColor = Surface,
        tonalElevation = 0.dp,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LissafiIcons.Alerte,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Signaler un problème",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OnBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Ton message arrive directement à l'équipe Lissafi.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        },
        text = {
            Column {
                CapsuleTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    placeholder = "Sujet",
                    leadingIcon = LissafiIcons.Info
                )
                Spacer(Modifier.height(10.dp))
                CapsuleTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = "Décris le problème…",
                    singleLine = false,
                    minLines = 3,
                    leadingIcon = LissafiIcons.Modifier
                )
            }
        },
        confirmButton = {
            PrimaryActionButton(
                text = "ENVOYER",
                icon = LissafiIcons.Partager,
                onClick = { onSend(subject.trim(), message.trim()) }
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Annuler", color = TextSecondary, fontSize = 14.sp)
            }
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
        shape = RoundedCornerShape(24.dp),
        containerColor = Surface,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LissafiIcons.Boutique,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Ta boutique",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OnBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Ces informations apparaîtront sur tes reçus.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column {
                CapsuleTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Nom de la boutique",
                    leadingIcon = LissafiIcons.Boutique
                )
                Spacer(Modifier.height(10.dp))
                CapsuleTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } },
                    placeholder = "Téléphone",
                    leadingIcon = LissafiIcons.Client
                )
            }
        },
        confirmButton = {
            PrimaryActionButton(
                text = "ENREGISTRER",
                icon = LissafiIcons.Valider,
                onClick = { onSave(name.trim(), phone.trim()) }
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Annuler", color = TextSecondary, fontSize = 14.sp)
            }
        }
    )
}
