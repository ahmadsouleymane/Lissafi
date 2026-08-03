package com.lissafi.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lissafi.app.data.dao.TopProduct
import com.lissafi.app.data.repository.LissafiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReportPeriod { TODAY, WEEK, MONTH }

data class ReportState(
    val period: ReportPeriod = ReportPeriod.TODAY,
    val totalVentes: Int = 0,
    val totalCredits: Int = 0,
    val totalComptant: Int = 0,
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

            _state.value = _state.value.copy(
                totalVentes = total,
                totalCredits = credit,
                totalComptant = total - credit,
                nbTransactions = count,
                panierMoyen = if (count > 0) total / count else 0,
                topProducts = top,
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
