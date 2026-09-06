package com.lissafi.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.LissafiApp
import com.lissafi.app.data.entity.Client
import com.lissafi.app.data.entity.Product
import com.lissafi.app.data.entity.Sale
import com.lissafi.app.data.entity.SaleAuditEntry
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.components.AmountText
import com.lissafi.app.ui.components.CapsuleTextField
import com.lissafi.app.ui.components.EmptyState
import com.lissafi.app.ui.components.IconCircle
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.components.PrimaryActionButton
import com.lissafi.app.ui.components.QuantityStepper
import com.lissafi.app.ui.components.SearchField
import com.lissafi.app.ui.components.SectionHeader
import com.lissafi.app.ui.components.SegmentedControl
import com.lissafi.app.ui.components.StatusBadge
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.Border
import com.lissafi.app.ui.theme.Error
import com.lissafi.app.ui.theme.OnBackground
import com.lissafi.app.ui.theme.OnPrimary
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.Secondary
import com.lissafi.app.ui.theme.Success
import com.lissafi.app.ui.theme.Surface
import com.lissafi.app.ui.theme.SurfaceAlt
import com.lissafi.app.ui.theme.TextSecondary
import com.lissafi.app.ui.theme.TextTertiary
import com.lissafi.app.ui.viewmodel.CartItem
import com.lissafi.app.ui.viewmodel.SalesJournalViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SalesJournalScreen(
    viewModel: SalesJournalViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val detail by viewModel.detail.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val pinRequired by viewModel.pinRequired.collectAsState()

    // Rafraîchit le journal à chaque entrée sur l'écran (nouvelles ventes depuis la dernière visite).
    LaunchedEffect(Unit) { viewModel.loadJournal() }

    // Le bouton retour système ferme d'abord le détail ouvert, pas tout l'écran.
    BackHandler(enabled = detail != null) { viewModel.clearSelection() }

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(JournalFilter.TOUS) }
    val clientNameById = remember(clients) { clients.associate { it.id to it.name } }

    // Filtrage côté client (le journal charge jusqu'à 500 ventes).
    val filtered = remember(state.sales, query, filter, clientNameById) {
        state.sales.filter { s ->
            val q = query.trim()
            val matchQuery = q.isBlank() ||
                s.id.toString().contains(q) ||
                (s.clientId?.let { clientNameById[it] }?.contains(q, ignoreCase = true) == true)
            val matchFilter = when (filter) {
                JournalFilter.TOUS -> true
                JournalFilter.COMPTANT -> !s.isCredit && !s.cancelled
                JournalFilter.CREDIT -> s.isCredit && !s.cancelled
                JournalFilter.ANNULEES -> s.cancelled
            }
            matchQuery && matchFilter
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(Modifier.fillMaxSize()) {
            LissafiHeader(
                title = "Journal des ventes",
                subtitle = "Historique complet · modifiable et traçable",
                onBack = onBack
            )

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
                state.sales.isEmpty() -> EmptyState(
                    icon = LissafiIcons.Recents,
                    title = "Aucune vente",
                    message = "Les ventes encaissées apparaîtront ici. Tu pourras les consulter, les corriger ou les annuler — toujours avec une trace.",
                    modifier = Modifier.padding(top = 24.dp)
                )
                else -> {
                    // Bandeau récap (sur l'ensemble du journal, pas le filtre).
                    JournalRecap(state.sales)
                    SearchField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "Rechercher (N° vente ou client)…",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        JournalFilter.values().forEach { f ->
                            FilterChip(
                                selected = filter == f,
                                onClick = { filter = f },
                                label = { Text(f.label, fontSize = 12.sp) },
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
                            icon = LissafiIcons.Rechercher,
                            title = "Aucun résultat",
                            message = "Aucune vente ne correspond à ta recherche ou à ce filtre.",
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    } else {
                        // Pré-groupement par jour (les ventes sont déjà triées date desc) :
                        // on n'utilise JAMAIS un var mutable pour dédupliquer les en-têtes dans
                        // un LazyColumn (composition paresseuse, ordre non garanti).
                        val grouped = remember(filtered) {
                            val fmtDay = SimpleDateFormat("EEEE d MMMM", Locale.FRENCH)
                            val map = LinkedHashMap<String, MutableList<Sale>>()
                            for (s in filtered) {
                                val day = fmtDay.format(java.util.Date(s.date)).replaceFirstChar { it.uppercase() }
                                map.getOrPut(day) { mutableListOf() }.add(s)
                            }
                            map
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            grouped.forEach { (day, sales) ->
                                item(key = "header_$day") {
                                    SectionHeader(
                                        text = day,
                                        icon = LissafiIcons.Recents,
                                        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                                    )
                                }
                                items(sales, key = { it.id }) { sale ->
                                    SaleJournalRow(sale) { viewModel.openSale(sale.id) }
                                }
                            }
                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                }
            }
        }

        // ── DÉTAIL / ÉDITEUR (plein écran par-dessus la liste) ──
        detail?.let { d ->
            SaleDetailScreen(
                sale = d.sale,
                initialItems = d.items,
                audit = d.audit,
                clientName = d.clientName,
                clients = clients,
                pinRequired = pinRequired,
                onVerifyPin = { pin, cb -> viewModel.verifyPin(pin, cb) },
                onClose = { viewModel.clearSelection() },
                onSave = { items, isCredit, client -> viewModel.saveModification(items, isCredit, client) },
                onCancel = { reason -> viewModel.cancelSale(reason) }
            )
        }
    }
}

private enum class JournalFilter(val label: String) {
    TOUS("Toutes"),
    COMPTANT("Comptant"),
    CREDIT("Crédit"),
    ANNULEES("Annulées")
}

/** Bandeau récap : encaissé (hors annulées), nb de ventes, nb d'annulations. */
@Composable
private fun JournalRecap(sales: List<Sale>) {
    val encaisse = sales.filter { !it.cancelled }.sumOf { it.total }
    val nbVentes = sales.count { !it.cancelled }
    val nbAnnulees = sales.count { it.cancelled }
    LissafiCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        cornerRadius = 16,
        elevation = 2
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RecapCell(FormatUtils.formatFCFA(encaisse).removeSuffix(" FCFA"), "encaissé", Primary)
            RecapCell("$nbVentes", "vente${if (nbVentes > 1) "s" else ""}", OnBackground)
            RecapCell("$nbAnnulees", "annulée${if (nbAnnulees > 1) "s" else ""}", if (nbAnnulees > 0) Error else TextTertiary)
        }
    }
}

