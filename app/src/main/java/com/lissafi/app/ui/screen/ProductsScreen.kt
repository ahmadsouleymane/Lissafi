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
import com.lissafi.app.data.remote.ProductLookupService
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.components.AmountField
import com.lissafi.app.ui.components.AmountText
import com.lissafi.app.ui.components.BarcodeView
import com.lissafi.app.ui.components.ConfirmDialog
import com.lissafi.app.ui.components.EmptyState
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.components.PremiumLimitDialog
import com.lissafi.app.ui.components.PrimaryActionButton
import com.lissafi.app.ui.components.SearchField
import com.lissafi.app.ui.components.StatusBadge
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.Error
import com.lissafi.app.ui.theme.OnPrimary
import com.lissafi.app.ui.theme.Primary
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
fun ProductsScreen(viewModel: ProductViewModel, onBack: () -> Unit, onNavigateToUpgrade: () -> Unit = {}) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPremiumDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var newProductPrefill by remember { mutableStateOf<Product?>(null) }
    var addChoiceOpen by remember { mutableStateOf(false) }
    var addScannerOpen by remember { mutableStateOf(false) }
    var addLookupBusy by remember { mutableStateOf(false) }
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
            subtitle = null,
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
                        if (viewModel.canAddProduct()) addChoiceOpen = true
                        else showPremiumDialog = true
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
                item {
                    Spacer(Modifier.height(8.dp))
                    PrimaryActionButton(
                        text = "Ajouter un produit",
                        icon = LissafiIcons.Ajouter,
                        onClick = {
                            scope.launch {
                                if (viewModel.canAddProduct()) addChoiceOpen = true
                                else showPremiumDialog = true
                            }
                        }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    // Choix du mode d'ajout : avec ou sans code-barres
    if (addChoiceOpen) {
        AddProductChoiceDialog(
            onWithoutBarcode = {
                addChoiceOpen = false
                newProductPrefill = Product(barcode = "", name = "", hasBarcode = false)
            },
            onWithBarcode = {
                addChoiceOpen = false
                addScannerOpen = true
            },
            onDismiss = { addChoiceOpen = false }
        )
    }

    // Ajout AVEC code-barres : on scanne, puis on tente de récupérer les infos via l'API.
    if (addScannerOpen) {
        BarcodeScannerScreen(
            onBarcodeScanned = { code ->
                addScannerOpen = false
                scope.launch {
                    addLookupBusy = true
                    val info = ProductLookupService.lookup(code)
                    addLookupBusy = false
                    newProductPrefill = Product(
                        barcode = code,
                        name = info?.name ?: "",
                        category = info?.category ?: "",
                        hasBarcode = true
                    )
                    Toast.makeText(
                        context,
                        if (info != null) "Produit trouvé : ${info.name}" else "Produit non trouvé — saisis les infos.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onDismiss = { addScannerOpen = false }
        )
    }

    if (showPremiumDialog) {
        // Limite atteinte → pop-up premium (remplace le Toast)
        PremiumLimitDialog(
            message = "Passe à Lissafi Premium pour ajouter autant de produits que tu veux. Sans limite.",
            onUpgrade = {
                showPremiumDialog = false
                onNavigateToUpgrade()
            },
            onDismiss = { showPremiumDialog = false }
        )
    }

    // Nouveau produit (pré-rempli ou non) — l'utilisateur complète avant d'enregistrer.
    newProductPrefill?.let { prefill ->
        ProductFormDialog(
            title = "Nouveau produit",
            subtitle = "Complète les informations puis enregistre.",
            isNew = true,
            initialProduct = prefill,
            onDismiss = { newProductPrefill = null },
            onSave = {
                viewModel.addProduct(it)
                newProductPrefill = null
            }
        )
    }

    editingProduct?.let { product ->
        ProductFormDialog(
            title = "Modifier « ${product.name} »",
            subtitle = "Corrige les informations du produit.",
            isNew = false,
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
// FORMULAIRE PRODUIT
// ============================================================
@Composable
fun ProductFormDialog(
    title: String,
    subtitle: String? = null,
    isNew: Boolean = true,
    initialProduct: Product? = null,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    // Pour un NOUVEAU produit, prix/stock démarrent vides (le pré-remplissage du
    // scan ne concerne que le nom/catégorie). En édition, on affiche les valeurs.
    var sellPrice by remember { mutableStateOf(if (isNew) "" else initialProduct?.sellPrice?.toString() ?: "") }
    var buyPrice by remember { mutableStateOf(if (isNew) "" else initialProduct?.buyPrice?.toString() ?: "") }
    var stock by remember { mutableStateOf(if (isNew) "" else initialProduct?.stock?.toString() ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "") }
    var hasBarcode by remember { mutableStateOf(initialProduct?.hasBarcode ?: true) }
    var barcodeText by remember {
        mutableStateOf(if (initialProduct?.hasBarcode == true) initialProduct.barcode else "")
    }
    var showPriceWarning by remember { mutableStateOf(false) }
    var showNameError by remember { mutableStateOf(false) }
    var showSellPriceError by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var lookupBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Scanner intégré au formulaire : scanne puis tente de pré-remplir nom/catégorie.
    if (showScanner) {
        BarcodeScannerScreen(
            onBarcodeScanned = { code ->
                showScanner = false
                barcodeText = code
                scope.launch {
                    lookupBusy = true
                    val info = ProductLookupService.lookup(code)
                    lookupBusy = false
                    if (info != null) {
                        if (name.isBlank()) name = info.name
                        if (category.isBlank()) category = info.category
                    }
                }
            },
            onDismiss = { showScanner = false }
        )
        return
    }

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
            // imePadding : le clavier ne cache plus les champs du formulaire.
            Column(
                modifier = Modifier.imePadding()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; showNameError = false },
                    label = { Text("Nom du produit *") },
                    isError = showNameError,
                    supportingText = if (showNameError) {{ Text("Le nom est obligatoire.", color = Error, fontSize = 12.sp) }} else null,
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
                        showSellPriceError = false
                    },
                    label = "Prix de vente *"
                )
                if (showSellPriceError) {
                    Text(
                        text = "Le prix de vente est obligatoire.",
                        color = Error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
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
                        trailingIcon = {
                            IconButton(onClick = { showScanner = true }) {
                                Icon(
                                    imageVector = LissafiIcons.Scanner,
                                    contentDescription = "Scanner un code-barres",
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        supportingText = if (lookupBusy) {
                            { Text("Recherche des informations du produit…", fontSize = 12.sp, color = TextSecondary) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Affiche un vrai code-barres RECTANGULAIRE (et non carré) pour le produit.
                    if (barcodeText.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        BarcodeView(
                            barcode = barcodeText.trim(),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        },
        confirmButton = {
            PrimaryActionButton(
                text = "ENREGISTRER",
                onClick = {
                    val sPrice = sellPrice.toIntOrNull() ?: 0
                    val bPrice = buyPrice.toIntOrNull() ?: 0
                    // Validation
                    var valid = true
                    if (bPrice > 0 && bPrice >= sPrice) {
                        showPriceWarning = true
                        valid = false
                    }
                    if (name.isBlank()) {
                        showNameError = true
                        valid = false
                    }
                    if (sPrice <= 0) {
                        showSellPriceError = true
                        valid = false
                    }
                    if (!valid) return@PrimaryActionButton

                    val barcode = when {
                        // Édition : on garde le code-barres d'origine (clé du produit).
                        !isNew && initialProduct != null -> initialProduct.barcode
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
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = TextSecondary) }
        }
    )
}

// ============================================================
// CHOIX DU MODE D'AJOUT — avec ou sans code-barres
// ============================================================
@Composable
private fun AddProductChoiceDialog(
    onWithoutBarcode: () -> Unit,
    onWithBarcode: () -> Unit,
    onDismiss: () -> Unit
) {
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
            Text("Comment ajouter ce produit ?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column {
                Text(
                    text = "Choisis la façon d'enregistrer ton article.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(16.dp))

                // Option 1 : sans code-barres
                Surface(
                    onClick = onWithoutBarcode,
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceAlt,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = LissafiIcons.Produit,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Sans code-barres", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Saisis le nom et le prix à la main.", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Option 2 : avec code-barres → scan + recherche automatique
                Surface(
                    onClick = onWithBarcode,
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceAlt,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = LissafiIcons.Scanner,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Avec code-barres", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Scanne, on tente de retrouver le produit automatiquement.", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = TextSecondary) }
        }
    )
}
