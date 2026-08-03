package com.lissafi.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lissafi.app.data.entity.Client
import com.lissafi.app.data.entity.DebtTransaction
import com.lissafi.app.data.repository.LissafiRepository
import com.lissafi.app.service.PremiumManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ClientListState(
    val clients: List<Client> = emptyList(),
    val searchQuery: String = "",
    val isPremium: Boolean = false,
    val isLimitReached: Boolean = false
)

class ClientViewModel(
    private val repository: LissafiRepository,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val _state = MutableStateFlow(ClientListState())
    val state: StateFlow<ClientListState> = _state.asStateFlow()

    private val _transactions = MutableStateFlow<List<DebtTransaction>>(emptyList())
    val transactions: StateFlow<List<DebtTransaction>> = _transactions.asStateFlow()

    private val _selectedClient = MutableStateFlow<Client?>(null)
    val selectedClient: StateFlow<Client?> = _selectedClient.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(isPremium = premiumManager.isPremium())
            repository.getAllClients().collect { clients ->
                _state.value = _state.value.copy(clients = clients)
            }
        }
    }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        viewModelScope.launch {
            if (query.isBlank()) {
                repository.getAllClients().collect { clients ->
                    _state.value = _state.value.copy(clients = clients)
                }
            } else {
                repository.searchClients(query).collect { clients ->
                    _state.value = _state.value.copy(clients = clients)
                }
            }
        }
    }

    suspend fun canAddClient(): Boolean {
        val can = premiumManager.canAddClient()
        _state.value = _state.value.copy(isLimitReached = !can)
        return can
    }

    fun addClient(name: String, phone: String = "") {
        viewModelScope.launch {
            val client = Client(
                id = UUID.randomUUID().toString(),
                name = name,
                phone = phone,
                totalDebt = 0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.upsertClient(client)
        }
    }

    fun loadClient(clientId: String) {
        viewModelScope.launch {
            val client = repository.getClient(clientId)
            _selectedClient.value = client
            repository.getDebtTransactions(clientId).collect { txns ->
                _transactions.value = txns
            }
        }
    }

    fun addDebt(clientId: String, amount: Int, note: String) {
        viewModelScope.launch {
            val txn = DebtTransaction(
                clientId = clientId,
                amount = amount,
                date = System.currentTimeMillis(),
                note = note
            )
            repository.addDebtTransaction(txn)
        }
    }

    fun addRepayment(clientId: String, amount: Int, note: String = "Remboursement") {
        viewModelScope.launch {
            val txn = DebtTransaction(
                clientId = clientId,
                amount = -amount,
                date = System.currentTimeMillis(),
                note = note
            )
            repository.addDebtTransaction(txn)
        }
    }
}
