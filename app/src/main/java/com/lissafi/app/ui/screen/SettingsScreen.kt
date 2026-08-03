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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.theme.LissafiGreen
import com.lissafi.app.ui.theme.LissafiOrange
import com.lissafi.app.ui.theme.LissafiCream
import com.lissafi.app.ui.theme.LissafiWhite
import com.lissafi.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showShopDialog by remember { mutableStateOf(false) }

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
                Text("← PARAMÈTRES", color = LissafiWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Statut Premium
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LissafiWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("STATUT", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LissafiGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (state.isPremium) "● Premium" else "○ Gratuit")
                        if (state.isPremium) {
                            Text(
                                "Expire le ${FormatUtils.formatDate(state.premiumExpiry ?: 0)}",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Boutique
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LissafiWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("BOUTIQUE", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LissafiGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nom : ${state.shopName.ifBlank { "Non défini" }}")
                    Text("Tél : ${state.shopPhone.ifBlank { "Non défini" }}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showShopDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = LissafiOrange)
                    ) {
                        Text("MODIFIER")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Admin
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LissafiWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ADMINISTRATION", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LissafiGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onNavigateToAdmin,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = LissafiGreen)
                    ) {
                        Text("🔒 Administration")
                    }
                }
            }
        }
    }

    // Dialog modification boutique
    if (showShopDialog) {
        var name by remember { mutableStateOf(state.shopName) }
        var phone by remember { mutableStateOf(state.shopPhone) }

        AlertDialog(
            onDismissRequest = { showShopDialog = false },
            title = { Text("Modifier la boutique") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom de la boutique") }, singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Téléphone") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveShopInfo(name.trim(), phone.trim())
                    showShopDialog = false
                }) { Text("ENREGISTRER") }
            },
            dismissButton = {
                TextButton(onClick = { showShopDialog = false }) { Text("Annuler") }
            }
        )
    }
}