@Composable
private fun RecapCell(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = color, maxLines = 1)
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun SaleJournalRow(sale: Sale, onClick: () -> Unit) {
    val fmtTime = remember { SimpleDateFormat("HH:mm", Locale.FRENCH) }
    LissafiCard(modifier = Modifier.padding(vertical = 4.dp), onClick = onClick, cornerRadius = 16, elevation = 1) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconCircle(
                icon = if (sale.isCredit) LissafiIcons.Credit else LissafiIcons.Encaisser,
                backgroundColor = (if (sale.isCredit) Secondary else Primary).copy(alpha = 0.1f),
                iconTint = if (sale.isCredit) Secondary else Primary,
                size = 42, iconSize = 20
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Vente N°${sale.id} · ${fmtTime.format(java.util.Date(sale.date))}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = OnBackground,
                    textDecoration = if (sale.cancelled) TextDecoration.LineThrough else null
                )
                Spacer(Modifier.height(3.dp))
                when {
                    sale.cancelled -> StatusBadge(text = "Annulée", color = Error)
                    sale.isCredit -> StatusBadge(text = "Crédit", color = Secondary)
                    else -> StatusBadge(text = "Comptant", color = Success)
                }
            }
            Spacer(Modifier.width(8.dp))
            AmountText(
                amount = sale.total,
                fontSize = 16,
                color = if (sale.cancelled) TextTertiary else OnBackground
            )
        }
    }
}

