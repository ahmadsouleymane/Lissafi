package com.lissafi.app.ui.screen

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.LissafiApp
import com.lissafi.app.data.entity.Client
import com.lissafi.app.data.entity.Product
import com.lissafi.app.data.sync.SyncStatus
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.service.ReceiptService
import com.lissafi.app.ui.components.AmountField
import com.lissafi.app.ui.components.AmountText
import com.lissafi.app.ui.components.EmptyState
import com.lissafi.app.ui.components.HelpHint
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.PrimaryActionButton
import com.lissafi.app.ui.components.QuantityStepper
import com.lissafi.app.ui.components.QuickAmountChips
import com.lissafi.app.ui.components.SearchField
import com.lissafi.app.ui.components.SectionHeader
import com.lissafi.app.ui.components.StatusBadge
import com.lissafi.app.ui.components.SyncIndicator
import com.lissafi.app.ui.theme.Danger
import com.lissafi.app.ui.theme.LissafiCream
import com.lissafi.app.ui.theme.LissafiGreen
import com.lissafi.app.ui.theme.LissafiOrange
import com.lissafi.app.ui.theme.LissafiWhite
import com.lissafi.app.ui.theme.Neutral200
import com.lissafi.app.ui.theme.Neutral400
import com.lissafi.app.ui.theme.Neutral500
import com.lissafi.app.ui.theme.Success
import com.lissafi.app.ui.theme.White
import com.lissafi.app.ui.viewmodel.CartItem
import com.lissafi.app.ui.viewmodel.CartViewModel
import com.lissafi.app.ui.viewmodel.LastSale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val QUICK_CASH = listOf(500, 1000, 2000, 5000)

