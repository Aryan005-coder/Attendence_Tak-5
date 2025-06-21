package com.example.attendence.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val authManager = AuthManager()

    // Public state for UI to observe
    val authState: StateFlow<AuthResult?> = authManager.authState
    val currentUser: StateFlow<User?> = authManager.currentUser

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authManager.loginUser(email, password)
            _uiState.value = when (result) {
                is AuthResult.Success -> _uiState.value.copy(isLoading = false)
                is AuthResult.Error -> _uiState.value.copy(isLoading = false, errorMessage = result.message)
                is AuthResult.Loading -> _uiState.value.copy(isLoading = true)
            }
        }
    }

    fun register(
        email: String,
        password: String,
        name: String,
        role: String,
        studentId: String = "",
        department: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authManager.registerUser(email, password, name, role, studentId, department)
            _uiState.value = when (result) {
                is AuthResult.Success -> _uiState.value.copy(isLoading = false)
                is AuthResult.Error -> _uiState.value.copy(isLoading = false, errorMessage = result.message)
                is AuthResult.Loading -> _uiState.value.copy(isLoading = true)
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authManager.resetPassword(email)
            _uiState.value = when (result) {
                is AuthResult.Success -> _uiState.value.copy(isLoading = false, successMessage = "Reset email sent!")
                is AuthResult.Error -> _uiState.value.copy(isLoading = false, errorMessage = result.message)
                is AuthResult.Loading -> _uiState.value.copy(isLoading = true)
            }
        }
    }

    fun logout() {
        authManager.logout()
        _uiState.value = AuthUiState()
    }

    fun isUserLoggedIn(): Boolean {
        return authManager.isUserLoggedIn()
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}

// UI state for authentication screen
data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)


