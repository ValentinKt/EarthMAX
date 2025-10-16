package com.earthmax.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.data.auth.SupabaseAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainNavigationUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class MainNavigationViewModel @Inject constructor(
    private val authRepository: SupabaseAuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainNavigationUiState())
    val uiState: StateFlow<MainNavigationUiState> = _uiState.asStateFlow()
    
    fun checkAuthState() {
        viewModelScope.launch {
            try {
                authRepository.getCurrentUser().collect { currentUser ->
                    val isAuthenticated = currentUser != null
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = isAuthenticated,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = false,
                    isLoading = false
                )
            }
        }
    }
}