@Composable
fun CaisseScreen(
    viewModel: CartViewModel,
    syncStatus: SyncStatus = SyncStatus.IDLE,
    onNavigateToProducts: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val lastSale by viewModel.lastSale.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showEncaisseDialog by remember { mutableStateOf(false) }
    var showScannerScreen by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showClientPicker by remember { mutableStateOf(false) }
    var showBluetoothPicker by remember { mutableStateOf(false) }
    var currentReceipt by remember { mutableStateOf<LastSale?>(null) }
    var amountText by remember { mutableStateOf("") }
    var creditError by remember { mutableStateOf(false) }
    var scanFeedback by remember { mutableStateOf<String?>(null) }
    var searchHint by remember { mutableStateOf("") }

    // Guide de premier lancement (affiché une seule fois)
    val prefs = remember { context.getSharedPreferences("lissafi_prefs", Context.MODE_PRIVATE) }
    var showHelp by remember {
        mutableStateOf(!prefs.getBoolean("caisse_help_seen", false))
    }

    val btLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { showBluetoothPicker = true }

    // Feedback de scan
    LaunchedEffect(scanResult) {
        scanResult?.let {
            scanFeedback = when {
                it.startsWith("OK:") -> "✓ ${it.removePrefix("OK:")} ajouté au panier"
                it.startsWith("NOT_FOUND:") -> "Produit non trouvé : ${it.removePrefix("NOT_FOUND:")}"
                else -> null
            }
            viewModel.clearScanResult()
            delay(2200)
            scanFeedback = null
        }
    }

    // Reçu après vente
    LaunchedEffect(lastSale) {
        lastSale?.let { sale ->
            currentReceipt = sale
            showReceiptDialog = true
        }
    }

    val paid = amountText.toIntOrNull() ?: 0
    val realTimeChange = if (paid >= state.total) paid - state.total else 0

    Box(modifier = Modifier.fillMaxSize().background(LissafiCream)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── EN-TÊTE ──
            LissafiHeader(
                title = "LISSAFI",
                subtitle = "Ta caisse",
                leadingIcon = Icons.Filled.Store,
                actions = {
                    SyncIndicator(
                        status = syncStatus,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Paramètres",
                            tint = LissafiWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            )

            // ── GUIDE DE PREMIÈRE UTILISATION ──
            AnimatedVisibility(
                visible = showHelp,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                FirstUseHelpCard(onDismiss = {
                    showHelp = false
                    prefs.edit().putBoolean("caisse_help_seen", true).apply()
                })
            }

            // ── AJOUTER UN PRODUIT ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scanner (caméra)
                Button(
                    onClick = { showScannerScreen = true },
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LissafiGreen)
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = "Scanner un code-barres",
                        modifier = Modifier.size(28.dp)
                    )
                }
                SearchField(
                    value = searchHint,
                    onValueChange = {
                        searchHint = it
                        if (it.isNotBlank()) showSearchDialog = true
                    },
                    placeholder = "Chercher un produit et le toucher…",
                    modifier = Modifier.weight(1f)
                )
            }

            // ── RETOUR SCAN ──
            AnimatedVisibility(scanFeedback != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (scanFeedback?.startsWith("✓") == true)
                                LissafiGreen.copy(alpha = 0.1f)
                            else
                                LissafiOrange.copy(alpha = 0.1f)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (scanFeedback?.startsWith("✓") == true)
                            Icons.Filled.CheckCircle else Icons.Filled.Error,
                        contentDescription = null,
                        tint = if (scanFeedback?.startsWith("✓") == true) LissafiGreen else LissafiOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = scanFeedback ?: "",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (scanFeedback?.startsWith("✓") == true) LissafiGreen else LissafiOrange
                    )
                }
            }

            // ── PRODUITS RÉCENTS (accès rapide) ──
            if (state.recentProducts.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    SectionHeader(
                        text = "PRODUITS VITESSE",
                        icon = Icons.Filled.History,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.recentProducts) { p ->
                            SpeedProductCard(p) { viewModel.scanProduct(p.barcode) }
                        }
                    }
                }
            }

            // ── TOTAL (toujours visible) ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL À PAYER",
                            color = Neutral500,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (state.items.isEmpty()) "Ton panier est vide" else
                                "${state.items.size} article${if (state.items.size > 1) "s" else ""}",
                            color = Neutral400,
                            fontSize = 12.sp
                        )
                    }
                    AnimatedContent(
                        targetState = state.total,
                        label = "total"
                    ) { total ->
                        AmountText(
                            amount = total,
                            fontSize = 32,
                            color = if (total > 0) LissafiGreen else Neutral400
                        )
                    }
                }
            }

            // ── PANIER ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                if (state.items.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.QrCodeScanner,
                        title = "Commence par ajouter un produit",
                        message = "Touche le scanner pour lire un code-barres,\nou tape le nom du produit dans la barre de recherche."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        itemsIndexed(state.items) { index, item ->
                            CartItemRow(
                                item = item,
                                onDecrease = {
                                    if (item.quantity > 1) viewModel.updateQuantity(index, item.quantity - 1)
                                    else viewModel.removeItem(index)
                                },
                                onIncrease = { viewModel.updateQuantity(index, item.quantity + 1) }
                            )
                        }
                    }
                }
            }

            // ── MODE DE PAIEMENT ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeButton(
                    icon = Icons.Filled.Payments,
                    label = "Comptant",
                    selected = !state.isCredit,
                    selectedColor = LissafiGreen,
                    onClick = {
                        viewModel.setCreditMode(false)
                        creditError = false
                    },
                    modifier = Modifier.weight(1f)
                )
                ModeButton(
                    icon = Icons.Filled.People,
                    label = "Crédit",
                    selected = state.isCredit,
                    selectedColor = LissafiOrange,
                    onClick = {
                        viewModel.setCreditMode(true)
                        if (state.selectedClient == null) showClientPicker = true
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Client sélectionné pour le crédit
            AnimatedVisibility(state.isCredit && state.selectedClient != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LissafiOrange.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.People,
                        contentDescription = null,
                        tint = LissafiOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Crédit pour : ${state.selectedClient?.name ?: ""}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = LissafiOrange,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { showClientPicker = true },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Changer", fontSize = 12.sp, color = LissafiOrange)
                    }
                }
            }
            AnimatedVisibility(creditError) {
                Text(
                    text = "Pour vendre à crédit, choisis d'abord un client.",
                    color = Danger,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )
            }

            // ── BOUTON ENCAISSER ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                PrimaryActionButton(
                    text = if (state.isCredit)
                        "ENREGISTRER LE CRÉDIT · ${FormatUtils.formatFCFA(state.total)}"
                    else
                        "ENCAISSER · ${FormatUtils.formatFCFA(state.total)}",
                    icon = if (state.isCredit) Icons.Filled.People else Icons.Filled.Payments,
                    onClick = {
                        when {
                            state.items.isEmpty() ->
                                Toast.makeText(context, "Ajoute au moins un article au panier", Toast.LENGTH_SHORT).show()
                            state.isCredit && state.selectedClient == null -> {
                                creditError = true
                                showClientPicker = true
                            }
                            state.isCredit -> scope.launch {
                                viewModel.encaisser(0)
                                viewModel.clearCart()
                            }
                            else -> {
                                amountText = ""
                                showEncaisseDialog = true
                            }
                        }
                    },
                    enabled = state.total > 0,
                    height = 62
                )
            }
        }
    }

    // ── DIALOGUES ──
    if (showClientPicker) {
        ClientPickerDialog(
            viewModel = viewModel,
            onDismiss = { showClientPicker = false },
            onClientSelected = {
                viewModel.selectClient(it)
                creditError = false
                showClientPicker = false
            }
        )
    }
    if (showEncaisseDialog) {
        EncaisseDialog(
            total = state.total,
            amountText = amountText,
            onAmountChange = { amountText = it },
            paid = paid,
            change = realTimeChange,
            onValidate = {
                scope.launch {
                    viewModel.encaisser(paid)
                    viewModel.clearCart()
                }
                amountText = ""
                showEncaisseDialog = false
            },
            onDismiss = {
                showEncaisseDialog = false
                amountText = ""
            }
        )
    }
    if (showScannerScreen) {
        BarcodeScannerScreen(
            onBarcodeScanned = {
                viewModel.scanProduct(it)
                showScannerScreen = false
            },
            onDismiss = { showScannerScreen = false }
        )
    }
    if (showSearchDialog) {
        ProductSearchDialog(
            viewModel = viewModel,
            initialQuery = searchHint,
            onDismiss = {
                showSearchDialog = false
                searchHint = ""
            }
        )
    }
    if (showReceiptDialog && currentReceipt != null) {
        ReceiptDialog(
            sale = currentReceipt!!,
            onImprimer = {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter != null && adapter.isEnabled) {
                    showBluetoothPicker = true
                } else {
                    btLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }
            },
            onWhatsApp = {
                val receiptText = buildReceiptText(currentReceipt!!, context)
                ReceiptService.shareViaWhatsApp(context, receiptText)
            },
            onPartager = {
                val receiptText = buildReceiptText(currentReceipt!!, context)
                ReceiptService.shareText(context, receiptText)
            },
            onFermer = {
                showReceiptDialog = false
                viewModel.clearLastSale()
            }
        )
    }
    if (showBluetoothPicker) {
        val printers = remember { ReceiptService.getPairedPrinters() }
        BluetoothPrinterDialog(
            printers = printers,
            receiptText = buildReceiptText(currentReceipt!!, context),
            onDismiss = { showBluetoothPicker = false }
        )
    }
}

