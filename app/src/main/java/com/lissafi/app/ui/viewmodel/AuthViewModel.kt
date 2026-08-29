package com.lissafi.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lissafi.app.data.auth.AuthManager
import com.lissafi.app.data.auth.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Modes d'affichage de l'écran d'authentification.
 */
enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
    RESET_PASSWORD
}

/**
 * État de l'écran d'auth.
 */
data class AuthState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val shopName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val market: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val isLoggedIn: Boolean = false
)

class AuthViewModel(
    private val authManager: AuthManager,
    private val onProfileCollected: (shopName: String, ownerName: String, phone: String, market: String) -> Unit = { _, _, _, _ -> }
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        _state.value = _state.value.copy(isLoggedIn = authManager.isLoggedIn())
    }

    fun setMode(mode: AuthMode) {
        _state.value = _state.value.copy(mode = mode, message = null, isError = false)
    }

    fun setEmail(email: String) {
        _state.value = _state.value.copy(email = email)
    }

    fun setPassword(password: String) {
        _state.value = _state.value.copy(password = password)
    }

    fun setConfirmPassword(password: String) {
        _state.value = _state.value.copy(confirmPassword = password)
    }

    fun setShopName(name: String) {
        _state.value = _state.value.copy(shopName = name)
    }

    fun setOwnerName(name: String) {
        _state.value = _state.value.copy(ownerName = name)
    }

    fun setPhone(phone: String) {
        _state.value = _state.value.copy(phone = phone)
    }

    fun setMarket(market: String) {
        _state.value = _state.value.copy(market = market)
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null, isError = false)
    }

    fun signUp() {
        val state = _state.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _state.value = state.copy(message = "Remplis tous les champs.", isError = true)
            return
        }
        if (state.password.length < 8) {
            _state.value = state.copy(message = "Le mot de passe doit faire au moins 8 caractères.", isError = true)
            return
        }
        if (state.password != state.confirmPassword) {
            _state.value = state.copy(message = "Les mots de passe ne correspondent pas.", isError = true)
            return
        }
        if (state.phone.count { it.isDigit() } < 8) {
            _state.value = state.copy(message = "Ton numéro WhatsApp est nécessaire (au moins 8 chiffres).", isError = true)
            return
        }

        _state.value = state.copy(isLoading = true, message = null)
        viewModelScope.launch {
            val result = authManager.signUp(
                state.email, state.password, state.shopName, state.ownerName, state.phone, state.market
            )
            when (result) {
                is AuthResult.Success -> {
                    onProfileCollected(state.shopName, state.ownerName, state.phone, state.market)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        message = result.message,
                        isError = false,
                        isLoggedIn = authManager.isLoggedIn()
                    )
                }
                is AuthResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        message = result.message,
                        isError = true
                    )
                }
            }
        }
    }

    /** Échange l'id_token Google (récupéré via Credential Manager) contre une session. */
    fun signInWithGoogle(idToken: String) {
        _state.value = _state.value.copy(isLoading = true, message = null)
        viewModelScope.launch {
            when (val result = authManager.signInWithGoogle(idToken)) {
                is AuthResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        message = result.message,
                        isError = false,
                        isLoggedIn = true
                    )
                }
                is AuthResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        message = result.message,
                        isError = true
                    )
                }
            }
        }
    }

    fun signIn() {
        val state = _state.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _state.value = state.copy(message = "Remplis tous les champs.", isError = true)
            return
        }

        _state.value = state.copy(isLoading = true, message = null)
        viewModelScope.launch {
            val result = authManager.signIn(state.email, state.password)
            when (result) {
                is AuthResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        message = result.message,
                        isError = false,
                        isLoggedIn = true
                    )
                }
                is AuthResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        message = result.message,
                        isError = true
                    )
                }
            }
        }
    }

    fun resetPassword() {
        val state = _state.value
        if (state.email.isBlank()) {
            _state.value = state.copy(message = "Entre ton adresse email.", isError = true)
            return
        }

        _state.value = state.copy(isLoading = true, message = null)
        viewModelScope.launch {
            val result = authManager.resetPassword(state.email)
            when (result) {
                is AuthResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        message = result.message,
                        isError = false
                    )
                }
                is AuthResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        message = result.message,
                        isError = true
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            _state.value = AuthState()
        }
    }
}
