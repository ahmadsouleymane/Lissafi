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
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val isLoggedIn: Boolean = false
)

class AuthViewModel(private val authManager: AuthManager) : ViewModel() {

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

    fun clearMessage() {
        _state.value = _state.value.copy(message = null, isError = false)
    }

    fun signUp() {
        val state = _state.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _state.value = state.copy(message = "Remplis tous les champs.", isError = true)
            return
        }
        if (state.password.length < 6) {
            _state.value = state.copy(message = "Le mot de passe doit faire au moins 6 caractères.", isError = true)
            return
        }
        if (state.password != state.confirmPassword) {
            _state.value = state.copy(message = "Les mots de passe ne correspondent pas.", isError = true)
            return
        }

        _state.value = state.copy(isLoading = true, message = null)
        viewModelScope.launch {
            val result = authManager.signUp(state.email, state.password, state.shopName)
            when (result) {
                is AuthResult.Success -> {
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
