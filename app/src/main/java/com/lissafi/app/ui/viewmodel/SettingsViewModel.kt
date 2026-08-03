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
    val isPremium: Boolean = false,
    val premiumExpiry: Long? = null,
    val premiumExpiryText: String = "",
    val productCount: Int = 0,
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
            val premium = repository.isPremium()
            val expiry = repository.getPremiumExpiry()

            _state.value = _state.value.copy(
                shopName = shopName,
                shopPhone = shopPhone,
                isPremium = premium,
                premiumExpiry = expiry,
                premiumExpiryText = if (expiry != null) FormatUtils.formatDate(expiry) else ""
            )
        }
    }

    fun saveShopInfo(name: String, phone: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            repository.setSetting("shop_name", name)
            repository.setSetting("shop_phone", phone)
            _state.value = _state.value.copy(
                shopName = name,
                shopPhone = phone,
                isSaving = false
            )
        }
    }
}
