package com.example.attendence.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val context: Context) : ViewModel() {

    private val authManager = AuthManager()

    val authState: StateFlow<AuthResult?> = authManager.authState
    val currentUser: StateFlow<User?> = authManager.currentUser

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ADD THIS METHOD - for auto-login
    fun attemptAutoLogin() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                authState = AuthResult.Loading
            )

            try {
                val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                val rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false)

                if (rememberMe) {
                    val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
                    val password = prefs.getString(KEY_USER_PASSWORD, "") ?: ""

                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        // Use existing authManager to login
                        val result = authManager.loginUser(email, password)

                        _uiState.value = when (result) {
                            is AuthResult.Success -> _uiState.value.copy(
                                isLoading = false,
                                authState = result
                            )
                            is AuthResult.Error -> {
                                // Clear invalid stored credentials
                                clearStoredCredentials()
                                _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = null, // Don't show auto-login errors
                                    authState = null
                                )
                            }
                            is AuthResult.Loading -> _uiState.value.copy(
                                isLoading = true,
                                authState = result
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            authState = null
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        authState = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null, // Don't show auto-login errors to user
                    authState = null
                )
            }
        }
    }

    // UPDATE THIS METHOD - add rememberMe parameter
    fun login(email: String, password: String, rememberMe: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                authState = AuthResult.Loading
            )

            val result = authManager.loginUser(email, password)

            _uiState.value = when (result) {
                is AuthResult.Success -> {
                    // Save credentials if rememberMe is true
                    if (rememberMe) {
                        saveCredentials(email, password)
                    }

                    _uiState.value.copy(
                        isLoading = false,
                        authState = result
                    )
                }
                is AuthResult.Error -> _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message,
                    authState = result
                )
                is AuthResult.Loading -> _uiState.value.copy(
                    isLoading = true,
                    authState = result
                )
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
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                authState = AuthResult.Loading
            )

            val result = authManager.registerUser(email, password, name, role, studentId, department)

            _uiState.value = when (result) {
                is AuthResult.Success -> _uiState.value.copy(
                    isLoading = false,
                    authState = result
                )
                is AuthResult.Error -> _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message,
                    authState = result
                )
                is AuthResult.Loading -> _uiState.value.copy(
                    isLoading = true,
                    authState = result
                )
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                authState = AuthResult.Loading
            )

            val result = authManager.resetPassword(email)

            _uiState.value = when (result) {
                is AuthResult.Success -> _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Reset email sent!",
                    authState = result
                )
                is AuthResult.Error -> _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message,
                    authState = result
                )
                is AuthResult.Loading -> _uiState.value.copy(
                    isLoading = true,
                    authState = result
                )
            }
        }
    }

    fun logout() {
        // Clear stored credentials on logout
        clearStoredCredentials()
        authManager.logout()
        _uiState.value = AuthUiState()
    }

    fun isUserLoggedIn(): Boolean {
        return authManager.isUserLoggedIn()
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    // ADD THESE HELPER METHODS
    private fun saveCredentials(email: String, password: String) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString(KEY_USER_EMAIL, email)
                putString(KEY_USER_PASSWORD, password)
                putBoolean(KEY_REMEMBER_ME, true)
                apply()
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    private fun clearStoredCredentials() {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                remove(KEY_USER_EMAIL)
                remove(KEY_USER_PASSWORD)
                putBoolean(KEY_REMEMBER_ME, false)
                apply()
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    companion object {
        private const val PREF_NAME = "user_prefs"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PASSWORD = "user_password"
    }
}

// Updated data class with authState:
data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val authState: AuthResult? = null
)