// ============================================================
// GUIDE DE PREMIÈRE UTILISATION
// ============================================================
@Composable
private fun FirstUseHelpCard(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = LissafiGreen.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Comment vendre en 3 étapes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = LissafiGreen
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Fermer",
                        tint = Neutral500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HelpStep(1, "Ajoute un produit : scanne son code ou tape son nom")
            Spacer(Modifier.height(6.dp))
            HelpStep(2, "Vérifie le panier et choisis Comptant ou Crédit")
            Spacer(Modifier.height(6.dp))
            HelpStep(3, "Touche ENCAISSER — c'est tout !")
            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = LissafiGreen)
            ) {
                Text("J'ai compris ✓", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun HelpStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(LissafiGreen),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = Neutral500,
            lineHeight = 17.sp
        )
    }
}

// ============================================================
// LIGNE D'ARTICLE DU PANIER
// ============================================================
@Composable
private fun CartItemRow(
    item: CartItem,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${FormatUtils.formatFCFA(item.price)} / unité",
                fontSize = 12.sp,
                color = Neutral400
            )
        }
        Spacer(Modifier.width(8.dp))
        AmountText(
            amount = (item.price * item.quantity).toInt(),
            fontSize = 16
        )
        Spacer(Modifier.width(12.dp))
        QuantityStepper(
            quantity = item.quantity,
            onDecrease = onDecrease,
            onIncrease = onIncrease
        )
    }
}

