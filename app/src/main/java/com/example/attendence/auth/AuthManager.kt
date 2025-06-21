package com.example.attendence.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "",
    val studentId: String = "",
    val department: String = ""
)

sealed class AuthResult {
    data object Loading : AuthResult()
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthResult?>(null)
    val authState: StateFlow<AuthResult?> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        auth.currentUser?.let { firebaseUser ->
            CoroutineScope(Dispatchers.IO).launch {
                loadUserData(firebaseUser.uid)
            }
        }
    }

    suspend fun registerUser(
        email: String,
        password: String,
        name: String,
        role: String,
        studentId: String = "",
        department: String
    ): AuthResult {
        return try {
            _authState.value = AuthResult.Loading
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                val user = User(
                    uid = firebaseUser.uid,
                    email = email,
                    name = name,
                    role = role,
                    studentId = studentId,
                    department = department
                )

                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(user)
                    .await()

                _currentUser.value = user
                _authState.value = AuthResult.Success
                AuthResult.Success
            } else {
                _authState.value = AuthResult.Error("Registration failed")
                AuthResult.Error("Registration failed")
            }
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Registration failed"
            _authState.value = AuthResult.Error(errorMessage)
            AuthResult.Error(errorMessage)
        }
    }

    suspend fun loginUser(email: String, password: String): AuthResult {
        return try {
            _authState.value = AuthResult.Loading
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                loadUserData(firebaseUser.uid)
                _authState.value = AuthResult.Success
                AuthResult.Success
            } else {
                _authState.value = AuthResult.Error("Login failed")
                AuthResult.Error("Login failed")
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("password") == true -> "Invalid password"
                e.message?.contains("email") == true -> "Invalid email"
                e.message?.contains("network") == true -> "Network error"
                else -> e.message ?: "Login failed"
            }
            _authState.value = AuthResult.Error(errorMessage)
            AuthResult.Error(errorMessage)
        }
    }

    private suspend fun loadUserData(uid: String) {
        try {
            val document = firestore.collection("users").document(uid).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                if (user != null) {
                    _currentUser.value = user
                    _authState.value = AuthResult.Success // 🔥 fix: restore session state
                } else {
                    _currentUser.value = null
                }
            } else {
                _currentUser.value = null
            }
        } catch (e: Exception) {
            _currentUser.value = null
        }
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
        _authState.value = null
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentFirebaseUser(): FirebaseUser? {
        return auth.currentUser
    }

    suspend fun resetPassword(email: String): AuthResult {
        return try {
            _authState.value = AuthResult.Loading
            auth.sendPasswordResetEmail(email).await()
            _authState.value = AuthResult.Success
            AuthResult.Success
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Failed to send reset email"
            _authState.value = AuthResult.Error(errorMessage)
            AuthResult.Error(errorMessage)
        }
    }

    suspend fun updateUserProfile(
        name: String,
        department: String,
        studentId: String = ""
    ): AuthResult {
        return try {
            val currentUid = auth.currentUser?.uid ?: return AuthResult.Error("No user logged in")

            val updates = mapOf(
                "name" to name,
                "department" to department,
                "studentId" to studentId
            )

            firestore.collection("users")
                .document(currentUid)
                .update(updates)
                .await()

            loadUserData(currentUid)
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Failed to update profile")
        }
    }
}
