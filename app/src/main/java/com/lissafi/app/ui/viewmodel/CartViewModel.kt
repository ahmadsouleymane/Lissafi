package com.lissafi.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lissafi.app.data.entity.*
import com.lissafi.app.data.repository.LissafiRepository
import com.lissafi.app.service.PremiumManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CartItem(
    val barcode: String,
    val name: String,
    val price: Int,
    var quantity: Double
)

data class CartState(
    val items: List<CartItem> = emptyList(),
    val isCredit: Boolean = false,
    val selectedClient: Client? = null,
    val recentProducts: List<Product> = emptyList(),
    val isPremium: Boolean = false,
    val total: Int = 0
)

data class LastSale(
    val items: List<CartItem>,
    val total: Int,
    val amountPaid: Int,
    val changeGiven: Int,
    val isCredit: Boolean,
    val date: Long
)

class CartViewModel(
    private val repository: LissafiRepository,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val _state = MutableStateFlow(CartState())
    val state: StateFlow<CartState> = _state.asStateFlow()

    private val _lastSaleChange = MutableStateFlow(0)
    val lastSaleChange: StateFlow<Int> = _lastSaleChange.asStateFlow()

    private val _scanResult = MutableStateFlow<String?>(null)
    val scanResult: StateFlow<String?> = _scanResult.asStateFlow()

    private val _lastSale = MutableStateFlow<LastSale?>(null)
    val lastSale: StateFlow<LastSale?> = _lastSale.asStateFlow()

    fun clearScanResult() { _scanResult.value = null }
    fun clearLastSale() { _lastSale.value = null }

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(isPremium = premiumManager.isPremium())
            loadRecentProducts()
        }
    }

    private suspend fun loadRecentProducts() {
        _state.value = _state.value.copy(recentProducts = repository.getRecentProducts(8))
    }

    fun scanProduct(barcode: String) {
        viewModelScope.launch {
            val product = repository.getProduct(barcode)
            if (product != null) {
                addToCart(product)
                _scanResult.value = "OK:${product.name}"
            } else {
                _scanResult.value = "NOT_FOUND:$barcode"
            }
        }
    }

    private fun addToCart(product: Product) {
        val currentItems = _state.value.items.toMutableList()
        val existing = currentItems.find { it.barcode == product.barcode }
        if (existing != null) {
            val index = currentItems.indexOf(existing)
            currentItems[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            currentItems.add(CartItem(product.barcode, product.name, product.sellPrice, 1.0))
        }
        _state.value = _state.value.copy(items = currentItems, total = computeTotal(currentItems))
    }

    fun addProductDirectly(product: Product) {
        viewModelScope.launch {
            // Vérifier la limite gratuite AVANT d'ajouter (bug corrigé)
            if (!premiumManager.isPremium() && !premiumManager.canAddProduct()) {
                return@launch // Limite atteinte, ignoré
            }
            addToCart(product)
            repository.upsertProduct(product)
            loadRecentProducts()
        }
    }

    fun updateQuantity(index: Int, quantity: Double) {
        val items = _state.value.items.toMutableList()
        if (index in items.indices) {
            if (quantity <= 0) items.removeAt(index)
            else items[index] = items[index].copy(quantity = quantity)
            _state.value = _state.value.copy(items = items, total = computeTotal(items))
        }
    }

    fun removeItem(index: Int) {
        val items = _state.value.items.toMutableList()
        if (index in items.indices) {
            items.removeAt(index)
            _state.value = _state.value.copy(items = items, total = computeTotal(items))
        }
    }

    fun setCreditMode(isCredit: Boolean) {
        _state.value = _state.value.copy(isCredit = isCredit)
    }

    fun selectClient(client: Client?) {
        _state.value = _state.value.copy(selectedClient = client)
    }

    fun clearCart() {
        _state.value = _state.value.copy(
            items = emptyList(), total = 0,
            isCredit = false, selectedClient = null
        )
    }

    /**
     * Encaisser une vente.
     * - Comptant : amountPaid = montant donné par le client.
     * - Crédit : amountPaid = 0, une DebtTransaction est créée.
     *
     * Retourne true si la vente est réussie.
     */
    suspend fun encaisser(amountPaid: Int): Boolean {
        val s = _state.value
        val sale = Sale(
            date = System.currentTimeMillis(),
            total = s.total,
            amountPaid = if (s.isCredit) s.total else amountPaid,
            changeGiven = if (s.isCredit) 0 else amountPaid - s.total,
            isCredit = s.isCredit,
            clientId = s.selectedClient?.id
        )
        val items = s.items.map { item ->
            SaleItem(barcode = item.barcode, name = item.name, price = item.price, quantity = item.quantity, saleId = 0)
        }

        val saleId = repository.insertSale(sale, items)

        // Décrémenter le stock pour chaque produit vendu
        for (item in s.items) {
            try {
                val product = repository.getProduct(item.barcode)
                if (product != null && product.stock > 0) {
                    repository.upsertProduct(product.copy(
                        stock = maxOf(0, product.stock - item.quantity.toInt()),
                        updatedAt = System.currentTimeMillis()
                    ))
                }
            } catch (_: Exception) {}
        }

        // Crédit → créer la transaction de dette
        if (s.isCredit && s.selectedClient != null) {
            val debtTxn = DebtTransaction(
                clientId = s.selectedClient!!.id,
                saleId = saleId,
                amount = s.total,
                date = System.currentTimeMillis(),
                note = "Vente N°$saleId"
            )
            repository.addDebtTransaction(debtTxn)
        }

        _lastSaleChange.value = if (s.isCredit) 0 else amountPaid - s.total
        _lastSale.value = LastSale(
            items = s.items.toList(),
            total = s.total,
            amountPaid = if (s.isCredit) 0 else amountPaid,
            changeGiven = if (s.isCredit) 0 else amountPaid - s.total,
            isCredit = s.isCredit,
            date = System.currentTimeMillis()
        )
        return true
    }

    private fun computeTotal(items: List<CartItem>): Int =
        items.sumOf { (it.price * it.quantity).toInt() }
}
