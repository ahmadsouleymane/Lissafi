package com.lissafi.app.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.data.entity.Product
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.theme.LissafiOrange
import com.lissafi.app.ui.theme.LissafiGreen
import com.lissafi.app.ui.theme.LissafiCream
import com.lissafi.app.ui.theme.LissafiWhite
import com.lissafi.app.ui.theme.LissafiBlack
import com.lissafi.app.ui.theme.LissafiDanger
import com.lissafi.app.ui.viewmodel.CartViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun CaisseScreen(
    viewModel: CartViewModel,
    onNavigateToProducts: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var showEncaisseDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var changeGiven by remember { mutableStateOf(0) }
    var showChangeDialog by remember { mutableStateOf(false) }

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
            Text("LISSAFI", color = LissafiWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row {
                TextButton(onClick = onNavigateToReports) {
                    Text("📊", fontSize = 20.sp)
                }
                TextButton(onClick = onNavigateToSettings) {
                    Text("⚙️", fontSize = 20.sp)
                }
            }
        }

        // Barre de recherche / scan
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { showScannerDialog = true },
                modifier = Modifier.height(48.dp)
            ) {
                Text("🔍 Scanner")
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { onNavigateToProducts },
                modifier = Modifier.height(48.dp)
            ) {
                Text("📦 Produits")
            }
        }

        // Panier
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(containerColor = LissafiWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "PANIER (${state.items.size} article${if (state.items.size > 1) "s" else ""})",
                    fontWeight = FontWeight.Bold,
                    color = LissafiGreen,
                    fontSize = 14.sp
                )

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(state.items) { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.removeItem(index) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.name, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text("×${item.quantity}", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                            Text(
                                FormatUtils.formatFCFA((item.price * item.quantity).toInt()),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Total
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTAL", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        FormatUtils.formatFCFA(state.total),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = LissafiGreen
                    )
                }
            }
        }

        // Mode de paiement
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mode : ", fontSize = 14.sp)
            TextButton(onClick = { viewModel.setCreditMode(false) }) {
                Text(
                    "○ Comptant",
                    fontWeight = if (!state.isCredit) FontWeight.Bold else FontWeight.Normal,
                    color = if (!state.isCredit) LissafiGreen else LissafiBlack
                )
            }
            TextButton(onClick = {
                viewModel.setCreditMode(true)
                onNavigateToClients()
            }) {
                Text(
                    "● Crédit",
                    fontWeight = if (state.isCredit) FontWeight.Bold else FontWeight.Normal,
                    color = if (state.isCredit) LissafiOrange else LissafiBlack
                )
            }
        }
        if (state.isCredit && state.selectedClient != null) {
            Text(
                "Client : ${state.selectedClient!!.name}",
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = LissafiOrange
            )
        }

        // Bouton encaisser
        Button(
            onClick = {
                if (state.items.isEmpty()) {
                    Toast.makeText(context, "Panier vide", Toast.LENGTH_SHORT).show()
                } else if (state.total <= 0) {
                    Toast.makeText(context, "Ajoutez des articles", Toast.LENGTH_SHORT).show()
                } else if (state.isCredit) {
                    // Crédit : encaisser directement
                    CoroutineScope(Dispatchers.IO).launch {
                        viewModel.encaisser(0)
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Crédit enregistré !", Toast.LENGTH_SHORT).show()
                            viewModel.clearCart()
                        }
                    }
                } else {
                    showEncaisseDialog = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LissafiGreen)
        ) {
            Text(
                "ENCAISSER  ${FormatUtils.formatFCFA(state.total)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Produits récents
        if (state.recentProducts.isNotEmpty()) {
            Text(
                "PRODUITS RÉCENTS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = LissafiGreen
            )
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.recentProducts) { product ->
                    Card(
                        modifier = Modifier
                            .clickable { viewModel.scanProduct(product.barcode) }
                            .width(100.dp),
                        colors = CardDefaults.cardColors(containerColor = LissafiWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(product.name, fontSize = 11.sp, maxLines = 1)
                            Text(
                                FormatUtils.formatFCFA(product.sellPrice),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LissafiGreen
                            )
                        }
                    }
                }
                item {
                    Card(
                        modifier = Modifier
                            .clickable { showAddProductDialog = true }
                            .width(80.dp),
                        colors = CardDefaults.cardColors(containerColor = LissafiOrange.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+ Ajout", fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }

    // Dialog encaisser comptant
    if (showEncaisseDialog) {
        AlertDialog(
            onDismissRequest = { showEncaisseDialog = false },
            title = { Text("Montant donné par le client ?") },
            text = {
                Column {
                    Text("Total à payer : ${FormatUtils.formatFCFA(state.total)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                        label = { Text("Montant (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val paid = amountText.toIntOrNull() ?: 0
                    if (paid >= state.total) {
                        changeGiven = paid - state.total
                        showEncaisseDialog = false
                        showChangeDialog = true
                        CoroutineScope(Dispatchers.IO).launch {
                            viewModel.encaisser(paid)
                            launch(Dispatchers.Main) {
                                viewModel.clearCart()
                            }
                        }
                    }
                }) { Text("Valider") }
            },
            dismissButton = {
                TextButton(onClick = { showEncaisseDialog = false }) { Text("Annuler") }
            }
        )
    }

    // Dialog monnaie rendue
    if (showChangeDialog) {
        AlertDialog(
            onDismissRequest = { showChangeDialog = false },
            title = { Text("Monnaie à rendre") },
            text = {
                Text(
                    "Rendre ${FormatUtils.formatFCFA(changeGiven)} au client",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    showChangeDialog = false
                    amountText = ""
                }) { Text("OK") }
            }
        )
    }

    // Dialog ajout rapide
    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onAdd = { product ->
                viewModel.addProductDirectly(product)
                showAddProductDialog = false
            }
        )
    }

    // Dialog scan
    if (showScannerDialog) {
        BarcodeScannerDialog(
            onDismiss = { showScannerDialog = false },
            onBarcodeScanned = { barcode ->
                viewModel.scanProduct(barcode)
                showScannerDialog = false
            }
        )
    }
}

@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onAdd: (Product) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajout rapide") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom du produit") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = sellPrice, onValueChange = { sellPrice = it.filter { c -> c.isDigit() } }, label = { Text("Prix de vente (FCFA)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = buyPrice, onValueChange = { buyPrice = it.filter { c -> c.isDigit() } }, label = { Text("Prix d'achat (FCFA, optionnel)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = quantity, onValueChange = { quantity = it.filter { c -> c.isDigit() } }, label = { Text("Quantité initiale (optionnel)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                val price = sellPrice.toIntOrNull() ?: 0
                if (name.isNotBlank() && price > 0) {
                    val product = Product(
                        barcode = "MANUAL-${System.currentTimeMillis()}",
                        name = name.trim(),
                        sellPrice = price,
                        buyPrice = buyPrice.toIntOrNull() ?: 0,
                        stock = quantity.toIntOrNull() ?: 0,
                        hasBarcode = false
                    )
                    onAdd(product)
                }
            }) { Text("AJOUTER") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun BarcodeScannerDialog(
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    var barcodeText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scanner ou saisir un code-barres") },
        text = {
            Column {
                Text(
                    "Placez le code-barres devant la caméra ou saisissez-le manuellement :",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = barcodeText,
                    onValueChange = { barcodeText = it },
                    label = { Text("Code-barres") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (barcodeText.isNotBlank()) {
                    onBarcodeScanned(barcodeText.trim())
                }
            }) { Text("AJOUTER") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
