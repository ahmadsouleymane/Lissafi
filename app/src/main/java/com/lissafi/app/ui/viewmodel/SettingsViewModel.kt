package com.lissafi.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lissafi.app.data.repository.LissafiRepository
import com.lissafi.app.service.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val shopName: String = "",
    val shopPhone: String = "",
    val receiptFooterMessage: String = "",
    val adminPin: String = "0000",
    val isPremium: Boolean = false,
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
            val shopName = repository.getShopName()
            val shopPhone = repository.getShopPhone()
            val receiptFooterMessage = repository.getSetting("receipt_footer_message") ?: ""
            val adminPin = repository.getAdminPin()
            val premium = repository.isPremium()
            val expiry = repository.getPremiumExpiry()
            val productCount = repository.getProductCount()
            val clientCount = repository.getClientCount()

            _state.value = _state.value.copy(
                shopName = shopName,
                shopPhone = shopPhone,
                receiptFooterMessage = receiptFooterMessage,
                adminPin = adminPin,
                isPremium = premium,
                premiumExpiry = expiry,
                premiumExpiryText = if (expiry != null) FormatUtils.formatDate(expiry) else "",
                productCount = productCount,
                clientCount = clientCount
            )
        }
    }

    fun saveShopInfo(name: String, phone: String, footerMessage: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            repository.setSetting("shop_name", name)
            repository.setSetting("shop_phone", phone)
            repository.setSetting("receipt_footer_message", footerMessage)
            _state.value = _state.value.copy(
                shopName = name,
                shopPhone = phone,
                receiptFooterMessage = footerMessage,
                isSaving = false
            )
        }
    }

    fun changeAdminPin(newPin: String): Boolean {
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) return false
        viewModelScope.launch {
            repository.setSetting("admin_pin", newPin)
            _state.value = _state.value.copy(adminPin = newPin)
        }
        return true
    }
}
