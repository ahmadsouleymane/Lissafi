package com.lissafi.app.ui.screen

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.LissafiApp
import com.lissafi.app.R
import com.lissafi.app.data.entity.Client
import com.lissafi.app.data.entity.Product
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.service.ReceiptService
import com.lissafi.app.ui.components.AmountField
import com.lissafi.app.ui.components.AmountText
import com.lissafi.app.ui.components.CapsuleTextField
import com.lissafi.app.ui.components.EmptyState
import com.lissafi.app.ui.components.HelpHint
import com.lissafi.app.ui.components.IconCircle
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.components.PrimaryActionButton
import com.lissafi.app.ui.components.QuantityStepper
import com.lissafi.app.ui.components.QuickAmountChips
import com.lissafi.app.ui.components.SearchField
import com.lissafi.app.ui.components.SecondaryActionButton
import com.lissafi.app.ui.components.SectionHeader
import com.lissafi.app.ui.components.SegmentedControl
import com.lissafi.app.ui.components.SuccessPulse
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.Border
import com.lissafi.app.ui.theme.Error
import com.lissafi.app.ui.theme.OnBackground
import com.lissafi.app.ui.theme.OnPrimary
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.PrimaryContainer
import com.lissafi.app.ui.theme.Secondary
import com.lissafi.app.ui.theme.Surface
import com.lissafi.app.ui.theme.SurfaceAlt
import com.lissafi.app.ui.theme.TextSecondary
import com.lissafi.app.ui.theme.TextTertiary
import com.lissafi.app.ui.viewmodel.CartItem
import com.lissafi.app.ui.viewmodel.CartViewModel
import com.lissafi.app.ui.viewmodel.LastSale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val QUICK_CASH = listOf(100, 500, 1000, 5000, 10000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaisseScreen(
    viewModel: CartViewModel,
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
    val haptic = LocalHapticFeedback.current

    var shopName by remember { mutableStateOf("LISSAFI") }
    var shopPhone by remember { mutableStateOf("") }
    var receiptFooterMessage by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val app = context.applicationContext as LissafiApp
        shopName = app.database.getSetting("shop_name")?.ifBlank { null } ?: "LISSAFI"
        shopPhone = app.database.getSetting("shop_phone") ?: ""
        receiptFooterMessage = app.database.getSetting("receipt_footer_message") ?: ""
    }

    var showEncaisseSheet by remember { mutableStateOf(false) }
    var showScannerScreen by remember { mutableStateOf(false) }
    var showReceiptSheet by remember { mutableStateOf(false) }
    var showClientPicker by remember { mutableStateOf(false) }
    var showBluetoothPicker by remember { mutableStateOf(false) }
    var currentReceipt by remember { mutableStateOf<LastSale?>(null) }
    var amountText by remember { mutableStateOf("") }
    var creditError by remember { mutableStateOf(false) }
    var scanFeedback by remember { mutableStateOf<String?>(null) }
    var scanIsSuccess by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchResults by remember { mutableStateOf(false) }

    val btLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { showBluetoothPicker = true }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showBluetoothPicker = true
        else Toast.makeText(context, "Active le Bluetooth dans les réglages pour imprimer.", Toast.LENGTH_LONG).show()
    }

    val todayString = remember {
        val sdf = java.text.SimpleDateFormat("EE d MMM", java.util.Locale.FRENCH)
        sdf.format(java.util.Date())
    }

    val prefs = remember { context.getSharedPreferences("lissafi_prefs", Context.MODE_PRIVATE) }
    var showHelp by remember {
        mutableStateOf(!prefs.getBoolean("caisse_help_seen", false))
    }

    LaunchedEffect(scanResult) {
        scanResult?.let {
            scanIsSuccess = it.startsWith("OK:")
            if (scanIsSuccess) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scanFeedback = when {
                it.startsWith("OK:") -> "${it.removePrefix("OK:")} ajouté"
                it.startsWith("NOT_FOUND:") -> "Code-barres inconnu"
                else -> null
            }
            viewModel.clearScanResult()
            delay(2200)
            scanFeedback = null
        }
    }

    LaunchedEffect(lastSale) {
        lastSale?.let { sale ->
            currentReceipt = sale
            showReceiptSheet = true
        }
    }

    val paid = amountText.toIntOrNull() ?: 0
    val realTimeChange = if (paid >= state.total) paid - state.total else 0
    val hasItems = state.items.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── EN-TÊTE — logo + date + settings ──
            LissafiHeader(
                title = "",
                subtitle = todayString,
                titleLogo = {
                    Image(
                        painter = painterResource(id = R.drawable.logo_header),
                        contentDescription = "Lissafi",
                        modifier = Modifier.height(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                },
                titleFontSize = 18.sp,
                titleFontWeight = FontWeight.Bold,
                actions = {
                    val todayTotal by viewModel.todayTotal.collectAsState()
                    if (todayTotal > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Primary.copy(alpha = 0.08f))
                                .clickable(onClick = onNavigateToReports)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Aujourd'hui · ${FormatUtils.formatFCFA(todayTotal)}",
                                color = Primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = LissafiIcons.Reglages,
                            contentDescription = "Réglages",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            // ── AIDE PREMIÈRE UTILISATION ──
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

            // ── RECHERCHE + SCANNER — ligne unifiée ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Champ de recherche
                SearchField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        showSearchResults = it.isNotBlank()
                    },
                    placeholder = "Rechercher un produit…",
                    modifier = Modifier.weight(1f)
                )
                // Bouton scanner
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Primary.copy(alpha = 0.08f))
                        .clickable { showScannerScreen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = LissafiIcons.Scanner,
                        contentDescription = "Scanner",
                        tint = Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // ── RÉSULTATS DE RECHERCHE ──
            if (showSearchResults) {
                ProductSearchDropdown(
                    query = searchQuery,
                    onSelect = { product ->
                        viewModel.scanProduct(product.barcode)
                        searchQuery = ""
                        showSearchResults = false
                    },
                    onDismiss = {
                        showSearchResults = false
                        searchQuery = ""
                    }
                )
            }

            // ── FEEDBACK SCAN ──
            AnimatedVisibility(scanFeedback != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (scanIsSuccess) Primary.copy(alpha = 0.08f)
                            else Secondary.copy(alpha = 0.08f)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (scanIsSuccess) LissafiIcons.Succes else LissafiIcons.Alerte,
                        contentDescription = null,
                        tint = if (scanIsSuccess) Primary else Secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = scanFeedback ?: "",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (scanIsSuccess) Primary else Secondary
                    )
                }
            }

            // ── PRODUITS RÉCENTS — pills horizontales ──
            if (state.recentProducts.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    SectionHeader(
                        text = "Récents",
                        icon = LissafiIcons.Recents,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.recentProducts) { p ->
                            SpeedProductPill(p) { viewModel.scanProduct(p.barcode) }
                        }
                    }
                }
            }

            // ── TOTAL — carte flottante avec montant animé ──
            LissafiCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                cornerRadius = 20,
                elevation = 4
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total à encaisser",
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (hasItems) "${state.items.size} article${if (state.items.size > 1) "s" else ""}"
                            else "Panier vide",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                    AnimatedContent(targetState = state.total, label = "total") { total ->
                        Text(
                            text = FormatUtils.formatFCFA(total),
                            color = if (total > 0) Primary else TextTertiary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // ── PANIER ──
            LissafiCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                cornerRadius = 20,
                elevation = 2
            ) {
                if (!hasItems) {
                    // État vide centré
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(40.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(Primary.copy(alpha = 0.06f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = LissafiIcons.Scanner,
                                    contentDescription = null,
                                    tint = Primary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Panier vide",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = OnBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Scannez un code-barres\npour commencer",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(
                            items = state.items,
                            key = { _, item -> item.barcode }
                        ) { index, item ->
                            CartItemRow(
                                item = item,
                                onDecrease = {
                                    if (item.quantity > 1) viewModel.updateQuantity(index, item.quantity - 1)
                                    else viewModel.removeItem(index)
                                },
                                onIncrease = { viewModel.updateQuantity(index, item.quantity + 1) },
                                modifier = Modifier.animateItem()
                            )
                            if (index < state.items.size - 1) {
                                HorizontalDivider(
                                    color = Border,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── MODE DE PAIEMENT — comptant / crédit ──
            SegmentedControl(
                options = listOf("Comptant", "Crédit"),
                selectedIndex = if (state.isCredit) 1 else 0,
                onSelect = { index ->
                    viewModel.setCreditMode(index == 1)
                    creditError = false
                    if (index == 1 && state.selectedClient == null) showClientPicker = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Client sélectionné pour le crédit
            AnimatedVisibility(state.isCredit && state.selectedClient != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Secondary.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = LissafiIcons.Client,
                        contentDescription = null,
                        tint = Secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Crédit · ${state.selectedClient?.name ?: ""}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { showClientPicker = true },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Changer", fontSize = 12.sp, color = Secondary)
                    }
                }
            }
            AnimatedVisibility(creditError) {
                Text(
                    text = "Choisis un client pour le crédit",
                    color = Error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )
            }

            // ── BOUTON ENCAISSER — géant, vert, flottant ──
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                PrimaryActionButton(
                    text = if (state.isCredit)
                        "Enregistrer · ${FormatUtils.formatFCFA(state.total)}"
                    else
                        "Encaisser · ${FormatUtils.formatFCFA(state.total)}",
                    icon = if (state.isCredit) LissafiIcons.Credit else LissafiIcons.Encaisser,
                    onClick = {
                        when {
                            state.items.isEmpty() ->
                                Toast.makeText(context, "Ajoute au moins un article au panier", Toast.LENGTH_SHORT).show()
                            state.isCredit && state.selectedClient == null -> {
                                creditError = true
                                showClientPicker = true
                            }
                            state.isCredit -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    viewModel.encaisser(0)
                                    viewModel.clearCart()
                                }
                            }
                            else -> {
                                amountText = ""
                                showEncaisseSheet = true
                            }
                        }
                    },
                    enabled = state.total > 0,
                    height = 58
                )
            }
        }
    }

    // ── BOTTOM SHEETS ──
    if (showClientPicker) {
        ClientPickerSheet(
            viewModel = viewModel,
            onDismiss = { showClientPicker = false },
            onClientSelected = {
                viewModel.selectClient(it)
                creditError = false
                showClientPicker = false
            }
        )
    }
    if (showEncaisseSheet) {
        EncaisseSheet(
            total = state.total,
            amountText = amountText,
            onAmountChange = { amountText = it },
            paid = paid,
            change = realTimeChange,
            onValidate = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    viewModel.encaisser(paid)
                    viewModel.clearCart()
                }
                amountText = ""
                showEncaisseSheet = false
            },
            onDismiss = {
                showEncaisseSheet = false
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
    if (showReceiptSheet && currentReceipt != null) {
        ReceiptSheet(
            sale = currentReceipt!!,
            shopName = shopName,
            shopPhone = shopPhone,
            footerMessage = receiptFooterMessage,
            onImprimer = {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter != null && adapter.isEnabled) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
                            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            showBluetoothPicker = true
                        } else {
                            btPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
                        }
                    } else {
                        showBluetoothPicker = true
                    }
                } else if (adapter != null) {
                    btLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                } else {
                    Toast.makeText(context, "Bluetooth non disponible sur cet appareil.", Toast.LENGTH_SHORT).show()
                }
            },
            onWhatsApp = {
                val receiptText = buildReceiptText(currentReceipt!!, context, shopName, shopPhone, receiptFooterMessage)
                ReceiptService.shareViaWhatsApp(context, receiptText)
                (context.applicationContext as LissafiApp).supabaseApi.logEvent(
                    eventType = "receipt",
                    message = "Reçu partagé via WhatsApp",
                    meta = "{\"total\":${currentReceipt!!.total}}"
                )
            },
            onPartager = {
                val receiptText = buildReceiptText(currentReceipt!!, context, shopName, shopPhone, receiptFooterMessage)
                ReceiptService.shareText(context, receiptText)
                (context.applicationContext as LissafiApp).supabaseApi.logEvent(
                    eventType = "receipt",
                    message = "Reçu partagé (texte)",
                    meta = "{\"total\":${currentReceipt!!.total}}"
                )
            },
            onFermer = {
                showReceiptSheet = false
                viewModel.clearLastSale()
            }
        )
    }
    if (showBluetoothPicker && currentReceipt != null) {
        val printers = remember { ReceiptService.getPairedPrinters() }
        BluetoothPrinterSheet(
            printers = printers,
            receiptText = buildReceiptText(currentReceipt!!, context, shopName, shopPhone, receiptFooterMessage),
            onDismiss = { showBluetoothPicker = false }
        )
    }
}

