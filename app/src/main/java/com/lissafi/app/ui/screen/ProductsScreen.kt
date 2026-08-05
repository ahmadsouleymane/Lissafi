package com.lissafi.app.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.data.entity.Product
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.components.AmountField
import com.lissafi.app.ui.components.AmountText
import com.lissafi.app.ui.components.ConfirmDialog
import com.lissafi.app.ui.components.EmptyState
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.components.PrimaryActionButton
import com.lissafi.app.ui.components.SearchField
import com.lissafi.app.ui.components.StatusBadge
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.Error
import com.lissafi.app.ui.theme.OnPrimary
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.Secondary
import com.lissafi.app.ui.theme.Success
import com.lissafi.app.ui.theme.SurfaceAlt
import com.lissafi.app.ui.theme.TextSecondary
import com.lissafi.app.ui.theme.Warning
import com.lissafi.app.ui.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

// ============================================================
// FILTRE LOCAL DE LA LISTE — ne touche pas au ViewModel
// ============================================================
private enum class ProductFilter(val label: String) {
    TOUS("Tous"),
    EN_STOCK("En stock"),
    ALERTE("Alerte stock")
}

private fun isStockAlert(p: Product): Boolean =
    p.stock == 0 || p.stock <= p.minStock

@Composable
fun ProductsScreen(viewModel: ProductViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var filter by remember { mutableStateOf(ProductFilter.TOUS) }

    val alertCount = state.products.count { isStockAlert(it) }
    val filtered = when (filter) {
        ProductFilter.TOUS -> state.products
        ProductFilter.EN_STOCK -> state.products.filter { it.stock > 0 }
        ProductFilter.ALERTE -> state.products.filter { isStockAlert(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        LissafiHeader(
            title = "Produits",
            subtitle = if (alertCount > 0)
                "${state.products.size} produits · $alertCount alertes stock"
            else
                "${state.products.size} produits",
            onBack = onBack
        )

        SearchField(
            value = state.searchQuery,
            onValueChange = { viewModel.search(it) },
            placeholder = "Rechercher un produit…",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProductFilter.values().forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = {
                        Text(
                            text = f.label,
                            fontSize = 12.sp,
                            fontWeight = if (filter == f) FontWeight.Medium else FontWeight.Normal
                        )
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

        if (filtered.isEmpty()) {
            EmptyState(
                icon = LissafiIcons.Produit,
                title = if (state.searchQuery.isBlank()) "Aucun produit" else "Aucun résultat",
                message = if (state.searchQuery.isBlank())
                    "Ajoute ton premier article !"
                else
                    "Aucun produit ne correspond à « ${state.searchQuery} ». Vérifie l'orthographe.",
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                actionLabel = if (state.searchQuery.isBlank()) "Ajouter un produit" else null,
                onAction = {
                    scope.launch {
                        if (viewModel.canAddProduct()) showAddDialog = true
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                items(filtered, key = { it.barcode }) { product ->
                    ProductCard(
                        product = product,
                        onEdit = { editingProduct = product },
                        onDelete = {
                            viewModel.deleteProduct(product)
                            Toast.makeText(context, "${product.name} supprimé", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                if (!state.isPremium) {
                    item {
                        PremiumLimitCard(
                            current = state.products.size,
                            limit = 10,
                            onClickUpgrade = {}
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    PrimaryActionButton(
                        text = "Ajouter un produit",
                        icon = LissafiIcons.Ajouter,
                        onClick = {
                            scope.launch {
                                if (viewModel.canAddProduct()) showAddDialog = true
                                else Toast.makeText(
                                    context,
                                    "10 produits max en version gratuite. Passe Premium !",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showAddDialog) {
        ProductFormDialog(
            title = "Nouveau produit",
            subtitle = "Renseigne le nom et le prix de vente. C'est tout ce qu'il faut pour vendre.",
            onDismiss = { showAddDialog = false },
            onSave = {
                viewModel.addProduct(it)
                showAddDialog = false
            }
        )
    }

    editingProduct?.let { product ->
        ProductFormDialog(
            title = "Modifier « ${product.name} »",
            subtitle = "Corrige les informations du produit.",
            initialProduct = product,
            onDismiss = { editingProduct = null },
            onSave = {
                viewModel.addProduct(it)
                editingProduct = null
            }
        )
    }
}

// ============================================================
// CARTE PRODUIT
// ============================================================
@Composable
private fun ProductCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val stockColor = when {
        !product.hasBarcode -> TextSecondary
        product.stock == 0 -> Error
        product.stock <= product.minStock -> Warning
        else -> Success
    }
    val stockLabel = when {
        !product.hasBarcode -> "Sans code-barres"
        product.stock == 0 -> "Rupture"
        product.stock <= product.minStock -> "Stock bas"
        else -> "En stock · ${product.stock}"
    }

    LissafiCard(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LissafiIcons.Produit,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                StatusBadge(text = stockLabel, color = stockColor)
                if (product.category.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = product.category,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                AmountText(amount = product.sellPrice, fontSize = 18)
                if (product.buyPrice > 0) {
                    Text(
                        text = "Achat ${FormatUtils.formatFCFA(product.buyPrice)}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = LissafiIcons.Modifier,
                            contentDescription = "Modifier",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = LissafiIcons.Supprimer,
                            contentDescription = "Supprimer",
                            tint = Error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Supprimer ce produit ?",
            message = "« ${product.name} » sera retiré du catalogue. Cette action ne peut pas être annulée.",
            confirmLabel = "Supprimer",
            onConfirm = {
                onDelete()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false },
            icon = LissafiIcons.Supprimer,
            destructive = true,
            iconTint = Error
        )
    }
}

// ============================================================
// CARTE LIMITE PREMIUM
// ============================================================
@Composable
private fun PremiumLimitCard(
    current: Int,
    limit: Int,
    onClickUpgrade: () -> Unit
) {
    LissafiCard(
        modifier = Modifier.padding(vertical = 8.dp),
        borderColor = Secondary.copy(alpha = 0.3f)
    ) {
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
                    .background(Secondary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LissafiIcons.Alerte,
                    contentDescription = null,
                    tint = Secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$current / $limit produits en gratuit",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Secondary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Passe Premium pour des produits illimités.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onClickUpgrade,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Secondary)
            ) {
                Text("Premium", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============================================================
// FORMULAIRE PRODUIT
// ============================================================
@Composable
fun ProductFormDialog(
    title: String,
    subtitle: String? = null,
    initialProduct: Product? = null,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var sellPrice by remember { mutableStateOf(initialProduct?.sellPrice?.toString() ?: "") }
    var buyPrice by remember { mutableStateOf(initialProduct?.buyPrice?.toString() ?: "") }
    var stock by remember { mutableStateOf(initialProduct?.stock?.toString() ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "") }
    var hasBarcode by remember { mutableStateOf(initialProduct?.hasBarcode ?: true) }
    var barcodeText by remember {
        mutableStateOf(if (initialProduct?.hasBarcode == true) initialProduct.barcode else "")
    }
    var showPriceWarning by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        icon = {
            Icon(
                imageVector = LissafiIcons.Produit,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(text = subtitle, fontSize = 12.sp, color = TextSecondary)
                }
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du produit *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                AmountField(
                    value = sellPrice,
                    onValueChange = {
                        sellPrice = it
                        showPriceWarning = false
                    },
                    label = "Prix de vente *"
                )
                Spacer(Modifier.height(10.dp))
                AmountField(
                    value = buyPrice,
                    onValueChange = {
                        buyPrice = it
                        showPriceWarning = false
                    },
                    label = "Prix d'achat (optionnel)"
                )
                AnimatedVisibility(showPriceWarning) {
                    Text(
                        text = "Le prix d'achat est plus élevé que le prix de vente — tu vendras à perte.",
                        color = Error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it.filter { c -> c.isDigit() } },
                        label = { Text("Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Catégorie") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { hasBarcode = !hasBarcode }) {
                    Icon(
                        imageVector = if (hasBarcode) LissafiIcons.Scanner else LissafiIcons.Produit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (hasBarcode) "Avec code-barres" else "Sans code-barres",
                        fontSize = 13.sp
                    )
                }
                if (hasBarcode) {
                    OutlinedTextField(
                        value = barcodeText,
                        onValueChange = { barcodeText = it },
                        label = { Text("Code-barres") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            PrimaryActionButton(
                text = "ENREGISTRER",
                onClick = {
                    val sPrice = sellPrice.toIntOrNull() ?: 0
                    val bPrice = buyPrice.toIntOrNull() ?: 0
                    if (bPrice > 0 && bPrice >= sPrice) {
                        showPriceWarning = true
                        return@PrimaryActionButton
                    }
                    if (name.isNotBlank() && sPrice > 0) {
                        val barcode = when {
                            initialProduct != null -> initialProduct.barcode
                            hasBarcode && barcodeText.isNotBlank() -> barcodeText.trim()
                            else -> "MANUAL-${System.currentTimeMillis()}"
                        }
                        onSave(
                            Product(
                                barcode = barcode,
                                name = name.trim(),
                                sellPrice = sPrice,
                                buyPrice = bPrice,
                                stock = stock.toIntOrNull() ?: (initialProduct?.stock ?: 0),
                                category = category.trim(),
                                hasBarcode = hasBarcode && barcodeText.isNotBlank(),
                                createdAt = initialProduct?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = TextSecondary) }
        }
    )
}
