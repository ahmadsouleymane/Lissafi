package com.lissafi.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lissafi.app.data.repository.LissafiRepository
import com.lissafi.app.service.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "SettingsViewModel"

data class SettingsState(
    val shopName: String = "",
    val shopPhone: String = "",
    val receiptFooterMessage: String = "",
    val isPremium: Boolean = false,
    val plan: String = "free", // "free" | "plus" | "business"
    val premiumExpiry: Long? = null,
    val premiumExpiryText: String = "",
    val productCount: Int = 0,
    val clientCount: Int = 0,
    val isSaving: Boolean = false
)

class SettingsViewModel(private val repository: LissafiRepository) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            try {
                val shopName = repository.getShopName()
                val shopPhone = repository.getShopPhone()
                val receiptFooterMessage = repository.getSetting("receipt_footer_message") ?: ""
                val premium = repository.isPremium()
                val plan = repository.getSetting("plan") ?: if (premium) "plus" else "free"
                val expiry = repository.getPremiumExpiry()
                val productCount = repository.getProductCount()
                val clientCount = repository.getClientCount()

                _state.value = _state.value.copy(
                    shopName = shopName,
                    shopPhone = shopPhone,
                    receiptFooterMessage = receiptFooterMessage,
                    isPremium = premium,
                    plan = plan,
                    premiumExpiry = expiry,
                    premiumExpiryText = if (expiry != null) FormatUtils.formatDate(expiry) else "",
                    productCount = productCount,
                    clientCount = clientCount
                )
            } catch (e: Exception) {
                Log.w(TAG, "Échec chargement réglages", e)
            }
        }
    }

    fun saveShopInfo(name: String, phone: String, footerMessage: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                repository.setSetting("shop_name", name)
                repository.setSetting("shop_phone", phone)
                repository.setSetting("receipt_footer_message", footerMessage)
                _state.value = _state.value.copy(
                    shopName = name,
                    shopPhone = phone,
                    receiptFooterMessage = footerMessage,
                    isSaving = false
                )
            } catch (e: Exception) {
                Log.w(TAG, "Échec sauvegarde infos boutique", e)
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }
}
