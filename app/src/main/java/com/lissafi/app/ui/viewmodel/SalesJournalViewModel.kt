package com.lissafi.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lissafi.app.data.entity.Client
import com.lissafi.app.data.entity.Sale
import com.lissafi.app.data.entity.SaleAuditEntry
import com.lissafi.app.data.entity.SaleItem
import com.lissafi.app.data.repository.LissafiRepository
import com.lissafi.app.service.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val TAG = "SalesJournalVM"

data class SalesJournalState(
    val sales: List<Sale> = emptyList(),
    val isLoading: Boolean = true
)

/** Détail d'une vente ouvert dans le journal : articles éditables + historique. */
data class SaleDetail(
    val sale: Sale,
    val items: List<CartItem>,
    val audit: List<SaleAuditEntry>,
    val clientName: String?
)

class SalesJournalViewModel(
    private val repository: LissafiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SalesJournalState())
    val state: StateFlow<SalesJournalState> = _state.asStateFlow()

    private val _detail = MutableStateFlow<SaleDetail?>(null)
    val detail: StateFlow<SaleDetail?> = _detail.asStateFlow()

    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    val clients: StateFlow<List<Client>> = _clients.asStateFlow()

    // Verrou gérant : un PIN est-il exigé avant de modifier/annuler une vente ?
    private val _pinRequired = MutableStateFlow(false)
    val pinRequired: StateFlow<Boolean> = _pinRequired.asStateFlow()

    init {
        loadJournal()
        viewModelScope.launch {
            try { _clients.value = repository.getAllClients() } catch (e: Exception) { Log.w(TAG, "clients", e) }
        }
        viewModelScope.launch {
            try { _pinRequired.value = repository.hasManagerPin() } catch (e: Exception) { Log.w(TAG, "pin", e) }
        }
    }

    /** Vérifie le PIN gérant (asynchrone) ; renvoie le résultat via callback. */
    fun verifyPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = try { repository.verifyManagerPin(pin) } catch (e: Exception) { false }
            onResult(ok)
        }
    }

    fun loadJournal() {
        _state.value = _state.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                _state.value = SalesJournalState(sales = repository.getSalesJournal(), isLoading = false)
            } catch (e: Exception) {
                Log.w(TAG, "Échec chargement journal", e)
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun openSale(saleId: Long) {
        viewModelScope.launch {
            try {
                val sale = repository.getSaleById(saleId) ?: return@launch
                val items = repository.getSaleItems(saleId).map {
                    CartItem(barcode = it.barcode, name = it.name, price = it.price, quantity = it.quantity)
                }
                val audit = repository.getSaleAudit(saleId)
                val clientName = sale.clientId?.let { id -> _clients.value.find { it.id == id }?.name }
                _detail.value = SaleDetail(sale, items, audit, clientName)
            } catch (e: Exception) {
                Log.w(TAG, "Échec ouverture vente", e)
            }
        }
    }

    fun clearSelection() { _detail.value = null }

    /**
     * Enregistre la modification de la vente actuellement ouverte.
     * `newItems` : panier édité. `isCredit`/`client` : nouveau mode de paiement.
     */
    fun saveModification(newItems: List<CartItem>, isCredit: Boolean, client: Client?) {
        val current = _detail.value ?: return
        val old = current.sale
        viewModelScope.launch {
            try {
                val oldItems = repository.getSaleItems(old.id)
                val newTotal = newItems.sumOf { (it.price * it.quantity).roundToInt() }
                val newSale = old.copy(
                    total = newTotal,
                    isCredit = isCredit,
                    clientId = if (isCredit) client?.id else null,
                    amountPaid = if (isCredit) 0 else newTotal,
                    changeGiven = 0
                )
                val newSaleItems = newItems.map {
                    SaleItem(saleId = old.id, barcode = it.barcode, name = it.name, price = it.price, quantity = it.quantity)
                }
                repository.modifySale(old, oldItems, newSale, newSaleItems, buildAuditDetails(old, oldItems, newSale, newSaleItems))
                _detail.value = null
                loadJournal()
            } catch (e: Exception) {
                Log.w(TAG, "Échec modification vente", e)
            }
        }
    }

    /** Annule (soft) la vente ouverte, avec un motif obligatoire. */
    fun cancelSale(reason: String) {
        val current = _detail.value ?: return
        val sale = current.sale
        viewModelScope.launch {
            try {
                val items = repository.getSaleItems(sale.id)
                repository.cancelSale(sale, items, reason.trim())
                _detail.value = null
                loadJournal()
            } catch (e: Exception) {
                Log.w(TAG, "Échec annulation vente", e)
            }
        }
    }

    private fun buildAuditDetails(oldSale: Sale, oldItems: List<SaleItem>, newSale: Sale, newItems: List<SaleItem>): String {
        val parts = mutableListOf<String>()
        if (oldSale.total != newSale.total) {
            parts.add("total ${FormatUtils.formatFCFA(oldSale.total)} → ${FormatUtils.formatFCFA(newSale.total)}")
        }
        if (oldSale.isCredit != newSale.isCredit) {
            parts.add(if (newSale.isCredit) "passée en crédit" else "passée en comptant")
        }
        if (itemsChanged(oldItems, newItems)) parts.add("articles modifiés")
        return "Modifiée · " + (if (parts.isEmpty()) "sans changement de total" else parts.joinToString(", "))
    }

    private fun itemsChanged(a: List<SaleItem>, b: List<SaleItem>): Boolean {
        if (a.size != b.size) return true
        val ma = a.associate { it.barcode to it.quantity }
        val mb = b.associate { it.barcode to it.quantity }
        return ma != mb
    }
}
