package com.lissafi.app.ui.viewmodel

import android.util.Log
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

private const val TAG = "ClientViewModel"

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
            try {
                _state.value = _state.value.copy(isPremium = premiumManager.isPremium())
                repository.clientsFlow.collect { clients ->
                    _state.value = _state.value.copy(clients = clients)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Échec chargement clients", e)
            }
        }
    }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        viewModelScope.launch {
            try {
                val results = if (query.isBlank()) {
                    repository.getAllClients()
                } else {
                    repository.searchClients(query)
                }
                _state.value = _state.value.copy(clients = results)
            } catch (e: Exception) {
                Log.w(TAG, "Échec recherche clients", e)
            }
        }
    }

    suspend fun canAddClient(): Boolean {
        val can = try {
            premiumManager.canAddClient()
        } catch (e: Exception) {
            Log.w(TAG, "Échec vérification limite clients", e)
            true
        }
        _state.value = _state.value.copy(isLimitReached = !can)
        return can
    }

    fun addClient(name: String, phone: String = "") {
        viewModelScope.launch {
            try {
                // Re-vérifie la limite au moment de l'écriture (course TOCTOU).
                if (!premiumManager.canAddClient()) {
                    _state.value = _state.value.copy(isLimitReached = true)
                    return@launch
                }
                val client = Client(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    phone = phone,
                    totalDebt = 0,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                repository.upsertClient(client)
            } catch (e: Exception) {
                Log.w(TAG, "Échec ajout client", e)
            }
        }
    }

    fun loadClient(clientId: String) {
        viewModelScope.launch {
            try {
                val client = repository.getClient(clientId)
                _selectedClient.value = client
                val txns = repository.getDebtTransactions(clientId)
                _transactions.value = txns
            } catch (e: Exception) {
                Log.w(TAG, "Échec chargement client", e)
            }
        }
    }

    fun addDebt(clientId: String, amount: Int, note: String) {
        viewModelScope.launch {
            try {
                val txn = DebtTransaction(
                    clientId = clientId,
                    amount = amount,
                    date = System.currentTimeMillis(),
                    note = note
                )
                repository.addDebtTransaction(txn)
                loadClient(clientId)
            } catch (e: Exception) {
                Log.w(TAG, "Échec ajout dette", e)
            }
        }
    }

    fun addRepayment(clientId: String, amount: Int, note: String = "Remboursement") {
        viewModelScope.launch {
            try {
                val txn = DebtTransaction(
                    clientId = clientId,
                    amount = -amount,
                    date = System.currentTimeMillis(),
                    note = note
                )
                repository.addDebtTransaction(txn)
                loadClient(clientId)
            } catch (e: Exception) {
                Log.w(TAG, "Échec ajout remboursement", e)
            }
        }
    }
}