// ============================================================
// CARTE PRODUIT VITESSE
// ============================================================
@Composable
private fun SpeedProductCard(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(112.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(LissafiGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    tint = LissafiGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = product.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AmountText(
                amount = product.sellPrice,
                fontSize = 13
            )
        }
    }
}

// ============================================================
// BOUTON MODE COMPTANT / CRÉDIT
// ============================================================
@Composable
private fun ModeButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        if (selected) selectedColor.copy(alpha = 0.12f) else White,
        label = "bg"
    )
    val border by animateColorAsState(
        if (selected) selectedColor else Neutral200,
        label = "border"
    )
    val tint by animateColorAsState(
        if (selected) selectedColor else Neutral500,
        label = "tint"
    )
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = if (selected) BorderStroke(2.dp, border) else BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                color = tint
            )
        }
    }
}

// ============================================================
// DIALOGUE ENCAISSEMENT
// ============================================================
@Composable
private fun EncaisseDialog(
    total: Int,
    amountText: String,
    onAmountChange: (String) -> Unit,
    paid: Int,
    change: Int,
    onValidate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        icon = {
            Icon(
                imageVector = Icons.Filled.Payments,
                contentDescription = null,
                tint = LissafiGreen,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Combien donne le client ?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Spacer(Modifier.height(4.dp))
                AmountText(
                    amount = total,
                    fontSize = 26
                )
            }
        },
        text = {
            Column {
                AmountField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    label = "Montant reçu"
                )
                Spacer(Modifier.height(12.dp))
                QuickAmountChips(
                    amounts = QUICK_CASH,
                    current = paid,
                    onSelect = { amt -> onAmountChange((paid + amt).toString()) }
                )
                AnimatedVisibility(visible = paid >= total) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = LissafiGreen.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Redeem,
                                contentDescription = null,
                                tint = LissafiGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "À rendre au client",
                                    fontSize = 12.sp,
                                    color = LissafiGreen,
                                    fontWeight = FontWeight.Medium
                                )
                                AmountText(
                                    amount = change,
                                    fontSize = 22
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            PrimaryActionButton(
                text = "VALIDER LA VENTE",
                onClick = onValidate,
                enabled = paid >= total && total > 0,
                height = 52
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = Neutral500)
            }
        }
    )
}

