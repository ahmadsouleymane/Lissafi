package com.lissafi.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.lissafi.app.data.entity.Client
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.components.AmountText
import com.lissafi.app.ui.components.CapsuleTextField
import com.lissafi.app.ui.components.EmptyState
import com.lissafi.app.ui.components.IconCircle
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.components.PremiumLimitDialog
import com.lissafi.app.ui.components.PrimaryActionButton
import com.lissafi.app.ui.components.SearchField
import com.lissafi.app.ui.components.StatusBadge
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.Error
import com.lissafi.app.ui.theme.OnBackground
import com.lissafi.app.ui.theme.OnPrimary
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.Secondary
import com.lissafi.app.ui.theme.OnSecondaryContainer
import com.lissafi.app.ui.theme.SecondaryContainer
import com.lissafi.app.ui.theme.Success
import com.lissafi.app.ui.theme.Surface
import com.lissafi.app.ui.theme.SurfaceAlt
import com.lissafi.app.ui.theme.TextSecondary
import com.lissafi.app.ui.theme.Warning
import com.lissafi.app.ui.viewmodel.ClientViewModel
import kotlinx.coroutines.launch

private const val DAY_MS = 24 * 60 * 60 * 1000L
private const val RECENT_WINDOW_DAYS = 30

// ============================================================
// FILTRE LOCAL DE LA LISTE — ne touche pas au ViewModel
// ============================================================
private enum class ClientFilter(val label: String) {
    TOUS("Tous"),
    AVEC_DETTE("Avec dette"),
    RECENTS("Récents")
}

