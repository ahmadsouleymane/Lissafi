package com.lissafi.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lissafi.app.data.entity.Client
import com.lissafi.app.data.entity.Product
import com.lissafi.app.data.entity.Sale
import com.lissafi.app.data.entity.SaleItem
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

class CartViewModel(
    private val repository: LissafiRepository,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val _state = MutableStateFlow(CartState())
    val state: StateFlow<CartState> = _state.asStateFlow()

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
            currentItems.add(
                CartItem(
                    barcode = product.barcode,
                    name = product.name,
                    price = product.sellPrice,
                    quantity = 1.0
                )
            )
        }
        _state.value = _state.value.copy(items = currentItems, total = computeTotal(currentItems))
    }

    fun addProductDirectly(product: Product) {
        viewModelScope.launch {
            addToCart(product)
            repository.upsertProduct(product)
            loadRecentProducts()
        }
    }

    fun updateQuantity(index: Int, quantity: Double) {
        val items = _state.value.items.toMutableList()
        if (index in items.indices) {
            if (quantity <= 0) {
                items.removeAt(index)
            } else {
                items[index] = items[index].copy(quantity = quantity)
            }
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
            items = emptyList(),
            total = 0,
            isCredit = false,
            selectedClient = null
        )
    }

    suspend fun encaisser(amountPaid: Int): Long {
        val state = _state.value
        val sale = Sale(
            date = System.currentTimeMillis(),
            total = state.total,
            amountPaid = if (state.isCredit) state.total else amountPaid,
            changeGiven = if (state.isCredit) 0 else amountPaid - state.total,
            isCredit = state.isCredit,
            clientId = state.selectedClient?.id
        )
        val items = state.items.map { item ->
            SaleItem(
                barcode = item.barcode,
                name = item.name,
                price = item.price,
                quantity = item.quantity,
                saleId = 0
            )
        }
        return repository.insertSale(sale, items)
    }

    private fun computeTotal(items: List<CartItem>): Int {
        return items.sumOf { (it.price * it.quantity).toInt() }
    }
}
