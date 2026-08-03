package com.lissafi.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.theme.LissafiDanger
import com.lissafi.app.ui.theme.LissafiGreen
import com.lissafi.app.ui.theme.LissafiOrange
import com.lissafi.app.ui.theme.LissafiCream
import com.lissafi.app.ui.theme.LissafiWhite
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
                Text("← RETOUR", color = LissafiWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Résumé client
        client?.let { cl ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = LissafiWhite)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(cl.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (cl.phone.isNotBlank()) {
                        Text(cl.phone, fontSize = 14.sp, color = LissafiOrange)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Dette totale : ${FormatUtils.formatFCFA(cl.totalDebt)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cl.totalDebt > 0) LissafiDanger else LissafiGreen
                    )
                }
            }

            // Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddDebtDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = LissafiOrange)
                ) {
                    Text("+ Vente à crédit")
                }
                Button(
                    onClick = { showRepayDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = LissafiGreen)
                ) {
                    Text("💰 Remboursement")
                }
            }
        }

        // Historique
        Text(
            "HISTORIQUE",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = LissafiGreen,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(modifier = Modifier.padding(horizontal = 12.dp)) {
            items(transactions) { txn ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = LissafiWhite)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (txn.amount >= 0) "Vente à crédit" else "Remboursement",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (txn.note.isNotBlank()) {
                                Text(txn.note, fontSize = 12.sp)
                            }
                            Text(FormatUtils.formatDate(txn.date), fontSize = 11.sp)
                        }
                        Text(
                            FormatUtils.formatFCFA(txn.amount),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (txn.amount >= 0) LissafiDanger else LissafiGreen
                        )
                    }
                }
            }
        }
    }

    // Dialog vente à crédit
    if (showAddDebtDialog) {
        var amount by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDebtDialog = false },
            title = { Text("Vente à crédit") },
            text = {
                Column {
                    OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() } }, label = { Text("Montant (FCFA) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (optionnel)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amount.toIntOrNull() ?: 0
                    if (amt > 0) {
                        viewModel.addDebt(clientId, amt, note)
                        showAddDebtDialog = false
                    }
                }) { Text("AJOUTER") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDebtDialog = false }) { Text("Annuler") }
            }
        )
    }

    // Dialog remboursement
    if (showRepayDialog) {
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRepayDialog = false },
            title = { Text("Remboursement") },
            text = {
                Column {
                    Text("Dette actuelle : ${FormatUtils.formatFCFA(client?.totalDebt ?: 0)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() } }, label = { Text("Montant remboursé (FCFA) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amount.toIntOrNull() ?: 0
                    if (amt > 0) {
                        viewModel.addRepayment(clientId, amt)
                        showRepayDialog = false
                    }
                }) { Text("ENREGISTRER") }
            },
            dismissButton = {
                TextButton(onClick = { showRepayDialog = false }) { Text("Annuler") }
            }
        )
    }
}
