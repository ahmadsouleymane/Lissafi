package com.lissafi.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lissafi.app.data.LissafiDatabase.TopProduct
import com.lissafi.app.data.repository.LissafiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

            // Calcul du bénéfice estimé (somme des ventes - somme des prix d'achat)
            var profit = 0
            val sales = repository.getSalesBetween(start, end)

            // Série temporelle du CA par jour (pour la sparkline de l'écran Activité).
            // La liste des ventes est déjà en mémoire pour le calcul du profit — pas de requête en plus.
            val revenueSeries = sales
                .groupBy { startOfDay(it.date) }
                .map { (day, daySales) -> RevenuePoint(day, daySales.sumOf { it.total }) }
                .sortedBy { it.date }

            for (sale in sales) {
                if (!sale.isCredit || sale.amountPaid > 0) {
                    val items = repository.getSaleItems(sale.id)
                    for (item in items) {
                        val product = repository.getProduct(item.barcode)
                        if (product != null && product.buyPrice > 0) {
                            profit += (item.price - product.buyPrice) * item.quantity.toInt()
                        } else {
                            // Pas de prix d'achat connu → on compte le prix de vente comme bénéfice
                            profit += (item.price * item.quantity).toInt()
                        }
                    }
                }
            }

            _state.value = _state.value.copy(
                totalVentes = total,
                totalCredits = credit,
                totalComptant = total - credit,
                estimatedProfit = profit,
                nbTransactions = count,
                panierMoyen = if (count > 0) total / count else 0,
                topProducts = top,
                revenueSeries = revenueSeries,
                isLoading = false
            )
        }
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
