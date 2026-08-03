package com.lissafi.app.ui.screen

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.theme.LissafiDanger
import com.lissafi.app.ui.theme.LissafiGreen
import com.lissafi.app.ui.theme.LissafiOrange
import com.lissafi.app.ui.theme.LissafiCream
import com.lissafi.app.ui.theme.LissafiWhite
import com.lissafi.app.ui.viewmodel.ReportPeriod
import com.lissafi.app.ui.viewmodel.ReportViewModel

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
                Text("← RAPPORTS", color = LissafiWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Tabs période
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Aujourd'hui" to ReportPeriod.TODAY,
                "Semaine" to ReportPeriod.WEEK,
                "Mois" to ReportPeriod.MONTH
            ).forEach { (label, period) ->
                Button(
                    onClick = { viewModel.loadReport(period) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.period == period) LissafiGreen else LissafiOrange.copy(alpha = 0.3f)
                    )
                ) {
                    Text(label, fontSize = 12.sp)
                }
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                color = LissafiGreen
            )
        }

        // Stats
        LazyColumn(modifier = Modifier.padding(horizontal = 12.dp)) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = LissafiWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        StatRow("Ventes totales", FormatUtils.formatFCFA(state.totalVentes))
                        StatRow("Dont crédit", FormatUtils.formatFCFA(state.totalCredits), LissafiOrange)
                        StatRow("Dont comptant", FormatUtils.formatFCFA(state.totalComptant), LissafiGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        StatRow("Nb transactions", state.nbTransactions.toString())
                        StatRow("Panier moyen", FormatUtils.formatFCFA(state.panierMoyen))
                    }
                }
            }

            if (state.topProducts.isNotEmpty()) {
                item {
                    Text(
                        "TOP PRODUITS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LissafiGreen,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }

                itemsIndexed(state.topProducts) { index, product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = LissafiWhite)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}. ${product.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${product.count.toInt()} ventes", fontSize = 13.sp, color = LissafiGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = LissafiGreen) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = valueColor)
    }
}
