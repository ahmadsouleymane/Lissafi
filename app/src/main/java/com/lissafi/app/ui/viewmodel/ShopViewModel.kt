package com.lissafi.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lissafi.app.LissafiApp
import com.lissafi.app.data.remote.ShopMemberDto
import com.lissafi.app.data.remote.SupabaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * État de l'écran « Mes caisses » (offre Grand boutique).
 * - `role`     : "patron" (par défaut / boutique solo) ou "vendeur" (caisse rattachée)
 * - `members`  : caisses de MA boutique (visible pour le patron uniquement)
 * - `pairingCode` : dernier code d'appairage généré, à montrer/partager
 */
data class ShopUiState(
    val role: String = "patron",
    val members: List<ShopMemberDto> = emptyList(),
    val pairingCode: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

class ShopViewModel(private val app: LissafiApp) : ViewModel() {

    private val api get() = app.supabaseApi

    private val _state = MutableStateFlow(ShopUiState())
    val state: StateFlow<ShopUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val role = SupabaseManager.currentShopRole(app)
            _state.value = _state.value.copy(role = role, loading = true, error = null)
            val members = if (role == "patron") api.myShopMembers() else emptyList()
            _state.value = _state.value.copy(members = members, loading = false)
        }
    }

    /** Patron : génère un code d'appairage à communiquer à la nouvelle caisse. */
    fun generatePairingCode() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, info = null)
            try {
                val code = api.createPairingCode()
                _state.value = _state.value.copy(pairingCode = code, loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = mapError(e.message))
            }
        }
    }

    /** N'importe quel compte : rejoint la boutique d'un patron via son code (devient vendeur). */
    fun joinWithCode(code: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, info = null)
            try {
                val result = api.joinShopWithCode(code)
                SupabaseManager.setShop(app, result.shopId, result.role)
                // Tire tout le dataset de la boutique rejointe.
                app.syncManager.syncInBackground()
                _state.value = _state.value.copy(
                    loading = false,
                    role = result.role,
                    pairingCode = null,
                    info = "Caisse rattachée à la boutique."
                )
                onDone()
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = mapError(e.message))
            }
        }
    }

    /** Patron : détache une caisse vendeur. */
    fun removeMember(memberId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                api.removeShopMember(memberId)
                refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = mapError(e.message))
            }
        }
    }

    /** Vendeur : quitte la boutique et retrouve sa boutique solo. */
    fun leaveShop(onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val newShop = api.leaveShop()
            val userId = SupabaseManager.currentUserId(app) ?: ""
            SupabaseManager.setShop(app, newShop ?: userId, "patron")
            app.syncManager.syncInBackground()
            _state.value = _state.value.copy(loading = false, role = "patron", info = "Tu as quitté la boutique.")
            onDone()
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, info = null)
    }

    /** Traduit les codes d'erreur serveur en messages lisibles. */
    private fun mapError(raw: String?): String = when {
        raw == null -> "Une erreur est survenue."
        raw.contains("quota_caisses_atteint") -> "Quota de caisses atteint. Passe à l'offre Grand boutique pour en ajouter."
        raw.contains("code_invalide") -> "Code invalide."
        raw.contains("code_expire") -> "Code expiré. Demande-en un nouveau."
        raw.contains("code_deja_utilise") -> "Ce code a déjà été utilisé."
        raw.contains("not_patron") -> "Seul le patron peut gérer les caisses."
        raw.contains("not_authenticated") -> "Connecte-toi d'abord."
        else -> "Erreur réseau. Réessaie."
    }
}