@Composable
fun ClientsScreen(viewModel: ClientViewModel, onClientClick: (String) -> Unit, onBack: () -> Unit, onNavigateToUpgrade: () -> Unit = {}) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(ClientFilter.TOUS) }

    val haptic = LocalHapticFeedback.current
    val recentCutoff = System.currentTimeMillis() - RECENT_WINDOW_DAYS * DAY_MS
    val debtCount = state.clients.count { it.totalDebt > 0 }
    val recentCount = state.clients.count { it.updatedAt >= recentCutoff }
    val filtered = when (filter) {
        ClientFilter.TOUS -> state.clients
        ClientFilter.AVEC_DETTE -> state.clients.filter { it.totalDebt > 0 }
        ClientFilter.RECENTS -> state.clients.filter { it.updatedAt >= recentCutoff }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        LissafiHeader(
            title = "Clients",
            subtitle = null,
            onBack = onBack
        )

        SearchField(
            value = state.searchQuery,
            onValueChange = { viewModel.search(it) },
            placeholder = "Rechercher un client…",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ClientFilter.values().forEach { f ->
                val count = when (f) {
                    ClientFilter.TOUS -> state.clients.size
                    ClientFilter.AVEC_DETTE -> debtCount
                    ClientFilter.RECENTS -> recentCount
                }
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = f.label,
                                fontSize = 12.sp,
                                fontWeight = if (filter == f) FontWeight.Medium else FontWeight.Normal
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "$count",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (filter == f) OnPrimary else TextSecondary
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = OnPrimary,
                        containerColor = SurfaceAlt,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        val totalDettes = state.clients.sumOf { it.totalDebt }
        if (state.searchQuery.isBlank() && totalDettes > 0) {
            LissafiCard(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                cornerRadius = 18,
                elevation = 2,
                containerColor = SecondaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconCircle(
                        icon = LissafiIcons.Encaisser,
                        backgroundColor = Secondary.copy(alpha = 0.1f),
                        iconTint = Secondary,
                        size = 40,
                        iconSize = 20
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Total à recouvrer",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSecondaryContainer
                        )
                        AmountText(amount = totalDettes, fontSize = 20, color = Secondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            EmptyState(
                icon = LissafiIcons.Client,
                title = if (state.searchQuery.isBlank()) "Aucun client" else "Aucun résultat",
                message = if (state.searchQuery.isBlank())
                    "Ajoute ton premier client pour vendre à crédit !"
                else
                    "Aucun client ne correspond à « ${state.searchQuery} ».",
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                actionLabel = if (state.searchQuery.isBlank()) "Ajouter un client" else null,
                onAction = {
                    scope.launch {
                        if (viewModel.canAddClient()) showAddDialog = true
                        else showPremiumDialog = true
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                items(filtered) { client ->
                    val daysSince = (System.currentTimeMillis() - client.updatedAt) / DAY_MS
                    ClientCard(
                        client = client,
                        daysSince = daysSince.toInt(),
                        onClick = { onClientClick(client.id) }
                    )
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    PrimaryActionButton(
                        text = "Ajouter un client",
                        icon = LissafiIcons.Ajouter,
                        onClick = {
                            scope.launch {
                                if (viewModel.canAddClient()) showAddDialog = true
                                else showPremiumDialog = true
                            }
                        }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddClientDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, phone ->
                viewModel.addClient(name, phone)
                showAddDialog = false
            }
        )
    }

    if (showPremiumDialog) {
        // Limite atteinte → pop-up premium (remplace le Toast)
        PremiumLimitDialog(
            message = "Passe à Lissafi Premium pour ajouter autant de clients que tu veux. Sans limite.",
            onUpgrade = {
                showPremiumDialog = false
                onNavigateToUpgrade()
            },
            onDismiss = { showPremiumDialog = false }
        )
    }
}

// ============================================================
// CARTE CLIENT
// ============================================================
@Composable
private fun ClientCard(
    client: Client,
    daysSince: Int,
    onClick: () -> Unit
) {
    val hasDebt = client.totalDebt > 0
    // L'urgence ne concerne que les clients qui doivent encore de l'argent
    val (badge, badgeColor) = when {
        !hasDebt -> null to Success
        daysSince > 21 -> "Urgent" to Error
        daysSince > 7 -> "À relancer" to Warning
        else -> null to Success
    }

    LissafiCard(modifier = Modifier.padding(vertical = 4.dp), onClick = onClick, cornerRadius = 18, elevation = 2) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar avec code couleur selon le retard
            val avatarColor = badgeColor.takeIf { badge != null } ?: Primary
            IconCircle(
                icon = LissafiIcons.Client,
                backgroundColor = avatarColor.copy(alpha = 0.12f),
                iconTint = avatarColor,
                size = 48,
                iconSize = 24
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (client.phone.isNotBlank()) client.phone
                    else "Vu ${FormatUtils.formatDateShort(client.updatedAt)}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (hasDebt && badge != null) {
                    Spacer(Modifier.height(4.dp))
                    StatusBadge(text = badge, color = badgeColor)
                } else if (!hasDebt) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "À jour · ${FormatUtils.formatDateShort(client.updatedAt)}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Dette",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(2.dp))
                AmountText(
                    amount = client.totalDebt,
                    fontSize = 18,
                    color = when {
                        !hasDebt -> TextSecondary
                        daysSince > 21 -> Error
                        else -> Secondary
                    }
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ============================================================
// DIALOGUE AJOUT CLIENT
// ============================================================
@Composable
private fun AddClientDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

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
                    imageVector = LissafiIcons.Client,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Nouveau client", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OnBackground, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(text = "Le nom suffit pour commencer.", fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center)
            }
        },
        text = {
            Column {
                CapsuleTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Nom du client *",
                    leadingIcon = LissafiIcons.Client
                )
                Spacer(Modifier.height(10.dp))
                CapsuleTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } },
                    placeholder = "Téléphone (optionnel)",
                    leadingIcon = LissafiIcons.Client
                )
            }
        },
        confirmButton = {
            PrimaryActionButton(
                text = "ENREGISTRER",
                onClick = {
                    if (name.isNotBlank()) onSave(name.trim(), phone.trim())
                },
                enabled = name.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = TextSecondary) }
        }
    )
}
