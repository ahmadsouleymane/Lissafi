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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.data.entity.Client
import com.lissafi.app.data.entity.DebtTransaction
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.components.AmountField
import com.lissafi.app.ui.components.AmountText
import com.lissafi.app.ui.components.EmptyState
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.components.PrimaryActionButton
import com.lissafi.app.ui.components.QuickAmountChips
import com.lissafi.app.ui.components.SecondaryActionButton
import com.lissafi.app.ui.components.SectionHeader
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.Error
import com.lissafi.app.ui.theme.OnBackground
import com.lissafi.app.ui.theme.OnPrimary
import com.lissafi.app.ui.theme.OnSecondary
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.Secondary
import com.lissafi.app.ui.theme.Success
import com.lissafi.app.ui.theme.Surface
import com.lissafi.app.ui.theme.TextSecondary
import com.lissafi.app.ui.viewmodel.ClientViewModel

@Composable
fun ClientDetailScreen(
    clientId: String,
    viewModel: ClientViewModel,
    onBack: () -> Unit
) {
    val client by viewModel.selectedClient.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
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
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                item {
                    client?.let { DebtCard(it, transactions) }
                }

                if (hasDebt) {
                    item {
                        SecondaryActionButton(
                            text = "Rembourser",
                            icon = LissafiIcons.Rembourser,
                            onClick = { showRepayDialog = true },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }
                }

                item {
                    client?.let { MiniStats(it, transactions) }
                }

                item {
                    SectionHeader(
                        text = "DERNIÈRES TRANSACTIONS",
                        icon = LissafiIcons.Recents,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                if (transactions.isEmpty()) {
                    item {
                        EmptyState(
                            icon = LissafiIcons.Credit,
                            title = "Aucune transaction",
                            message = "Les ventes à crédit et les remboursements\napparaîtront ici.",
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    items(transactions, key = { it.id }) { txn ->
                        TransactionRow(txn.amount, txn.note, txn.date)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDebtDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = Primary,
            contentColor = OnPrimary,
            shape = CircleShape
        ) {
            Icon(
                imageVector = LissafiIcons.Ajouter,
                contentDescription = "Ajouter une dette"
            )
        }
    }

    // ── DIALOGUE AJOUT DE DETTE ──
    if (showAddDebtDialog) {
        AddDebtDialog(
            onDismiss = { showAddDebtDialog = false },
            onSave = { amount, note ->
                viewModel.addDebt(clientId, amount, note)
                showAddDebtDialog = false
            }
        )
    }

    // ── DIALOGUE REMBOURSEMENT ──
    if (showRepayDialog) {
        RepayDialog(
            currentDebt = client?.totalDebt ?: 0,
            onDismiss = { showRepayDialog = false },
            onSave = { amount ->
                viewModel.addRepayment(clientId, amount)
                showRepayDialog = false
            }
        )
    }
}

// ============================================================
// CARTE DETTE — Proéminente, avec barre de progression
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = LissafiIcons.Credit,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Dette actuelle",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = fg.copy(alpha = 0.9f)
                )
            }
            Spacer(Modifier.height(8.dp))
            AmountText(
                amount = client.totalDebt,
                fontSize = 36,
                color = fg,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (hasDebt) "En attente de remboursement" else "Ce client ne te doit rien",
                fontSize = 12.sp,
                color = fg.copy(alpha = 0.8f)
            )
            if (hasDebt) {
                Spacer(Modifier.height(16.dp))
                // Barre de progression : part remboursée du total accordé
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
                        color = fg.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Crédité ${FormatUtils.formatFCFA(totalCredit)}",
                        fontSize = 12.sp,
                        color = fg.copy(alpha = 0.9f)
                    )
                }
            } else {
                Spacer(Modifier.height(12.dp))
                DebtStatRow("Total crédit accordé", FormatUtils.formatFCFA(totalCredit), fg.copy(alpha = 0.9f))
                DebtStatRow("Total remboursé", FormatUtils.formatFCFA(totalRepaid), fg.copy(alpha = 0.9f))
            }
        }
    }
}

// Ligne label + valeur, lisible sur fond coloré (InfoRow est prévu pour fond clair)
@Composable
private fun DebtStatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = valueColor)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

// ============================================================
// MINI-STATS — Achats + ancienneté
// ============================================================
@Composable
private fun MiniStats(client: Client, transactions: List<DebtTransaction>) {
    val purchaseCount = transactions.count { it.amount > 0 }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatMiniCard(
            icon = LissafiIcons.Panier,
            value = "$purchaseCount",
            label = "ventes à crédit",
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            icon = LissafiIcons.Recents,
            value = FormatUtils.formatDateShort(client.createdAt),
            label = "client depuis",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatMiniCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    LissafiCard(modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

// ============================================================
// LIGNE TRANSACTION
// ============================================================
@Composable
private fun TransactionRow(amount: Int, note: String, date: Long) {
    val isCredit = amount >= 0
    val color = if (isCredit) Error else Success
    val icon = if (isCredit) LissafiIcons.Credit else LissafiIcons.Rembourser
    val label = if (isCredit) "Vente à crédit" else "Remboursement"

    LissafiCard(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
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
                    color = TextSecondary
                )
            }
            Text(
                text = if (isCredit) "+${FormatUtils.formatFCFA(amount)}" else "-${FormatUtils.formatFCFA(-amount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
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
        shape = RoundedCornerShape(26.dp),
        icon = {
            Icon(
                imageVector = LissafiIcons.Credit,
                contentDescription = null,
                tint = Secondary,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Column {
                Text(text = "Ajouter une dette", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Le client paiera plus tard. Le montant sera ajouté à sa dette.",
                    fontSize = 12.sp,
                    color = TextSecondary
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
            TextButton(onClick = onDismiss) { Text("Annuler", color = TextSecondary) }
        }
    )
}

// ============================================================
// DIALOGUE REMBOURSEMENT — avec montants rapides
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
        shape = RoundedCornerShape(26.dp),
        icon = {
            Icon(
                imageVector = LissafiIcons.Rembourser,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Column {
                Text(text = "Remboursement", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Dette actuelle : ${FormatUtils.formatFCFA(currentDebt)}",
                    fontSize = 13.sp,
                    color = TextSecondary
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
                // Montants rapides
                QuickAmountChips(
                    amounts = listOf(1000, 2000, 5000),
                    current = amt,
                    onSelect = { amount = it.toString() }
                )
                Spacer(Modifier.height(8.dp))
                // Tout rembourser
                FilterChip(
                    selected = currentDebt > 0 && amt == currentDebt,
                    onClick = { amount = currentDebt.toString() },
                    label = {
                        Text(
                            text = "Solde complet",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = OnPrimary,
                        containerColor = Surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                // Avertissement si le montant dépasse la dette
                if (amt > currentDebt) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Le montant reçu dépasse la dette de ${FormatUtils.formatFCFA(currentDebt)}. Le remboursement sera plafonné.",
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
                onClick = {
                    if (amt > 0) onSave(if (amt > currentDebt) currentDebt else amt)
                },
                enabled = amt > 0 && amt <= currentDebt
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = TextSecondary) }
        }
    )
}
