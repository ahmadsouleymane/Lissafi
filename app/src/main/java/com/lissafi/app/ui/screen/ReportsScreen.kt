package com.lissafi.app.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.components.EmptyState
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.SectionHeader
import com.lissafi.app.ui.components.StatusBadge
import com.lissafi.app.ui.theme.Danger
import com.lissafi.app.ui.theme.LissafiCream
import com.lissafi.app.ui.theme.LissafiGreen
import com.lissafi.app.ui.theme.LissafiOrange
import com.lissafi.app.ui.theme.Neutral400
import com.lissafi.app.ui.theme.Neutral500
import com.lissafi.app.ui.theme.Success
import com.lissafi.app.ui.theme.White
import com.lissafi.app.ui.viewmodel.ReportPeriod
import com.lissafi.app.ui.viewmodel.ReportViewModel
import com.lissafi.app.ui.viewmodel.TopProduct

private data class PeriodOption(val label: String, val period: ReportPeriod)

@Composable
fun ReportsScreen(
    viewModel: ReportViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LissafiCream)
    ) {
        LissafiHeader(
            title = "Rapports",
            subtitle = "Ton activité en un coup d'œil",
            leadingIcon = Icons.Filled.BarChart,
            onBack = onBack
        )

        // ── FILTRES PÉRIODE ──
        val periods = listOf(
            PeriodOption("Aujourd'hui", ReportPeriod.TODAY),
            PeriodOption("Semaine", ReportPeriod.WEEK),
            PeriodOption("Mois", ReportPeriod.MONTH)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            periods.forEach { option ->
                FilterChip(
                    selected = state.period == option.period,
                    onClick = { viewModel.loadReport(option.period) },
                    label = {
                        Text(
                            text = option.label,
                            fontSize = 12.sp,
                            fontWeight = if (state.period == option.period) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LissafiGreen,
                        selectedLabelColor = White,
                        containerColor = White
                    )
                )
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = LissafiGreen, strokeWidth = 3.dp)
            }
        } else if (state.nbTransactions == 0) {
            EmptyState(
                icon = Icons.Filled.BarChart,
                title = "Aucune vente sur cette période",
                message = "Les chiffres de ta caisse apparaîtront ici. Enregistre une vente depuis la caisse pour commencer.",
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                // ── VENTES TOTALES (carte mise en avant) ──
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = LissafiGreen),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Ventes totales",
                                    color = White.copy(alpha = 0.85f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            AnimatedContent(targetState = state.totalVentes, label = "ventes") { total ->
                                Text(
                                    text = FormatUtils.formatFCFA(total),
                                    color = White,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${state.nbTransactions} vente${if (state.nbTransactions > 1) "s" else ""} · panier moyen ${FormatUtils.formatFCFA(state.panierMoyen)}",
                                color = White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // ── GRILLE DE STATS ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            label = "Comptant",
                            value = state.totalComptant,
                            icon = Icons.Filled.Payments,
                            color = LissafiGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Crédit",
                            value = state.totalCredits,
                            icon = Icons.Filled.CreditCard,
                            color = LissafiOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            label = "Bénéfice estimé",
                            value = state.estimatedProfit,
                            icon = Icons.Filled.TrendingUp,
                            color = Success,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Crédits à recevoir",
                            value = state.totalCredits,
                            icon = Icons.Filled.People,
                            color = Danger,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── TOP PRODUITS ──
                if (state.topProducts.isNotEmpty()) {
                    item {
                        SectionHeader(
                            text = "TOP PRODUITS",
                            icon = Icons.Filled.ShoppingBag,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                    itemsIndexed(state.topProducts.take(10)) { index, product ->
                        TopProductRow(index = index, product = product)
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ============================================================
// TUILE STATISTIQUE
// ============================================================
@Composable
private fun StatTile(
    label: String,
    value: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(vertical = 2.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = Neutral500,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = FormatUtils.formatFCFA(value),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1
            )
        }
    }
}

// ============================================================
// LIGNE TOP PRODUIT
// ============================================================
@Composable
private fun TopProductRow(index: Int, product: TopProduct) {
    val rankColor = if (index == 0) LissafiGreen else if (index == 1) LissafiOrange else Neutral400

    LissafiCard(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(rankColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (index + 1).toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = rankColor
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = "Vendu ${product.count} fois",
                    fontSize = 12.sp,
                    color = Neutral400
                )
            }
            if (index == 0) {
                StatusBadge(text = "N°1", color = LissafiGreen)
            }
        }
    }
}
