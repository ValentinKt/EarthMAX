package com.earthmax.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.core.models.User
import com.earthmax.core.models.EnvironmentalImpact
import com.earthmax.core.models.MonthlyGoals
import com.earthmax.core.models.Achievement
import com.earthmax.core.models.AchievementCategory
import com.earthmax.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isSigningOut: Boolean = false,
    val environmentalImpact: EnvironmentalImpact? = null,
    val monthlyGoals: MonthlyGoals? = null,
    val recentAchievements: List<Achievement> = emptyList()
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                userRepository.getCurrentUser().collect { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        error = null,
                        environmentalImpact = user?.environmentalImpact,
                        monthlyGoals = user?.environmentalImpact?.monthlyGoals,
                        recentAchievements = user?.environmentalImpact?.achievements?.takeLast(3) ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load profile"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSigningOut = true, error = null)
            
            try {
                userRepository.signOut()
                // Navigation will be handled by the parent component
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSigningOut = false,
                    error = e.message ?: "Failed to sign out"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refreshProfile() {
        loadCurrentUser()
    }
}