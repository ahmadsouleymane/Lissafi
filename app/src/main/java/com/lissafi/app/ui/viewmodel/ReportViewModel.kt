package com.lissafi.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lissafi.app.data.LissafiDatabase.TopProduct
import com.lissafi.app.data.entity.Product
import com.lissafi.app.data.entity.Sale
import com.lissafi.app.data.repository.LissafiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.util.Calendar

enum class ReportPeriod { TODAY, WEEK, MONTH }

// Point de la série temporelle du chiffre d'affaires (pour la sparkline)
data class RevenuePoint(
    val date: Long,    // epoch millis du début du jour
    val amount: Int    // CA total ce jour-là
)

data class ReportState(
    val period: ReportPeriod = ReportPeriod.TODAY,
    val totalVentes: Int = 0,
    val totalCredits: Int = 0,
    val totalComptant: Int = 0,
    val estimatedProfit: Int = 0,
    val nbTransactions: Int = 0,
    val panierMoyen: Int = 0,
    val topProducts: List<TopProduct> = emptyList(),
    val revenueSeries: List<RevenuePoint> = emptyList(),
    val comptantTrend: Int? = null,
    val creditTrend: Int? = null,
    val profitTrend: Int? = null,
    val hourlyBreakdown: List<Int> = emptyList(),
    val lowStockProducts: List<Product> = emptyList(),
    val bestDay: RevenuePoint? = null,
    val isLoading: Boolean = false
)

class ReportViewModel(private val repository: LissafiRepository) : ViewModel() {

    private val _state = MutableStateFlow(ReportState())
    val state: StateFlow<ReportState> = _state.asStateFlow()

    init {
        loadReport(ReportPeriod.TODAY)
    }

    fun loadReport(period: ReportPeriod) {
        _state.value = _state.value.copy(period = period, isLoading = true)
        viewModelScope.launch {
            val (start, end) = getDateRange(period)
            val total = repository.sumTotalBetween(start, end)
            val credit = repository.sumCreditBetween(start, end)
            val count = repository.countSalesBetween(start, end)
            val top = repository.getTopProducts(start, end)
            val sales = repository.getSalesBetween(start, end)
            val comptant = total - credit
            val profit = computeProfit(sales)

            // Série temporelle du CA par jour (pour la sparkline de l'écran Activité).
            val revenueSeries = sales
                .groupBy { startOfDay(it.date) }
                .map { (day, daySales) -> RevenuePoint(day, daySales.sumOf { it.total }) }
                .sortedBy { it.date }

            // Répartition par heure de la journée — calculée en mémoire à partir des
            // ventes déjà chargées, pas de requête DB supplémentaire.
            val hourly = IntArray(24)
            for (sale in sales) {
                val cal = Calendar.getInstance()
                cal.timeInMillis = sale.date
                hourly[cal.get(Calendar.HOUR_OF_DAY)]++
            }

            // Comparaison avec la période équivalente immédiatement précédente
            // (même durée que [start, end), juste avant start).
            val (prevStart, prevEnd) = getPreviousDateRange(start, end)
            val prevTotal = repository.sumTotalBetween(prevStart, prevEnd)
            val prevCredit = repository.sumCreditBetween(prevStart, prevEnd)
            val prevComptant = prevTotal - prevCredit
            val prevSales = repository.getSalesBetween(prevStart, prevEnd)
            val prevProfit = computeProfit(prevSales)

            val lowStock = repository.getLowStockProducts()
            val bestDay = revenueSeries.maxByOrNull { it.amount }

            _state.value = _state.value.copy(
                totalVentes = total,
                totalCredits = credit,
                totalComptant = comptant,
                estimatedProfit = profit,
                nbTransactions = count,
                panierMoyen = if (count > 0) total / count else 0,
                topProducts = top,
                revenueSeries = revenueSeries,
                comptantTrend = trendPercent(comptant, prevComptant),
                creditTrend = trendPercent(credit, prevCredit),
                profitTrend = trendPercent(profit, prevProfit),
                hourlyBreakdown = hourly.toList(),
                lowStockProducts = lowStock,
                bestDay = bestDay,
                isLoading = false
            )
        }
    }

    // Bénéfice estimé : somme des ventes - somme des prix d'achat. Toutes les
    // ventes comptent dans la marge (comptant ET crédit) : la marchandise sort
    // dans les deux cas.
    private suspend fun computeProfit(sales: List<Sale>): Int {
        var profit = 0
        for (sale in sales) {
            val items = repository.getSaleItems(sale.id)
            for (item in items) {
                val product = repository.getProduct(item.barcode)
                if (product != null && product.buyPrice > 0) {
                    profit += ((item.price - product.buyPrice) * item.quantity).roundToInt()
                } else {
                    // Pas de prix d'achat connu → on compte le prix de vente comme bénéfice
                    profit += (item.price * item.quantity).roundToInt()
                }
            }
        }
        return profit
    }

    // % d'évolution vs la période précédente. null si la période précédente n'a
    // aucune donnée (évite un pourcentage absurde du type +∞%).
    private fun trendPercent(current: Int, previous: Int): Int? {
        if (previous <= 0) return null
        return (((current - previous).toDouble() / previous) * 100).roundToInt()
    }

    private fun getDateRange(period: ReportPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = when (period) {
            ReportPeriod.TODAY -> cal.timeInMillis
            ReportPeriod.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.timeInMillis
            }
            ReportPeriod.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis
            }
        }
        return start to end
    }

    // Période équivalente précédente : même durée que [start, end), juste avant `start`.
    private fun getPreviousDateRange(start: Long, end: Long): Pair<Long, Long> {
        val duration = end - start
        return (start - duration) to start
    }

    // Ramène un timestamp au début du jour (minuit) pour regrouper les ventes par jour
    private fun startOfDay(epoch: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epoch
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
