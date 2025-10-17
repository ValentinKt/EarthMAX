package com.earthmax.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.data.auth.SupabaseAuthRepository
import com.earthmax.core.models.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupabaseAuthViewModel @Inject constructor(
    private val authRepository: SupabaseAuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                authRepository.getCurrentUser().collect { user ->
                    if (user != null) {
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated
                    } else {
                        _authState.value = AuthState.Unauthenticated
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun signUp(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = authRepository.signUp(email, password, name)
                if (result.isSuccess) {
                    // Signup request sent successfully, email confirmation required
                    _authState.value = AuthState.EmailConfirmationSent
                } else {
                    val error = result.exceptionOrNull()
                    _authState.value = AuthState.Error(error?.message ?: "Sign up failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = authRepository.signIn(email, password).getOrNull()
                if (user != null) {
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error("Sign in failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                _currentUser.value = null
                _authState.value = AuthState.Unauthenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign out failed")
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            try {
                authRepository.resetPassword(email)
                _authState.value = AuthState.PasswordResetSent
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Password reset failed")
            }
        }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object EmailConfirmationSent : AuthState()
    object PasswordResetSent : AuthState()
    data class Error(val message: String) : AuthState()
}