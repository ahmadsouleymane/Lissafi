package com.lissafi.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lissafi.app.data.entity.Product
import com.lissafi.app.data.repository.LissafiRepository
import com.lissafi.app.service.PremiumManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductListState(
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val isPremium: Boolean = false,
    val isLimitReached: Boolean = false
)

class ProductViewModel(
    private val repository: LissafiRepository,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val _state = MutableStateFlow(ProductListState())
    val state: StateFlow<ProductListState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(isPremium = premiumManager.isPremium())
        }
        viewModelScope.launch {
            repository.getAllProducts().collect { products ->
                _state.value = _state.value.copy(products = products)
            }
        }
    }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        viewModelScope.launch {
            if (query.isBlank()) {
                repository.getAllProducts().collect { products ->
                    _state.value = _state.value.copy(products = products)
                }
            } else {
                repository.searchProducts(query).collect { products ->
                    _state.value = _state.value.copy(products = products)
                }
            }
        }
    }

    suspend fun canAddProduct(): Boolean {
        val can = premiumManager.canAddProduct()
        _state.value = _state.value.copy(isLimitReached = !can)
        return can
    }

    fun addProduct(product: Product) {
        viewModelScope.launch {
            repository.upsertProduct(product)
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.upsertProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }
}
