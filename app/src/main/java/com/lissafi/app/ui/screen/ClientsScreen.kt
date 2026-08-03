package com.lissafi.app.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.data.entity.Client
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.theme.LissafiDanger
import com.lissafi.app.ui.theme.LissafiGreen
import com.lissafi.app.ui.theme.LissafiOrange
import com.lissafi.app.ui.theme.LissafiCream
import com.lissafi.app.ui.theme.LissafiWhite
import com.lissafi.app.ui.theme.LissafiWarning
import com.lissafi.app.ui.viewmodel.ClientViewModel
import kotlinx.coroutines.launch

@Composable
fun ClientsScreen(
    viewModel: ClientViewModel,
    onClientClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    val totalDebt = state.clients.sumOf { it.totalDebt }

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
                Text("← CRÉDITS CLIENTS", color = LissafiWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    scope.launch {
                        if (viewModel.canAddClient()) {
                            showAddDialog = true
                        } else {
                            Toast.makeText(
                                context,
                                "Limite de 10 débiteurs atteinte. Passe Premium !",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LissafiOrange)
            ) {
                Text("+ Client", fontSize = 13.sp)
            }
        }

        // Total dû
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = LissafiWhite)
        ) {
            Text(
                "Total dû : ${FormatUtils.formatFCFA(totalDebt)}",
                modifier = Modifier.padding(12.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = LissafiOrange,
                textAlign = TextAlign.Center,
            )
        }

        // Barre de recherche
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.search(it) },
            label = { Text("🔍 Rechercher un client") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            singleLine = true
        )

        // Liste
        LazyColumn(modifier = Modifier.padding(horizontal = 12.dp)) {
            items(state.clients) { client ->
                ClientCard(
                    client = client,
                    onClick = { onClientClick(client.id) }
                )
            }

            if (!state.isPremium) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = LissafiOrange.copy(alpha = 0.15f))
                    ) {
                        Text(
                            "${state.clients.size}/10 débiteurs (Gratuit) ⬆ Passe Premium",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            color = LissafiOrange,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Dialog ajout client
    if (showAddDialog) {
        AddClientDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, phone ->
                viewModel.addClient(name, phone)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ClientCard(client: Client, onClick: () -> Unit) {
    val daysSinceLastTransaction = client.updatedAt.let { last ->
        (System.currentTimeMillis() - last) / (24 * 60 * 60 * 1000)
    }
    val indicator = when {
        daysSinceLastTransaction > 21 -> "🔴"
        daysSinceLastTransaction > 7 -> "⚠️"
        else -> ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = LissafiWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${client.name} $indicator",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    "Dernier achat : ${FormatUtils.formatDateShort(client.updatedAt)}",
                    fontSize = 12.sp
                )
            }
            Text(
                FormatUtils.formatFCFA(client.totalDebt),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (client.totalDebt > 0) LissafiDanger else LissafiGreen
            )
        }
    }
}

@Composable
fun AddClientDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau client") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom du client *") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } }, label = { Text("Téléphone") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onAdd(name.trim(), phone.trim()) }) { Text("AJOUTER") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