// ============================================================
// DÉTAIL + ÉDITEUR D'UNE VENTE
// ============================================================
@Composable
private fun SaleDetailScreen(
    sale: Sale,
    initialItems: List<CartItem>,
    audit: List<SaleAuditEntry>,
    clientName: String?,
    clients: List<Client>,
    pinRequired: Boolean,
    onVerifyPin: (String, (Boolean) -> Unit) -> Unit,
    onClose: () -> Unit,
    onSave: (items: List<CartItem>, isCredit: Boolean, client: Client?) -> Unit,
    onCancel: (reason: String) -> Unit
) {
    val editItems = remember(sale.id) { mutableStateListOf<CartItem>().apply { addAll(initialItems) } }
    var isCredit by remember(sale.id) { mutableStateOf(sale.isCredit) }
    var selectedClient by remember(sale.id) { mutableStateOf(clients.find { it.id == sale.clientId }) }
    var showAddItem by remember { mutableStateOf(false) }
    var showClientPicker by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    // Action en attente de validation par le PIN gérant (modif ou annulation).
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Exécute l'action directement, ou via le verrou PIN s'il est activé.
    fun guarded(action: () -> Unit) {
        if (pinRequired) pendingAction = action else action()
    }

    val readOnly = sale.cancelled
    val total = editItems.sumOf { (it.price * it.quantity).roundToInt() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(Modifier.fillMaxSize()) {
            LissafiHeader(
                title = "Vente N°${sale.id}",
                subtitle = FormatUtils.formatDate(sale.date),
                onBack = onClose
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .imePadding()
            ) {
                if (readOnly) {
                    LissafiCard(containerColor = Error.copy(alpha = 0.08f), cornerRadius = 16, elevation = 0) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(LissafiIcons.Alerte, contentDescription = null, tint = Error, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Vente annulée. Elle est conservée pour la traçabilité (voir l'historique ci-dessous).",
                                fontSize = 13.sp, color = OnBackground, lineHeight = 18.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── ARTICLES ──
                SectionHeader(text = "ARTICLES", icon = LissafiIcons.Produit, modifier = Modifier.padding(bottom = 8.dp))
                LissafiCard(cornerRadius = 16, containerColor = SurfaceAlt, elevation = 0) {
                    Column(Modifier.padding(8.dp)) {
                        if (editItems.isEmpty()) {
                            Text("Aucun article.", fontSize = 13.sp, color = TextTertiary, modifier = Modifier.padding(12.dp))
                        }
                        editItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${FormatUtils.formatFCFA(item.price)} · ${FormatUtils.formatFCFA((item.price * item.quantity).roundToInt())}",
                                        fontSize = 12.sp, color = TextSecondary
                                    )
                                }
                                if (!readOnly) {
                                    QuantityStepper(
                                        quantity = item.quantity,
                                        onDecrease = {
                                            if (item.quantity > 1) editItems[index] = item.copy(quantity = item.quantity - 1)
                                            else editItems.removeAt(index)
                                        },
                                        onIncrease = { editItems[index] = item.copy(quantity = item.quantity + 1) }
                                    )
                                } else {
                                    Text("×${formatQty(item.quantity)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                }
                            }
                            if (index < editItems.size - 1) HorizontalDivider(color = Border, thickness = 0.5.dp)
                        }
                    }
                }
                if (!readOnly) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showAddItem = true }) {
                        Icon(LissafiIcons.Ajouter, contentDescription = null, modifier = Modifier.size(18.dp), tint = Primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Ajouter un article", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── PAIEMENT ──
                if (!readOnly) {
                    SectionHeader(text = "PAIEMENT", icon = LissafiIcons.Encaisser, modifier = Modifier.padding(bottom = 8.dp))
                    SegmentedControl(
                        options = listOf("Comptant", "Crédit"),
                        selectedIndex = if (isCredit) 1 else 0,
                        onSelect = { idx ->
                            isCredit = idx == 1
                            if (isCredit && selectedClient == null) showClientPicker = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isCredit) {
                        Spacer(Modifier.height(8.dp))
                        LissafiCard(cornerRadius = 12, containerColor = Secondary.copy(alpha = 0.08f), elevation = 0, onClick = { showClientPicker = true }) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(LissafiIcons.Client, contentDescription = null, tint = Secondary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    selectedClient?.name ?: "Choisir un client",
                                    fontSize = 14.sp, color = if (selectedClient != null) OnBackground else Secondary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("Changer", fontSize = 12.sp, color = Secondary)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── TOTAL ──
                LissafiCard(cornerRadius = 16, elevation = 2) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextSecondary)
                        AmountText(amount = if (readOnly) sale.total else total, fontSize = 22, color = Primary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── HISTORIQUE (audit) ──
                SectionHeader(text = "HISTORIQUE", icon = LissafiIcons.Recents, modifier = Modifier.padding(bottom = 8.dp))
                LissafiCard(cornerRadius = 16, containerColor = SurfaceAlt, elevation = 0) {
                    Column(Modifier.padding(12.dp)) {
                        if (audit.isEmpty()) {
                            Text("Aucun historique.", fontSize = 13.sp, color = TextTertiary)
                        }
                        audit.forEachIndexed { index, entry ->
                            AuditRow(entry)
                            if (index < audit.size - 1) Spacer(Modifier.height(10.dp))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── ACTIONS ──
                if (!readOnly) {
                    PrimaryActionButton(
                        text = "Enregistrer les modifications",
                        icon = LissafiIcons.Valider,
                        onClick = { guarded { onSave(editItems.toList(), isCredit, selectedClient) } },
                        enabled = editItems.isNotEmpty() && (!isCredit || selectedClient != null) && total > 0
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                    ) {
                        Icon(LissafiIcons.Supprimer, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Annuler cette vente", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    if (showAddItem) {
        AddItemDialog(
            onDismiss = { showAddItem = false },
            onAdd = { product ->
                val existing = editItems.indexOfFirst { it.barcode == product.barcode }
                if (existing >= 0) editItems[existing] = editItems[existing].copy(quantity = editItems[existing].quantity + 1)
                else editItems.add(CartItem(product.barcode, product.name, product.sellPrice, 1.0))
                showAddItem = false
            }
        )
    }

    if (showClientPicker) {
        ClientPickerDialog(
            clients = clients,
            onDismiss = { showClientPicker = false },
            onSelect = { selectedClient = it; showClientPicker = false }
        )
    }

    if (showCancelDialog) {
        CancelReasonDialog(
            onDismiss = { showCancelDialog = false },
            onConfirm = { reason -> showCancelDialog = false; guarded { onCancel(reason) } }
        )
    }

    if (pendingAction != null) {
        PinPromptDialog(
            onVerify = onVerifyPin,
            onDismiss = { pendingAction = null },
            onSuccess = {
                val action = pendingAction
                pendingAction = null
                action?.invoke()
            }
        )
    }
}

@Composable
private fun AuditRow(entry: SaleAuditEntry) {
    val color = when (entry.action) {
        "created" -> Success
        "cancelled" -> Error
        else -> Secondary
    }
    val label = when (entry.action) {
        "created" -> "Créée"
        "cancelled" -> "Annulée"
        "modified" -> "Modifiée"
        else -> entry.action
    }
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.padding(top = 5.dp).size(8.dp).clip(CircleShape).background(color)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = color)
            if (entry.details.isNotBlank()) {
                Text(entry.details, fontSize = 12.sp, color = OnBackground, lineHeight = 16.sp)
            }
            Text(FormatUtils.formatDate(entry.date), fontSize = 11.sp, color = TextTertiary)
        }
    }
}

// ── Dialogue : ajouter un article (recherche produit) ──
@Composable
private fun AddItemDialog(onDismiss: () -> Unit, onAdd: (Product) -> Unit) {
    val context = LocalContext.current
    val app = remember { context.applicationContext as LissafiApp }
    val userId = app.authManager.currentUserId() ?: ""
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Product>>(emptyList()) }

    LaunchedEffect(query) {
        results = if (query.isBlank()) app.database.getRecentProducts(20, userId)
        else app.database.searchProducts(query, userId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Surface,
        title = { Text("Ajouter un article", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OnBackground) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SearchField(value = query, onValueChange = { query = it }, placeholder = "Rechercher un produit…")
                Spacer(Modifier.height(8.dp))
                if (results.isEmpty()) {
                    Text("Aucun produit.", fontSize = 13.sp, color = TextTertiary, modifier = Modifier.padding(8.dp))
                } else {
                    results.take(30).forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onAdd(p) }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                AmountText(amount = p.sellPrice, fontSize = 12, color = TextSecondary)
                            }
                            Icon(LissafiIcons.Ajouter, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fermer", color = TextSecondary) } }
    )
}

// ── Dialogue : choisir un client ──
@Composable
private fun ClientPickerDialog(clients: List<Client>, onDismiss: () -> Unit, onSelect: (Client) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Surface,
        title = { Text("Choisir un client", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OnBackground) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (clients.isEmpty()) {
                    Text("Aucun client enregistré.", fontSize = 13.sp, color = TextTertiary, modifier = Modifier.padding(8.dp))
                } else {
                    clients.forEach { c ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSelect(c) }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconCircle(icon = LissafiIcons.Client, backgroundColor = Secondary.copy(alpha = 0.1f), iconTint = Secondary, size = 36, iconSize = 18)
                            Spacer(Modifier.width(10.dp))
                            Text(c.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fermer", color = TextSecondary) } }
    )
}

// ── Dialogue : motif d'annulation (obligatoire) ──
@Composable
private fun CancelReasonDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Surface,
        icon = {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(Error.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) { Icon(LissafiIcons.Supprimer, contentDescription = null, tint = Error, modifier = Modifier.size(28.dp)) }
        },
        title = { Text("Annuler cette vente ?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OnBackground, textAlign = TextAlign.Center) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "La vente ne sera pas supprimée : elle reste dans le journal, marquée annulée, avec ce motif. Le stock est restauré et la dette annulée si c'était un crédit.",
                    fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))
                CapsuleTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = "Motif de l'annulation *",
                    singleLine = false,
                    minLines = 2
                )
            }
        },
        confirmButton = {
            PrimaryActionButton(
                text = "Confirmer l'annulation",
                onClick = { if (reason.isBlank()) Unit else onConfirm(reason) },
                enabled = reason.isNotBlank(),
                containerColor = Error
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Retour", color = TextSecondary) } }
    )
}

// ── Dialogue : saisie du PIN gérant (verrou modif/annulation) ──
@Composable
private fun PinPromptDialog(
    onVerify: (String, (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Surface,
        icon = {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) { Icon(LissafiIcons.Motdepasse, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp)) }
        },
        title = { Text("Code gérant requis", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OnBackground, textAlign = TextAlign.Center) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Entre le code pour confirmer cette opération sur la vente.",
                    fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6); error = false },
                    placeholder = { Text("Code (4 à 6 chiffres)") },
                    singleLine = true,
                    isError = error,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error) {
                    Text("Code incorrect.", color = Error, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }
            }
        },
        confirmButton = {
            PrimaryActionButton(
                text = "Valider",
                icon = LissafiIcons.Valider,
                enabled = pin.length >= 4 && !checking,
                onClick = {
                    checking = true
                    onVerify(pin) { ok ->
                        checking = false
                        if (ok) onSuccess() else error = true
                    }
                }
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = TextSecondary) } }
    )
}

private fun formatQty(q: Double): String = if (q == q.toInt().toDouble()) q.toInt().toString() else q.toString()