// ============================================================
// DIALOGUE RECHERCHE PRODUIT
// ============================================================
@Composable
private fun ProductSearchDialog(
    viewModel: CartViewModel,
    initialQuery: String = "",
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf(initialQuery) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val app = remember { context.applicationContext as LissafiApp }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        products = app.database.getAllProducts()
        isLoading = false
    }
    LaunchedEffect(query) {
        isLoading = true
        products = if (query.isBlank()) app.database.getAllProducts()
        else app.database.searchProducts(query)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = {
            Column {
                Text(
                    text = "Ajouter un produit",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(10.dp))
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Nom du produit…"
                )
            }
        },
        text = {
            when {
                isLoading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LissafiGreen, modifier = Modifier.size(26.dp))
                }
                products.isEmpty() -> Text(
                    text = "Aucun produit trouvé. Ajoute d'abord des produits dans le catalogue.",
                    fontSize = 13.sp,
                    color = Neutral500,
                    modifier = Modifier.padding(20.dp)
                )
                else -> LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(products.take(40)) { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(LissafiCream)
                                .clickable {
                                    viewModel.scanProduct(product.barcode)
                                    onDismiss()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(LissafiGreen.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Inventory2,
                                    contentDescription = null,
                                    tint = LissafiGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AmountText(amount = product.sellPrice, fontSize = 14)
                                    if (product.hasBarcode && product.stock <= product.minStock) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Stock : ${product.stock}",
                                            fontSize = 11.sp,
                                            color = if (product.stock == 0) Danger else LissafiOrange
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = Icons.Filled.AddCircle,
                                contentDescription = "Ajouter",
                                tint = LissafiGreen,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer", color = Neutral500) }
        },
        dismissButton = null
    )
}

