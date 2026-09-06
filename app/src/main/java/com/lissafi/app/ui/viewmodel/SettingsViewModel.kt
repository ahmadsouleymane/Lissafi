package com.lissafi.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lissafi.app.data.repository.LissafiRepository
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.service.PremiumManager
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
    val plan: String = "trial", // "trial" | "plus" | "business" | "locked"
    val trialDaysLeft: Int = 0,
    val premiumExpiry: Long? = null,
    val premiumExpiryText: String = "",
    val productCount: Int = 0,
    val clientCount: Int = 0,
    val hasManagerPin: Boolean = false,
    val isSaving: Boolean = false
)

class SettingsViewModel(
    private val repository: LissafiRepository,
    private val premiumManager: PremiumManager
) : ViewModel() {

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
                val planKind = when (premiumManager.getPlan()) {
                    PremiumManager.Plan.TRIAL -> "trial"
                    PremiumManager.Plan.PLUS -> "plus"
                    PremiumManager.Plan.BUSINESS -> "business"
                    PremiumManager.Plan.LOCKED -> "locked"
                }
                val trialDaysLeft = premiumManager.trialDaysLeft()
                val expiry = repository.getPremiumExpiry()
                val productCount = repository.getProductCount()
                val clientCount = repository.getClientCount()
                val hasManagerPin = repository.hasManagerPin()

                _state.value = _state.value.copy(
                    shopName = shopName,
                    shopPhone = shopPhone,
                    receiptFooterMessage = receiptFooterMessage,
                    isPremium = premium,
                    plan = planKind,
                    trialDaysLeft = trialDaysLeft,
                    premiumExpiry = expiry,
                    premiumExpiryText = if (expiry != null) FormatUtils.formatDate(expiry) else "",
                    productCount = productCount,
                    clientCount = clientCount,
                    hasManagerPin = hasManagerPin
                )
            } catch (e: Exception) {
                Log.w(TAG, "Échec chargement réglages", e)
            }
        }
    }

    /** Crée un PIN gérant (première activation du verrou). */
    fun setManagerPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.setManagerPin(pin)
                _state.value = _state.value.copy(hasManagerPin = true)
                onResult(true)
            } catch (e: Exception) {
                Log.w(TAG, "Échec création PIN", e)
                onResult(false)
            }
        }
    }

    /** Change le PIN gérant : exige le code actuel correct. */
    fun changeManagerPin(current: String, newPin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                if (!repository.verifyManagerPin(current)) { onResult(false); return@launch }
                repository.setManagerPin(newPin)
                _state.value = _state.value.copy(hasManagerPin = true)
                onResult(true)
            } catch (e: Exception) {
                Log.w(TAG, "Échec changement PIN", e)
                onResult(false)
            }
        }
    }

    /** Désactive le verrou : exige le code actuel correct. */
    fun disableManagerPin(current: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                if (!repository.verifyManagerPin(current)) { onResult(false); return@launch }
                repository.clearManagerPin()
                _state.value = _state.value.copy(hasManagerPin = false)
                onResult(true)
            } catch (e: Exception) {
                Log.w(TAG, "Échec désactivation PIN", e)
                onResult(false)
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
