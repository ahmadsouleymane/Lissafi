package com.lissafi.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lissafi.app.data.entity.Product
import com.lissafi.app.data.repository.LissafiRepository
import com.lissafi.app.service.PremiumManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "ProductViewModel"

data class ProductListState(
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val isPremium: Boolean = false,
    val isLimitReached: Boolean = false,
    val isSearching: Boolean = false
)

class ProductViewModel(
    private val repository: LissafiRepository,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val _state = MutableStateFlow(ProductListState())
    val state: StateFlow<ProductListState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isPremium = premiumManager.isPremium())
            } catch (e: Exception) {
                Log.w(TAG, "Échec lecture statut premium", e)
            }
        }
        viewModelScope.launch {
            try {
                repository.productsFlow.collect { products ->
                    // Ne pas écraser si l'utilisateur est en train de chercher
                    if (!_state.value.isSearching) {
                        _state.value = _state.value.copy(products = products)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Échec collecte des produits", e)
            }
        }
    }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query, isSearching = query.isNotBlank())
        viewModelScope.launch {
            try {
                val results = if (query.isBlank()) {
                    repository.getAllProducts()
                } else {
                    repository.searchProducts(query)
                }
                _state.value = _state.value.copy(products = results, isSearching = query.isNotBlank())
            } catch (e: Exception) {
                Log.w(TAG, "Échec recherche produits", e)
                _state.value = _state.value.copy(isSearching = false)
            }
        }
    }

    suspend fun canAddProduct(): Boolean {
        val can = try {
            premiumManager.canAddProduct()
        } catch (e: Exception) {
            Log.w(TAG, "Échec vérification limite produits", e)
            true
        }
        _state.value = _state.value.copy(isLimitReached = !can)
        return can
    }

    fun addProduct(product: Product) {
        viewModelScope.launch {
            try {
                // La limite ne concerne que les NOUVEAUX produits : modifier un produit
                // existant (prix, stock, catégorie) doit rester possible même quand le
                // catalogue a atteint la limite du plan — sinon la sauvegarde d'une
                // simple correction serait silencieusement bloquée.
                val isNew = repository.getProduct(product.barcode) == null
                // Re-vérifie la limite au moment de l'écriture (course TOCTOU : deux
                // clics rapides ne doivent pas dépasser la limite du plan).
                if (isNew && !premiumManager.canAddProduct()) {
                    _state.value = _state.value.copy(isLimitReached = true)
                    return@launch
                }
                repository.upsertProduct(product)
            } catch (e: Exception) {
                Log.w(TAG, "Échec ajout produit", e)
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(product)
            } catch (e: Exception) {
                Log.w(TAG, "Échec suppression produit", e)
            }
        }
    }
}