// ============================================================
// DIALOGUE REÇU
// ============================================================
@Composable
private fun ReceiptDialog(
    sale: LastSale,
    onImprimer: () -> Unit,
    onWhatsApp: () -> Unit,
    onPartager: () -> Unit,
    onFermer: () -> Unit
) {
    val context = LocalContext.current
    val receiptText = remember { buildReceiptText(sale, context) }

    AlertDialog(
        onDismissRequest = onFermer,
        shape = RoundedCornerShape(22.dp),
        icon = {
            Icon(
                imageVector = Icons.Filled.Receipt,
                contentDescription = null,
                tint = LissafiGreen,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (sale.isCredit) "Crédit enregistré ✓" else "Vente enregistrée ✓",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                if (!sale.isCredit && sale.changeGiven > 0) {
                    HelpHint(
                        text = "Monnaie à rendre : ${FormatUtils.formatFCFA(sale.changeGiven)}"
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LissafiCream)
                ) {
                    Text(
                        text = receiptText,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onImprimer,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Print, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Imprimer", fontSize = 13.sp)
                    }
                    Button(
                        onClick = onWhatsApp,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Text("WhatsApp", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPartager,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Partager", fontSize = 13.sp)
                    }
                    TextButton(
                        onClick = onFermer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Terminer", fontSize = 14.sp, color = LissafiGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        dismissButton = null
    )
}

fun buildReceiptText(sale: LastSale, context: Context): String {
    return ReceiptService.formatReceipt(
        ReceiptService.ReceiptData(
            shopName = "LISSAFI",
            shopPhone = "",
            date = sale.date,
            items = sale.items.map { ReceiptService.ReceiptItem(it.name, it.quantity, it.price) },
            total = sale.total,
            amountPaid = sale.amountPaid,
            changeGiven = sale.changeGiven,
            isCredit = sale.isCredit
        )
    )
}

// ============================================================
// DIALOGUE IMPRIMANTES BLUETOOTH
// ============================================================
@Composable
private fun BluetoothPrinterDialog(
    printers: List<ReceiptService.PrinterDevice>,
    receiptText: String,
    onDismiss: () -> Unit
) {
    var printing by remember { mutableStateOf(false) }
    var resultMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!printing) onDismiss() },
        shape = RoundedCornerShape(22.dp),
        icon = {
            Icon(
                imageVector = Icons.Filled.Print,
                contentDescription = null,
                tint = LissafiGreen,
                modifier = Modifier.size(30.dp)
            )
        },
        title = {
            Text("Imprimer le reçu", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column {
                when {
                    printers.isEmpty() -> Text(
                        text = "Aucune imprimante Bluetooth appairée. Connecte une imprimante dans les réglages Bluetooth de ton téléphone.",
                        fontSize = 13.sp,
                        color = Neutral500,
                        lineHeight = 18.sp
                    )
                    printing -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = LissafiGreen, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Impression en cours…", fontSize = 14.sp)
                    }
                    else -> {
                        Text(
                            text = "Choisir une imprimante :",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        printers.forEach { printer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LissafiCream)
                                    .clickable {
                                        printing = true
                                        ReceiptService.printReceipt(printer.address, receiptText) { ok, msg ->
                                            resultMsg = msg
                                            printing = false
                                        }
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Print,
                                    contentDescription = null,
                                    tint = LissafiGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = printer.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
                resultMsg?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = it,
                        fontSize = 13.sp,
                        color = if (it.contains("succès")) Success else Danger,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            if (!printing) TextButton(onClick = onDismiss) { Text("Fermer", color = Neutral500) }
        },
        dismissButton = null
    )
}

// ============================================================
// DIALOGUE CHOIX CLIENT (CRÉDIT)
// ============================================================
@Composable
private fun ClientPickerDialog(
    viewModel: CartViewModel,
    onDismiss: () -> Unit,
    onClientSelected: (Client) -> Unit
) {
    var q by remember { mutableStateOf("") }
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showNewClientForm by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val app = remember { ctx.applicationContext as LissafiApp }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        clients = app.database.getAllClients()
        loading = false
    }
    LaunchedEffect(q) {
        loading = true
        clients = if (q.isBlank()) app.database.getAllClients() else app.database.searchClients(q)
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        icon = {
            Icon(
                imageVector = Icons.Filled.People,
                contentDescription = null,
                tint = LissafiGreen,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (showNewClientForm) "Nouveau client" else "Vendre à crédit : qui est le client ?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            if (showNewClientForm) {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nom du client *") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it.filter { c -> c.isDigit() || c == '+' } },
                        label = { Text("Téléphone (optionnel)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showNewClientForm = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Retour") }
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    val client = Client(
                                        id = java.util.UUID.randomUUID().toString(),
                                        name = newName.trim(),
                                        phone = newPhone.trim(),
                                        totalDebt = 0,
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    scope.launch { app.database.upsertClient(client) }
                                    onClientSelected(client)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LissafiGreen)
                        ) { Text("Ajouter", fontWeight = FontWeight.SemiBold) }
                    }
                }
            } else {
                Column {
                    OutlinedButton(
                        onClick = { showNewClientForm = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, LissafiGreen)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PersonAdd,
                            contentDescription = null,
                            tint = LissafiGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("+ Nouveau client", color = LissafiGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    SearchField(
                        value = q,
                        onValueChange = { q = it },
                        placeholder = "Rechercher un client…"
                    )
                    Spacer(Modifier.height(8.dp))
                    when {
                        loading -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = LissafiGreen, modifier = Modifier.size(22.dp))
                        }
                        clients.isEmpty() -> Text(
                            text = "Aucun client. Ajoute le premier !",
                            fontSize = 13.sp,
                            color = Neutral500,
                            modifier = Modifier.padding(16.dp)
                        )
                        else -> LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(clients.take(15)) { c ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(LissafiCream)
                                        .clickable { onClientSelected(c) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(LissafiGreen.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.People,
                                            contentDescription = null,
                                            tint = LissafiGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = c.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        if (c.phone.isNotBlank()) {
                                            Text(c.phone, fontSize = 12.sp, color = Neutral500)
                                        }
                                    }
                                    if (c.totalDebt > 0) {
                                        AmountText(amount = c.totalDebt, fontSize = 13, color = LissafiOrange)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer", color = Neutral500) }
        },
        dismissButton = null
    )
}