// ============================================================
// GUIDE DE PREMIÈRE UTILISATION
// ============================================================
@Composable
private fun FirstUseHelpCard(onDismiss: () -> Unit) {
    LissafiCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        cornerRadius = 20
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Vendre en 3 étapes",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = OnBackground
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = LissafiIcons.Fermer,
                        contentDescription = "Fermer",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HelpStep(1, "Scanne un code-barres ou cherche un produit")
            Spacer(Modifier.height(8.dp))
            HelpStep(2, "Vérifie le panier, choisis Comptant ou Crédit")
            Spacer(Modifier.height(8.dp))
            HelpStep(3, "Touche Encaisser — c'est tout !")
            Spacer(Modifier.height(14.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = Primary)
            ) {
                Text("J'ai compris", fontWeight = FontWeight.Medium, fontSize = 14.sp)
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
                .background(Primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = OnPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 17.sp
        )
    }
}

// ============================================================
// DROPDOWN RECHERCHE PRODUIT
// ============================================================
@Composable
private fun ProductSearchDropdown(
    query: String,
    onSelect: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val app = remember { context.applicationContext as LissafiApp }
    val userId = app.authManager.currentUserId() ?: ""

    LaunchedEffect(query) {
        isLoading = true
        products = if (query.isBlank()) app.database.getAllProducts(userId)
        else app.database.searchProducts(query, userId)
        isLoading = false
    }

    LissafiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        cornerRadius = 16,
        elevation = 8
    ) {
        when {
            isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            products.isEmpty() -> Text(
                text = "Aucun produit trouvé.",
                fontSize = 13.sp,
                color = TextTertiary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
            else -> {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(products.take(40)) { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(product) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconCircle(
                                icon = LissafiIcons.Produit,
                                size = 36,
                                iconSize = 18
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                AmountText(amount = product.sellPrice, fontSize = 13)
                            }
                            Icon(
                                imageVector = LissafiIcons.Ajouter,
                                contentDescription = "Ajouter",
                                tint = Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        if (products.indexOf(product) < products.size - 1) {
                            HorizontalDivider(
                                color = Border,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// LIGNE D'ARTICLE DU PANIER
// ============================================================
@Composable
private fun CartItemRow(
    item: CartItem,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = OnBackground
            )
            Text(
                text = "${FormatUtils.formatFCFA(item.price)} / unité",
                fontSize = 12.sp,
                color = TextTertiary
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = FormatUtils.formatFCFA((item.price * item.quantity).toInt()),
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = OnBackground
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
// PILL PRODUIT RÉCENT
// ============================================================
@Composable
private fun SpeedProductPill(product: Product, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Surface, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = LissafiIcons.Produit,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = product.name,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 1,
            color = OnBackground
        )
    }
}

// ============================================================
// BOTTOM SHEET ENCAISSEMENT
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EncaisseSheet(
    total: Int,
    amountText: String,
    onAmountChange: (String) -> Unit,
    paid: Int,
    change: Int,
    onValidate: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .imePadding()
                .padding(bottom = 32.dp)
        ) {
            // Poignée visuelle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Border)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconCircle(
                    icon = LissafiIcons.Encaisser,
                    size = 48,
                    iconSize = 24
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Encaisser",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = OnBackground
                    )
                    Text(
                        text = "Total : ${FormatUtils.formatFCFA(total)}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            AmountField(
                value = amountText,
                onValueChange = onAmountChange,
                label = "Montant reçu"
            )
            Spacer(Modifier.height(12.dp))
            QuickAmountChips(
                amounts = QUICK_CASH,
                current = paid,
                onSelect = { amt -> onAmountChange(amt.toString()) }
            )
            AnimatedVisibility(visible = paid >= total) {
                LissafiCard(
                    modifier = Modifier.padding(top = 12.dp),
                    cornerRadius = 16,
                    containerColor = PrimaryContainer,
                    elevation = 0
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = LissafiIcons.Encaisser,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "À rendre au client",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            AmountText(amount = change, fontSize = 24, color = Primary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            PrimaryActionButton(
                text = "Valider la vente",
                icon = LissafiIcons.Valider,
                onClick = onValidate,
                enabled = paid >= total && total > 0
            )
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Annuler", color = TextSecondary)
            }
        }
    }
}

// ============================================================
// BOTTOM SHEET REÇU
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptSheet(
    sale: LastSale,
    shopName: String = "LISSAFI",
    shopPhone: String = "",
    footerMessage: String = "",
    onImprimer: () -> Unit,
    onWhatsApp: () -> Unit,
    onPartager: () -> Unit,
    onFermer: () -> Unit
) {
    val context = LocalContext.current
    val receiptText = remember(sale, shopName, shopPhone, footerMessage) {
        buildReceiptText(sale, context, shopName, shopPhone, footerMessage)
    }

    ModalBottomSheet(
        onDismissRequest = onFermer,
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Border)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                SuccessPulse(size = 48, iconSize = 24)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (sale.isCredit) "Crédit enregistré" else "Vente enregistrée",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = OnBackground
                    )
                }
                IconButton(onClick = onFermer, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = LissafiIcons.Fermer,
                        contentDescription = "Fermer",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (!sale.isCredit && sale.changeGiven > 0) {
                Spacer(Modifier.height(12.dp))
                HelpHint(text = "Monnaie à rendre : ${FormatUtils.formatFCFA(sale.changeGiven)}", tint = Primary)
            }
            Spacer(Modifier.height(12.dp))
            LissafiCard(cornerRadius = 16, containerColor = SurfaceAlt, elevation = 0) {
                Text(
                    text = receiptText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 15.sp,
                    color = OnBackground,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onImprimer,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(
                        imageVector = LissafiIcons.Imprimer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Imprimer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                SecondaryActionButton(
                    text = "WhatsApp",
                    icon = LissafiIcons.Partager,
                    onClick = onWhatsApp,
                    modifier = Modifier.weight(1f),
                    height = 52
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryActionButton(
                    text = "Partager",
                    icon = LissafiIcons.Partager,
                    onClick = onPartager,
                    modifier = Modifier.weight(1f),
                    height = 52
                )
                TextButton(
                    onClick = onFermer,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text("Terminer", fontSize = 14.sp, color = Primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ============================================================
// BOTTOM SHEET IMPRIMANTES BLUETOOTH
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BluetoothPrinterSheet(
    printers: List<ReceiptService.PrinterDevice>,
    receiptText: String,
    onDismiss: () -> Unit
) {
    var printing by remember { mutableStateOf(false) }
    var resultMsg by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = { if (!printing) onDismiss() },
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Border)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconCircle(
                    icon = LissafiIcons.Imprimer,
                    size = 48,
                    iconSize = 24
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Imprimer le reçu",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = OnBackground
                )
            }
            Spacer(Modifier.height(16.dp))
            when {
                printers.isEmpty() -> Text(
                    text = "Aucune imprimante Bluetooth appairée. Connecte une imprimante dans les réglages Bluetooth de ton téléphone.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
                printing -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Primary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Impression en cours…", fontSize = 14.sp)
                }
                else -> {
                    Text(text = "Choisir une imprimante :", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    printers.forEach { printer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceAlt)
                                .clickable {
                                    printing = true
                                    ReceiptService.printReceipt(printer.address, receiptText) { ok, msg ->
                                        resultMsg = msg
                                        printing = false
                                        (ctx.applicationContext as LissafiApp).supabaseApi.logEvent(
                                            eventType = "receipt",
                                            level = if (ok) "info" else "error",
                                            message = if (ok) "Reçu imprimé (Bluetooth)" else "Échec impression : $msg"
                                        )
                                    }
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = LissafiIcons.Imprimer,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(text = printer.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }
            }
            resultMsg?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = if (it.contains("succès")) Primary else Error,
                    fontWeight = FontWeight.Medium
                )
            }
            if (!printing) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Fermer", color = TextSecondary)
                }
            }
        }
    }
}

// ============================================================
// BOTTOM SHEET CHOIX CLIENT (CRÉDIT)
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientPickerSheet(
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
    val userId = app.authManager.currentUserId() ?: ""

    LaunchedEffect(Unit) {
        clients = app.database.getAllClients(userId)
        loading = false
    }
    LaunchedEffect(q) {
        loading = true
        clients = if (q.isBlank()) app.database.getAllClients(userId) else app.database.searchClients(q, userId)
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .imePadding()
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Border)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconCircle(
                    icon = LissafiIcons.Client,
                    backgroundColor = Secondary.copy(alpha = 0.1f),
                    iconTint = Secondary,
                    size = 48,
                    iconSize = 24
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (showNewClientForm) "Nouveau client" else "Vendre à crédit",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = OnBackground
                )
            }
            Spacer(Modifier.height(16.dp))

            if (showNewClientForm) {
                CapsuleTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = "Nom du client *"
                )
                Spacer(Modifier.height(10.dp))
                CapsuleTextField(
                    value = newPhone,
                    onValueChange = { newPhone = it.filter { c -> c.isDigit() || c == '+' } },
                    placeholder = "Téléphone (optionnel)"
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showNewClientForm = false },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Retour") }
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                scope.launch {
                                    if (!viewModel.canAddClient()) {
                                        Toast.makeText(ctx, "Limite atteinte. Passe à Premium pour ajouter plus de clients.", Toast.LENGTH_LONG).show()
                                        return@launch
                                    }
                                    val client = Client(
                                        id = java.util.UUID.randomUUID().toString(),
                                        name = newName.trim(),
                                        phone = newPhone.trim(),
                                        totalDebt = 0,
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis(),
                                        userId = userId
                                    )
                                    app.database.upsertClient(client)
                                    onClientSelected(client)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("Ajouter", fontWeight = FontWeight.SemiBold) }
                }
            } else {
                SecondaryActionButton(
                    text = "Nouveau client",
                    icon = LissafiIcons.Ajouter,
                    onClick = {
                        scope.launch {
                            if (!viewModel.canAddClient()) {
                                Toast.makeText(ctx, "Limite atteinte. Passe à Premium pour ajouter plus de clients.", Toast.LENGTH_LONG).show()
                            } else {
                                showNewClientForm = true
                            }
                        }
                    },
                    height = 48
                )
                Spacer(Modifier.height(10.dp))
                SearchField(
                    value = q,
                    onValueChange = { q = it },
                    placeholder = "Rechercher un client…"
                )
                Spacer(Modifier.height(8.dp))
                when {
                    loading -> Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    }
                    clients.isEmpty() -> Text(
                        text = "Aucun client. Ajoute le premier !",
                        fontSize = 13.sp,
                        color = TextTertiary,
                        modifier = Modifier.padding(16.dp)
                    )
                    else -> LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(clients.take(15)) { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceAlt)
                                    .clickable { onClientSelected(c) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconCircle(
                                    icon = LissafiIcons.Client,
                                    backgroundColor = Secondary.copy(alpha = 0.1f),
                                    iconTint = Secondary,
                                    size = 40,
                                    iconSize = 18
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = c.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    if (c.phone.isNotBlank()) {
                                        Text(c.phone, fontSize = 12.sp, color = TextSecondary)
                                    }
                                }
                                if (c.totalDebt > 0) {
                                    Spacer(Modifier.width(8.dp))
                                    AmountText(amount = c.totalDebt, fontSize = 13, color = Secondary)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Fermer", color = TextSecondary)
            }
        }
    }
}

// ============================================================
// TEXTE DU REÇU
// ============================================================
fun buildReceiptText(
    sale: LastSale,
    context: Context,
    shopName: String = "LISSAFI",
    shopPhone: String = "",
    footerMessage: String = ""
): String {
    return ReceiptService.formatReceipt(
        ReceiptService.ReceiptData(
            shopName = shopName.ifBlank { "LISSAFI" },
            shopPhone = shopPhone,
            date = sale.date,
            items = sale.items.map { ReceiptService.ReceiptItem(it.name, it.quantity, it.price) },
            total = sale.total,
            amountPaid = sale.amountPaid,
            changeGiven = sale.changeGiven,
            isCredit = sale.isCredit,
            footerMessage = footerMessage
        )
    )
}
