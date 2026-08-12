package com.lissafi.app.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.components.AmountText
import com.lissafi.app.ui.components.EmptyState
import com.lissafi.app.ui.components.IconCircle
import com.lissafi.app.ui.components.InfoRow
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.components.SectionHeader
import com.lissafi.app.ui.components.SegmentedControl
import com.lissafi.app.ui.components.TrendBadge
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.OnBackground
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.Secondary
import com.lissafi.app.ui.theme.Success
import com.lissafi.app.ui.theme.SurfaceAlt
import com.lissafi.app.ui.theme.TextSecondary
import com.lissafi.app.ui.theme.TextTertiary
import com.lissafi.app.ui.theme.White
import com.lissafi.app.ui.viewmodel.ReportPeriod
import com.lissafi.app.ui.viewmodel.ReportState
import com.lissafi.app.ui.viewmodel.ReportViewModel
import com.lissafi.app.ui.viewmodel.RevenuePoint
import com.lissafi.app.data.LissafiDatabase.TopProduct
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import java.text.SimpleDateFormat
import java.util.Locale

// ============================================================
// ÉCRAN ACTIVITÉ — Dashboard avec sparkline Vico + KPIs
// ============================================================
@Composable
fun ReportsScreen(
    viewModel: ReportViewModel,
    onBack: () -> Unit,
    onNavigateToProducts: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    // Rafraîchit le rapport dès l'entrée sur l'écran, sans action de l'utilisateur.
    LaunchedEffect(Unit) {
        viewModel.loadReport(state.period)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        LissafiHeader(
            title = "Activité",
            subtitle = when (state.period) {
                ReportPeriod.TODAY -> "Aujourd'hui"
                ReportPeriod.WEEK -> "Cette semaine"
                ReportPeriod.MONTH -> "Ce mois"
            },
            onBack = onBack
        )

        // ── PÉRIODE ──
        SegmentedControl(
            options = listOf("Aujourd'hui", "Cette semaine", "Ce mois"),
            selectedIndex = when (state.period) {
                ReportPeriod.TODAY -> 0
                ReportPeriod.WEEK -> 1
                ReportPeriod.MONTH -> 2
            },
            onSelect = { index ->
                viewModel.loadReport(
                    when (index) {
                        0 -> ReportPeriod.TODAY
                        1 -> ReportPeriod.WEEK
                        else -> ReportPeriod.MONTH
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Le stock bas est géré dans l'écran Produits (badge par produit).

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
            }
        } else if (state.nbTransactions == 0) {
            EmptyState(
                icon = LissafiIcons.Activite,
                title = "Aucune vente",
                message = "Les chiffres de ta caisse apparaîtront ici. Enregistre une vente depuis la caisse pour commencer.",
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                // ── CARTE CHIFFRE D'AFFAIRES + SPARKLINE ──
                item { RevenueCard(state) }

                // ── GRILLE DE KPIs 2×2 ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiTile(
                            label = "Comptant",
                            value = state.totalComptant,
                            icon = LissafiIcons.Encaisser,
                            color = Primary,
                            percentage = if (state.totalVentes > 0) (state.totalComptant.toLong() * 100 / state.totalVentes).toInt() else 0,
                            trend = state.comptantTrend,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        KpiTile(
                            label = "À crédit",
                            value = state.totalCredits,
                            icon = LissafiIcons.Credit,
                            color = Secondary,
                            percentage = if (state.totalVentes > 0) (state.totalCredits.toLong() * 100 / state.totalVentes).toInt() else 0,
                            trend = state.creditTrend,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiTile(
                            label = "Marge estimée",
                            value = state.estimatedProfit,
                            icon = LissafiIcons.Marge,
                            color = Success,
                            percentage = if (state.totalVentes > 0) (state.estimatedProfit.toLong() * 100 / state.totalVentes).toInt() else 0,
                            trend = state.profitTrend,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        KpiTile(
                            label = "Panier moyen",
                            value = state.panierMoyen,
                            icon = LissafiIcons.Panier,
                            color = TextSecondary,
                            percentage = null,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }

                // ── HEURES DE POINTE — peu lisible sur un mois entier ──
                if (state.period != ReportPeriod.MONTH && state.hourlyBreakdown.any { it > 0 }) {
                    item {
                        SectionHeader(
                            text = "Heures de pointe",
                            icon = LissafiIcons.Recents,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    item {
                        LissafiCard(cornerRadius = 18, elevation = 2) {
                            HourlyBarsChart(
                                hourly = state.hourlyBreakdown,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }
                }

                // ── PRODUITS LES PLUS VENDUS ──
                if (state.topProducts.isNotEmpty()) {
                    item {
                        SectionHeader(
                            text = "Produits les plus vendus",
                            icon = LissafiIcons.Produit,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(state.topProducts.take(10)) { product ->
                        TopProductBar(
                            product = product,
                            maxCount = state.topProducts.first().count
                        )
                    }
                }

                // ── CRÉDITS EN ATTENTE ──
                if (state.totalCredits > 0) {
                    item {
                        SectionHeader(
                            text = "Crédits en attente",
                            icon = LissafiIcons.Credit,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    item {
                        LissafiCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                InfoRow(
                                    icon = LissafiIcons.Credit,
                                    label = "Total à recouvrer",
                                    value = FormatUtils.formatFCFA(state.totalCredits),
                                    valueColor = Secondary
                                )
                                InfoRow(
                                    icon = LissafiIcons.Encaisser,
                                    label = "Ventes comptant",
                                    value = FormatUtils.formatFCFA(state.totalComptant),
                                    valueColor = Primary
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ============================================================
// CARTE CHIFFRE D'AFFAIRES — Fond vert Primary + sparkline Vico
// ============================================================
@Composable
private fun RevenueCard(state: ReportState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = LissafiIcons.Activite,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Chiffre d'affaires",
                    color = White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(8.dp))
            AnimatedContent(targetState = state.totalVentes, label = "chiffreAffaires") { total ->
                Text(
                    text = FormatUtils.formatFCFA(total),
                    color = White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${state.nbTransactions} vente${if (state.nbTransactions > 1) "s" else ""} sur la période",
                color = White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
            if (state.bestDay != null && state.revenueSeries.size >= 2) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Meilleur jour : ${formatDayLabel(state.bestDay.date)} · ${FormatUtils.formatFCFA(state.bestDay.amount)}",
                    color = White.copy(alpha = 0.75f),
                    fontSize = 11.sp
                )
            }

            // Sparkline du CA par jour — seulement si assez de points
            if (state.revenueSeries.size >= 2) {
                Spacer(Modifier.height(12.dp))
                RevenueSparkline(series = state.revenueSeries)
            }
        }
    }
}

private fun formatDayLabel(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEE d", Locale.FRENCH)
    return sdf.format(java.util.Date(timestamp))
}

// ============================================================
// SPARKLINE VICO — Ligne blanche sur fond vert, sans axes
// ============================================================
@Composable
private fun RevenueSparkline(series: List<RevenuePoint>) {
    val modelProducer = remember { CartesianChartModelProducer() }

    // Alimente le modèle avec les montants (série temporelle du CA)
    LaunchedEffect(series) {
        modelProducer.runTransaction {
            lineSeries {
                val amounts: List<Number> = series.map { it.amount.toFloat() as Number }
                series(amounts)
            }
        }
    }

    // Ligne blanche (contraste sur la carte vert Primary) avec remplissage translucide.
    // Les autres paramètres (points, labels, connecteur) gardent leur valeur par défaut.
    val line = remember {
        LineCartesianLayer.Line(
            fill = LineCartesianLayer.LineFill.single(fill(Color.White)),
            stroke = LineCartesianLayer.LineStroke.Continuous(4f),
            areaFill = LineCartesianLayer.AreaFill.single(fill(Color.White.copy(alpha = 0.25f)))
        )
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(line)
            )
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    )
}

// ============================================================
// BARRES HORAIRES — Répartition des ventes par heure (0-23h)
// Composant Compose natif (comme TopProductBar), pas Vico : évite
// la complexité d'un axe personnalisé pour 24 barres compactes.
// ============================================================
@Composable
private fun HourlyBarsChart(hourly: List<Int>, modifier: Modifier = Modifier) {
    val maxCount = (hourly.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            hourly.forEach { count ->
                val fraction = (count.toFloat() / maxCount).coerceIn(0.04f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(if (count > 0) Primary else SurfaceAlt)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, 6, 12, 18, 23).forEach { h ->
                Text(text = "${h}h", fontSize = 10.sp, color = TextTertiary)
            }
        }
    }
}

// ============================================================
// TUILE KPI — Petite carte blanche avec icône en cercle
// ============================================================
@Composable
private fun KpiTile(
    label: String,
    value: Int,
    icon: ImageVector,
    color: Color,
    percentage: Int?,
    modifier: Modifier = Modifier,
    trend: Int? = null
) {
    LissafiCard(modifier = modifier, cornerRadius = 18, elevation = 2) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconCircle(
                    icon = icon,
                    backgroundColor = color.copy(alpha = 0.1f),
                    iconTint = color,
                    size = 36,
                    iconSize = 18
                )
                if (percentage != null) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "$percentage%",
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            AmountText(
                amount = value,
                fontSize = 17,
                color = color,
                fontWeight = FontWeight.Bold
            )
            if (trend != null) {
                Spacer(Modifier.height(4.dp))
                TrendBadge(percentage = trend)
            }
        }
    }
}

// ============================================================
// BARRE PRODUIT — Barre horizontale proportionnelle
// ============================================================
@Composable
private fun TopProductBar(product: TopProduct, maxCount: Int) {
    val fraction = if (maxCount > 0) (product.count.toFloat() / maxCount).coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = product.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${product.count}×",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SurfaceAlt)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Primary)
            )
        }
    }
}
