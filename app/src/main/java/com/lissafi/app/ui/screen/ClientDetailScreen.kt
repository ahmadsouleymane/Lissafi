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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.data.entity.Client
import com.lissafi.app.data.entity.DebtTransaction
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.components.AmountField
import com.lissafi.app.ui.components.AmountText
import com.lissafi.app.ui.components.EmptyState
import com.lissafi.app.ui.components.IconCircle
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.components.PrimaryActionButton
import com.lissafi.app.ui.components.QuickAmountChips
import com.lissafi.app.ui.components.SecondaryActionButton
import com.lissafi.app.ui.components.SectionHeader
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.Border
import com.lissafi.app.ui.theme.Error
import com.lissafi.app.ui.theme.OnBackground
import com.lissafi.app.ui.theme.OnPrimary
import com.lissafi.app.ui.theme.OnSecondary
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.Secondary
import com.lissafi.app.ui.theme.Success
import com.lissafi.app.ui.theme.SuccessContainer
import com.lissafi.app.ui.theme.Surface
import com.lissafi.app.ui.theme.SurfaceAlt
import com.lissafi.app.ui.theme.TextSecondary
import com.lissafi.app.ui.theme.TextTertiary
import com.lissafi.app.ui.viewmodel.ClientViewModel

@Composable
fun ClientDetailScreen(
    clientId: String,
    viewModel: ClientViewModel,
    onBack: () -> Unit
) {
    val client by viewModel.selectedClient.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val haptic = LocalHapticFeedback.current
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showRepayDialog by remember { mutableStateOf(false) }
    val hasDebt = (client?.totalDebt ?: 0) > 0

    LaunchedEffect(clientId) {
        viewModel.loadClient(clientId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(Modifier.fillMaxSize()) {
            LissafiHeader(
                title = client?.name ?: "Client",
                subtitle = client?.let { "Depuis le ${FormatUtils.formatDateShort(it.createdAt)}" }
                    ?: "Suivi des dettes",
                onBack = onBack
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // ── CARTE DETTE ──
                item {
                    client?.let { DebtCard(it, transactions) }
                }

                // ── BOUTON REMBOURSER (pilule) ──
                if (hasDebt) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SecondaryActionButton(
                                text = "Rembourser",
                                icon = LissafiIcons.Rembourser,
                                onClick = { showRepayDialog = true },
                                modifier = Modifier.fillMaxWidth(0.7f),
                                height = 48
                            )
                        }
                    }
                }

                // ── MINI STATS ──
                item {
                    client?.let { MiniStats(it, transactions) }
                }

                // ── TITRE TRANSACTIONS ──
                item {
                    SectionHeader(
                        text = "DERNIÈRES TRANSACTIONS",
                        icon = LissafiIcons.Recents,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                // ── LISTE TRANSACTIONS ──
                if (transactions.isEmpty()) {
                    item {
                        EmptyState(
                            icon = LissafiIcons.Credit,
                            title = "Aucune transaction",
                            message = "Les ventes à crédit et les remboursements\napparaîtront ici."
                        )
                    }
                } else {
                    items(transactions, key = { it.id }) { txn ->
                        TransactionRow(txn.amount, txn.note, txn.date)
                        Spacer(Modifier.height(6.dp))
                    }
                }

                // Espace pour le FAB
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        // ── FAB ──
        FloatingActionButton(
            onClick = { showAddDebtDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = Primary,
            contentColor = OnPrimary,
            shape = RoundedCornerShape(16.dp),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            )
        ) {
            Icon(
                imageVector = LissafiIcons.Ajouter,
                contentDescription = "Ajouter une dette",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // ── DIALOGUES ──
    if (showAddDebtDialog) {
        AddDebtDialog(
            onDismiss = { showAddDebtDialog = false },
            onSave = { amount, note ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addDebt(clientId, amount, note)
                showAddDebtDialog = false
            }
        )
    }

    if (showRepayDialog) {
        RepayDialog(
            currentDebt = client?.totalDebt ?: 0,
            onDismiss = { showRepayDialog = false },
            onSave = { amount ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addRepayment(clientId, amount)
                showRepayDialog = false
            }
        )
    }
}

// ============================================================
// CARTE DETTE — Orange, proéminente, avec barre de progression
// ============================================================
@Composable
private fun DebtCard(client: Client, transactions: List<DebtTransaction>) {
    val hasDebt = client.totalDebt > 0
    val bg = if (hasDebt) Secondary else Primary
    val fg = if (hasDebt) OnSecondary else OnPrimary
    val totalCredit = transactions.filter { it.amount > 0 }.sumOf { it.amount }
    val totalRepaid = transactions.filter { it.amount < 0 }.sumOf { -it.amount }
    val progress = if (totalCredit > 0) (totalRepaid.toFloat() / totalCredit).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = LissafiIcons.Credit,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Dette actuelle",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = fg.copy(alpha = 0.9f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = FormatUtils.formatFCFA(client.totalDebt),
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                color = fg,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (hasDebt) "En attente de remboursement" else "Ce client ne te doit rien",
                fontSize = 13.sp,
                color = fg.copy(alpha = 0.8f)
            )

            if (hasDebt && totalCredit > 0) {
                Spacer(Modifier.height(20.dp))
                // Barre de progression
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(fg.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(fg)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Remboursé ${FormatUtils.formatFCFA(totalRepaid)}",
                        fontSize = 12.sp,
                        color = fg.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Crédité ${FormatUtils.formatFCFA(totalCredit)}",
                        fontSize = 12.sp,
                        color = fg.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ============================================================
// MINI-STATS — Trois tuiles de stats
// ============================================================
@Composable
private fun MiniStats(client: Client, transactions: List<DebtTransaction>) {
    val purchaseCount = transactions.count { it.amount > 0 }
    val totalRepaid = transactions.filter { it.amount < 0 }.sumOf { -it.amount }
    val daysSince = ((System.currentTimeMillis() - client.createdAt) / (24 * 3600 * 1000)).toInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatMiniCard(
            icon = LissafiIcons.Panier,
            value = "$purchaseCount",
            label = "ventes à crédit",
            color = Secondary,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        StatMiniCard(
            icon = LissafiIcons.Rembourser,
            value = FormatUtils.formatFCFA(totalRepaid).removeSuffix(" FCFA"),
            label = "total remboursé",
            color = Success,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        StatMiniCard(
            icon = LissafiIcons.Recents,
            value = if (daysSince <= 0) "Aujourd'hui" else "Il y a ${daysSince}j",
            label = "client depuis",
            color = Primary,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

@Composable
private fun StatMiniCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    LissafiCard(modifier = modifier, cornerRadius = 18, elevation = 2) {
        Column(Modifier.padding(16.dp)) {
            IconCircle(
                icon = icon,
                backgroundColor = color.copy(alpha = 0.08f),
                iconTint = color,
                size = 36,
                iconSize = 18
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

// ============================================================
// LIGNE TRANSACTION — Pastille colorée + infos + montant
// ============================================================
@Composable
private fun TransactionRow(amount: Int, note: String, date: Long) {
    val isCredit = amount >= 0
    val color = if (isCredit) Secondary else Success
    val icon = if (isCredit) LissafiIcons.Credit else LissafiIcons.Rembourser
    val label = if (isCredit) "Vente à crédit" else "Remboursement"
    val bgColor = if (isCredit) Secondary.copy(alpha = 0.08f) else SuccessContainer

    LissafiCard(cornerRadius = 16, elevation = 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pastille cerclée
            IconCircle(
                icon = icon,
                backgroundColor = bgColor,
                iconTint = color,
                size = 42,
                iconSize = 20
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = OnBackground
                )
                if (note.isNotBlank()) {
                    Text(
                        text = note,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = FormatUtils.formatDate(date),
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
            Text(
                text = if (isCredit) "+${FormatUtils.formatFCFA(amount)}" else "-${FormatUtils.formatFCFA(-amount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color
            )
        }
    }
}

// ============================================================
// DIALOGUE AJOUT DE DETTE
// ============================================================
@Composable
private fun AddDebtDialog(
    onDismiss: () -> Unit,
    onSave: (amount: Int, note: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Surface,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LissafiIcons.Credit,
                    contentDescription = null,
                    tint = Secondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Ajouter une dette",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OnBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Le client paiera plus tard.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column {
                AmountField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Montant de la vente *"
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optionnel)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            PrimaryActionButton(
                text = "AJOUTER À LA DETTE",
                onClick = {
                    val amt = amount.toIntOrNull() ?: 0
                    if (amt > 0) onSave(amt, note.trim())
                },
                enabled = (amount.toIntOrNull() ?: 0) > 0,
                containerColor = Secondary
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Annuler", color = TextSecondary, fontSize = 14.sp)
            }
        }
    )
}

// ============================================================
// DIALOGUE REMBOURSEMENT
// ============================================================
@Composable
private fun RepayDialog(
    currentDebt: Int,
    onDismiss: () -> Unit,
    onSave: (amount: Int) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    val amt = amount.toIntOrNull() ?: 0

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
                    imageVector = LissafiIcons.Rembourser,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Remboursement",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OnBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Dette actuelle : ${FormatUtils.formatFCFA(currentDebt)}",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column {
                AmountField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Montant reçu *"
                )
                Spacer(Modifier.height(12.dp))
                QuickAmountChips(
                    amounts = listOf(1000, 2000, 5000),
                    current = amt,
                    onSelect = { amount = it.toString() }
                )
                Spacer(Modifier.height(8.dp))
                FilterChip(
                    selected = currentDebt > 0 && amt == currentDebt,
                    onClick = { amount = currentDebt.toString() },
                    label = {
                        Text(
                            text = "Solde complet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = OnPrimary,
                        containerColor = Surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (amt > currentDebt) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Le montant dépasse la dette. Il sera plafonné à ${FormatUtils.formatFCFA(currentDebt)}.",
                        fontSize = 12.sp,
                        color = Error,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            PrimaryActionButton(
                text = "ENREGISTRER LE PAIEMENT",
                icon = LissafiIcons.Valider,
                onClick = { if (amt > 0) onSave(if (amt > currentDebt) currentDebt else amt) },
                enabled = amt > 0 && amt <= currentDebt
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Annuler", color = TextSecondary, fontSize = 14.sp)
            }
        }
    )
}
