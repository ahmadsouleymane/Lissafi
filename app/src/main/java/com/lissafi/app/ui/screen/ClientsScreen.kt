package com.lissafi.app.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.data.entity.Client
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.components.AmountText
import com.lissafi.app.ui.components.EmptyState
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.PrimaryActionButton
import com.lissafi.app.ui.components.SearchField
import com.lissafi.app.ui.components.SectionHeader
import com.lissafi.app.ui.components.StatusBadge
import com.lissafi.app.ui.theme.Danger
import com.lissafi.app.ui.theme.LissafiCream
import com.lissafi.app.ui.theme.LissafiGreen
import com.lissafi.app.ui.theme.LissafiOrange
import com.lissafi.app.ui.theme.LissafiWhite
import com.lissafi.app.ui.theme.Neutral400
import com.lissafi.app.ui.theme.Neutral500
import com.lissafi.app.ui.theme.Success
import com.lissafi.app.ui.theme.Warning
import com.lissafi.app.ui.theme.White
import com.lissafi.app.ui.viewmodel.ClientViewModel
import kotlinx.coroutines.launch

private const val DAY_MS = 24 * 60 * 60 * 1000L

@Composable
fun ClientsScreen(viewModel: ClientViewModel, onClientClick: (String) -> Unit, onBack: () -> Unit) {
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
        LissafiHeader(
            title = "Clients",
            subtitle = "Qui te doit de l'argent",
            leadingIcon = Icons.Filled.People,
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = {
                        scope.launch {
                            if (viewModel.canAddClient()) showAddDialog = true
                            else Toast.makeText(context, "10 clients max en gratuit. Passe Premium !", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = "Ajouter un client",
                        tint = LissafiWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        )

        // ── RÉSUMÉ DES DETTES ──
        if (totalDebt > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LissafiOrange.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Payments,
                            contentDescription = null,
                            tint = LissafiOrange,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Total à recevoir",
                                color = LissafiOrange,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${state.clients.count { it.totalDebt > 0 }} client(s) vous doivent",
                                color = Neutral500,
                                fontSize = 11.sp
                            )
                        }
                    }
                    AmountText(amount = totalDebt, fontSize = 24, color = LissafiOrange)
                }
            }
        }

        SearchField(
            value = state.searchQuery,
            onValueChange = { viewModel.search(it) },
            placeholder = "Rechercher un client…",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        if (state.clients.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.People,
                title = if (state.searchQuery.isBlank()) "Aucun client pour l'instant" else "Aucun résultat",
                message = if (state.searchQuery.isBlank())
                    "Ajoute tes clients avant de leur faire crédit. Tu pourras ensuite suivre leurs dettes d'un coup d'œil."
                else
                    "Aucun client ne correspond à « ${state.searchQuery} ».",
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                actionLabel = if (state.searchQuery.isBlank()) "Ajouter un client" else null,
                onAction = {
                    scope.launch {
                        if (viewModel.canAddClient()) showAddDialog = true
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                if (state.searchQuery.isBlank()) {
                    item {
                        SectionHeader(
                            text = "DETTES ACTIVES",
                            icon = Icons.Filled.People,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                }
                items(state.clients) { client ->
                    val daysSince = (System.currentTimeMillis() - client.updatedAt) / DAY_MS
                    ClientCard(
                        client = client,
                        daysSince = daysSince.toInt(),
                        onClick = { onClientClick(client.id) }
                    )
                }
                if (!state.isPremium) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = LissafiOrange.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = LissafiOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "${state.clients.size}/10 clients en gratuit — Passe Premium",
                                    color = LissafiOrange,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddClientDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, phone ->
                viewModel.addClient(name, phone)
                showAddDialog = false
            }
        )
    }
}

// ============================================================
// CARTE CLIENT
// ============================================================
@Composable
private fun ClientCard(
    client: Client,
    daysSince: Int,
    onClick: () -> Unit
) {
    val hasDebt = client.totalDebt > 0
    // L'urgence ne concerne que les clients qui doivent encore de l'argent
    val (badge, badgeColor) = when {
        !hasDebt -> null to Success
        daysSince > 21 -> "Urgent" to Danger
        daysSince > 7 -> "À relancer" to Warning
        else -> null to Success
    }

    LissafiCard(modifier = Modifier.padding(vertical = 4.dp), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar avec code couleur selon le retard (uniquement s'il y a une dette)
            val avatarColor = badgeColor.takeIf { badge != null } ?: LissafiGreen
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avatarColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = avatarColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                if (hasDebt && badge != null) {
                    StatusBadge(text = badge, color = badgeColor)
                } else {
                    Text(
                        text = "Vu ${FormatUtils.formatDateShort(client.updatedAt)}",
                        fontSize = 12.sp,
                        color = Neutral400
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (hasDebt) {
                    Text(
                        text = "Doit",
                        fontSize = 11.sp,
                        color = Neutral400
                    )
                    AmountText(
                        amount = client.totalDebt,
                        fontSize = 18,
                        color = if (daysSince > 21) Danger else LissafiOrange
                    )
                } else {
                    StatusBadge(text = "À jour", color = Success)
                }
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Neutral400,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ============================================================
// DIALOGUE AJOUT CLIENT
// ============================================================
@Composable
private fun AddClientDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        icon = {
            Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = null,
                tint = LissafiGreen,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Column {
                Text(text = "Nouveau client", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Le nom suffit pour commencer.",
                    fontSize = 12.sp,
                    color = Neutral500
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du client *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } },
                    label = { Text("Téléphone (optionnel)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            PrimaryActionButton(
                text = "AJOUTER LE CLIENT",
                onClick = {
                    if (name.isNotBlank()) onSave(name.trim(), phone.trim())
                },
                enabled = name.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = Neutral500) }
        }
    )
}
