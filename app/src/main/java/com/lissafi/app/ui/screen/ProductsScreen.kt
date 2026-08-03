package com.lissafi.app.ui.screen

import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.data.entity.Product
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.theme.LissafiGreen
import com.lissafi.app.ui.theme.LissafiOrange
import com.lissafi.app.ui.theme.LissafiCream
import com.lissafi.app.ui.theme.LissafiWhite
import com.lissafi.app.ui.theme.LissafiDanger
import com.lissafi.app.ui.theme.LissafiWarning
import com.lissafi.app.ui.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

@Composable
fun ProductsScreen(
    viewModel: ProductViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }

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
                Text("← CATALOGUE", color = LissafiWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    scope.launch {
                        if (viewModel.canAddProduct()) {
                            showAddDialog = true
                        } else {
                            Toast.makeText(
                                context,
                                "Limite de 10 produits atteinte. Passe Premium !",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LissafiOrange)
            ) {
                Text("➕", fontSize = 18.sp)
            }
        }

        // Barre de recherche
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.search(it) },
            label = { Text("🔍 Rechercher") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            singleLine = true
        )

        // Liste
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            items(state.products) { product ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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
                            Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row {
                                Text(
                                    "Stock : ${if (product.hasBarcode) product.stock.toString() else "—"}",
                                    fontSize = 12.sp
                                )
                                if (product.hasBarcode && product.stock <= product.minStock) {
                                    Text(
                                        " ⚠️",
                                        fontSize = 12.sp,
                                        color = LissafiWarning
                                    )
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Vente : ${FormatUtils.formatFCFA(product.sellPrice)}",
                                color = LissafiGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (product.buyPrice > 0) {
                                Text(
                                    "Achat : ${FormatUtils.formatFCFA(product.buyPrice)}",
                                    fontSize = 11.sp,
                                    color = LissafiOrange
                                )
                            }
                        }
                    }
                }
            }

            // Limite gratuite
            if (!state.isPremium) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = LissafiOrange.copy(alpha = 0.15f))
                    ) {
                        Text(
                            "${state.products.size}/10 produits (Gratuit) ⬆ Passe Premium",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            color = LissafiOrange,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        )
                    }
                }
            }
        }
    }

    // Dialog ajout
    if (showAddDialog) {
        AddProductFullDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { product ->
                viewModel.addProduct(product)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddProductFullDialog(
    onDismiss: () -> Unit,
    onAdd: (Product) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var hasBarcode by remember { mutableStateOf(true) }
    var barcodeText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau produit") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom du produit *") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = sellPrice, onValueChange = { sellPrice = it.filter { c -> c.isDigit() } }, label = { Text("Prix de vente (FCFA) *") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = buyPrice, onValueChange = { buyPrice = it.filter { c -> c.isDigit() } }, label = { Text("Prix d'achat (FCFA)") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = stock, onValueChange = { stock = it.filter { c -> c.isDigit() } }, label = { Text("Quantité en stock") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Catégorie") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { hasBarcode = !hasBarcode }) {
                        Text(if (hasBarcode) "📱 Avec code-barres" else "📝 Sans code-barres")
                    }
                }
                if (hasBarcode) {
                    OutlinedTextField(value = barcodeText, onValueChange = { barcodeText = it }, label = { Text("Code-barres") }, singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val price = sellPrice.toIntOrNull() ?: 0
                if (name.isNotBlank() && price > 0) {
                    val barcode = if (hasBarcode && barcodeText.isNotBlank()) barcodeText.trim()
                        else "MANUAL-${System.currentTimeMillis()}"
                    val product = Product(
                        barcode = barcode,
                        name = name.trim(),
                        sellPrice = price,
                        buyPrice = buyPrice.toIntOrNull() ?: 0,
                        stock = stock.toIntOrNull() ?: 0,
                        category = category.trim(),
                        hasBarcode = hasBarcode && barcodeText.isNotBlank()
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
