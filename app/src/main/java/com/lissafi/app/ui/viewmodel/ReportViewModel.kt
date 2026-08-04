package com.lissafi.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lissafi.app.data.LissafiDatabase
import com.lissafi.app.data.repository.LissafiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReportPeriod { TODAY, WEEK, MONTH }

data class TopProduct(
    val name: String,
    val totalQty: Double,
    val count: Int
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
                topProducts = top.map { TopProduct(it.name, it.totalQty, it.count) },
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
}
