package com.earthmax.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.data.auth.SupabaseAuthRepository
import com.earthmax.core.models.User
import com.earthmax.core.utils.Logger
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

    companion object {
        private const val TAG = "SupabaseAuthViewModel"
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        Logger.enter(TAG, "init")
        Logger.logUserAction(TAG, "ViewModel Initialized")
        checkAuthState()
        Logger.exit(TAG, "init")
    }

    private fun checkAuthState() {
        Logger.enter(TAG, "checkAuthState")
        
        viewModelScope.launch {
            try {
                Logger.d(TAG, "Starting auth state collection")
                authRepository.getCurrentUser().collect { user ->
                    Logger.d(TAG, "Received user update: ${if (user != null) "authenticated" else "unauthenticated"}")
                    
                    if (user != null) {
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated
                        
                        Logger.logBusinessEvent(TAG, "User Authentication State Changed", mapOf(
                            "state" to "authenticated",
                            "userId" to user.id,
                            "email" to user.email
                        ))
                    } else {
                        _authState.value = AuthState.Unauthenticated
                        
                        Logger.logBusinessEvent(TAG, "User Authentication State Changed", mapOf(
                            "state" to "unauthenticated"
                        ))
                    }
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error checking auth state", e)
                Logger.logBusinessEvent(TAG, "Auth State Check Error", mapOf(
                    "errorType" to e.javaClass.simpleName,
                    "errorMessage" to (e.message ?: "Unknown error")
                ))
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
        
        Logger.exit(TAG, "checkAuthState")
    }

    fun signUp(email: String, password: String, name: String) {
        Logger.enter(TAG, "signUp")
        Logger.logUserAction(TAG, "Sign Up Initiated", mapOf(
            "email" to email,
            "name" to name
        ))
        
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _authState.value = AuthState.Loading
            
            try {
                Logger.d(TAG, "Calling repository signUp")
                val result = authRepository.signUp(email, password, name)
                
                if (result.isSuccess) {
                    Logger.i(TAG, "Sign up request sent successfully")
                    Logger.logBusinessEvent(TAG, "Sign Up Success", mapOf(
                        "email" to email,
                        "name" to name,
                        "confirmationRequired" to true
                    ))
                    _authState.value = AuthState.EmailConfirmationSent
                } else {
                    val error = result.exceptionOrNull()
                    Logger.w(TAG, "Sign up failed: ${error?.message}")
                    Logger.logBusinessEvent(TAG, "Sign Up Failed", mapOf(
                        "email" to email,
                        "errorType" to (error?.javaClass?.simpleName ?: "Unknown"),
                        "errorMessage" to (error?.message ?: "Sign up failed")
                    ))
                    _authState.value = AuthState.Error(error?.message ?: "Sign up failed")
                }
                
                Logger.logPerformance(TAG, "signUp", System.currentTimeMillis() - startTime)
            } catch (e: Exception) {
                Logger.e(TAG, "Exception during sign up", e)
                Logger.logBusinessEvent(TAG, "Sign Up Exception", mapOf(
                    "email" to email,
                    "errorType" to e.javaClass.simpleName,
                    "errorMessage" to (e.message ?: "Sign up failed")
                ))
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
                Logger.logPerformance(TAG, "signUp", System.currentTimeMillis() - startTime)
            }
        }
        
        Logger.exit(TAG, "signUp")
    }

    fun signIn(email: String, password: String) {
        Logger.enter(TAG, "signIn")
        Logger.logUserAction(TAG, "Sign In Initiated", mapOf("email" to email))
        
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _authState.value = AuthState.Loading
            
            try {
                Logger.d(TAG, "Calling repository signIn")
                val user = authRepository.signIn(email, password).getOrNull()
                
                if (user != null) {
                    Logger.i(TAG, "Sign in successful")
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated
                    
                    Logger.logBusinessEvent(TAG, "Sign In Success", mapOf(
                        "userId" to user.id,
                        "email" to user.email
                    ))
                } else {
                    Logger.w(TAG, "Sign in failed - no user returned")
                    Logger.logBusinessEvent(TAG, "Sign In Failed", mapOf(
                        "email" to email,
                        "reason" to "No user returned"
                    ))
                    _authState.value = AuthState.Error("Sign in failed")
                }
                
                Logger.logPerformance(TAG, "signIn", System.currentTimeMillis() - startTime)
            } catch (e: Exception) {
                Logger.e(TAG, "Exception during sign in", e)
                Logger.logBusinessEvent(TAG, "Sign In Exception", mapOf(
                    "email" to email,
                    "errorType" to e.javaClass.simpleName,
                    "errorMessage" to (e.message ?: "Sign in failed")
                ))
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
                Logger.logPerformance(TAG, "signIn", System.currentTimeMillis() - startTime)
            }
        }
        
        Logger.exit(TAG, "signIn")
    }

    fun signOut() {
        Logger.enter(TAG, "signOut")
        Logger.logUserAction(TAG, "Sign Out Initiated")
        
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            
            try {
                Logger.d(TAG, "Calling repository signOut")
                authRepository.signOut()
                
                _currentUser.value = null
                _authState.value = AuthState.Unauthenticated
                
                Logger.i(TAG, "Sign out successful")
                Logger.logBusinessEvent(TAG, "Sign Out Success")
                Logger.logPerformance(TAG, "signOut", System.currentTimeMillis() - startTime)
            } catch (e: Exception) {
                Logger.e(TAG, "Exception during sign out", e)
                Logger.logBusinessEvent(TAG, "Sign Out Exception", mapOf(
                    "errorType" to e.javaClass.simpleName,
                    "errorMessage" to (e.message ?: "Sign out failed")
                ))
                _authState.value = AuthState.Error(e.message ?: "Sign out failed")
                Logger.logPerformance(TAG, "signOut", System.currentTimeMillis() - startTime)
            }
        }
        
        Logger.exit(TAG, "signOut")
    }

    fun resetPassword(email: String) {
        Logger.enter(TAG, "resetPassword")
        Logger.logUserAction(TAG, "Password Reset Initiated", mapOf("email" to email))
        
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            
            try {
                Logger.d(TAG, "Calling repository resetPassword")
                authRepository.resetPassword(email)
                
                Logger.i(TAG, "Password reset request sent successfully")
                Logger.logBusinessEvent(TAG, "Password Reset Success", mapOf("email" to email))
                _authState.value = AuthState.PasswordResetSent
                Logger.logPerformance(TAG, "resetPassword", System.currentTimeMillis() - startTime)
            } catch (e: Exception) {
                Logger.e(TAG, "Exception during password reset", e)
                Logger.logBusinessEvent(TAG, "Password Reset Exception", mapOf(
                    "email" to email,
                    "errorType" to e.javaClass.simpleName,
                    "errorMessage" to (e.message ?: "Password reset failed")
                ))
                _authState.value = AuthState.Error(e.message ?: "Password reset failed")
                Logger.logPerformance(TAG, "resetPassword", System.currentTimeMillis() - startTime)
            }
        }
        
        Logger.exit(TAG, "resetPassword")
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