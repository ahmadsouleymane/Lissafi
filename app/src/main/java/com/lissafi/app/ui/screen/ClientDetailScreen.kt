package com.lissafi.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.components.AmountField
import com.lissafi.app.ui.components.AmountText
import com.lissafi.app.ui.components.EmptyState
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
import com.lissafi.app.ui.theme.Success
import com.lissafi.app.ui.theme.White
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

    LaunchedEffect(clientId) {
        viewModel.loadClient(clientId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LissafiCream)
    ) {
        LissafiHeader(
            title = client?.name ?: "Client",
            subtitle = "Suivi des dettes",
            leadingIcon = Icons.Filled.People,
            onBack = onBack
        )

        client?.let { cl ->
            // ── CARTE PROFIL ──
            LissafiCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(LissafiGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = LissafiGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(cl.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (cl.phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = null,
                                tint = Neutral400,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(cl.phone, fontSize = 13.sp, color = Neutral400)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Spacer(Modifier.width(0.dp))
                    Text(
                        text = if (cl.totalDebt > 0) "Dette actuelle" else "Aucune dette",
                        color = Neutral500,
                        fontSize = 13.sp
                    )
                    AmountText(
                        amount = cl.totalDebt,
                        fontSize = 32,
                        color = if (cl.totalDebt > 0) Danger else Success
                    )
                    Spacer(Modifier.height(6.dp))
                    StatusBadge(
                        text = if (cl.totalDebt > 0) "À recouvrer" else "Client à jour",
                        color = if (cl.totalDebt > 0) LissafiOrange else Success
                    )
                }
            }

            // ── ACTIONS ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showAddDebtDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, LissafiOrange),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LissafiOrange)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Vente à crédit", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Button(
                    onClick = { showRepayDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LissafiGreen)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Remboursement", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── HISTORIQUE ──
        SectionHeader(
            text = "HISTORIQUE",
            icon = Icons.Filled.Receipt,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (transactions.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Receipt,
                title = "Aucune transaction",
                message = "Les ventes à crédit et les remboursements\napparaîtront ici.",
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                items(transactions, key = { it.id }) { txn ->
                    TransactionRow(txn.amount, txn.note, txn.date)
                }
            }
        }
    }

    // ── DIALOGUE VENTE À CRÉDIT ──
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
// LIGNE TRANSACTION
// ============================================================
@Composable
private fun TransactionRow(amount: Int, note: String, date: Long) {
    val isCredit = amount >= 0
    val color = if (isCredit) Danger else Success
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
                    imageVector = if (isCredit) Icons.Filled.CreditCard else Icons.Filled.Payments,
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
                        color = Neutral400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = FormatUtils.formatDate(date),
                    fontSize = 11.sp,
                    color = Neutral400
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
// DIALOGUE VENTE À CRÉDIT
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
                imageVector = Icons.Filled.CreditCard,
                contentDescription = null,
                tint = LissafiOrange,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Column {
                Text(text = "Vente à crédit", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Le client paiera plus tard. Le montant sera ajouté à sa dette.",
                    fontSize = 12.sp,
                    color = Neutral500
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
                containerColor = LissafiOrange
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = Neutral500) }
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
                imageVector = Icons.Filled.Payments,
                contentDescription = null,
                tint = LissafiGreen,
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
                    color = Neutral500
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
                // Montants rapides + tout rembourser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1000, 2000, 5000).forEach { quick ->
                        FilterChip(
                            selected = amt == quick,
                            onClick = { amount = quick.toString() },
                            label = {
                                Text(
                                    FormatUtils.formatFCFA(quick),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LissafiGreen,
                                selectedLabelColor = White,
                                containerColor = White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FilterChip(
                        selected = amt == currentDebt && currentDebt > 0,
                        onClick = { amount = currentDebt.toString() },
                        label = { Text("Solde complet", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LissafiGreen,
                            selectedLabelColor = White,
                            containerColor = White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            PrimaryActionButton(
                text = "ENREGISTRER LE PAIEMENT",
                onClick = {
                    if (amt > 0) onSave(amt)
                },
                enabled = amt > 0
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = Neutral500) }
        }
    )
}
