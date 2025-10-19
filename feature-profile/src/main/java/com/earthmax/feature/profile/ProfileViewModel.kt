package com.earthmax.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.core.models.User
import com.earthmax.core.models.EnvironmentalImpact
import com.earthmax.core.models.MonthlyGoals
import com.earthmax.core.models.Achievement
import com.earthmax.core.models.AchievementCategory
import com.earthmax.data.repository.UserRepository
import com.earthmax.core.utils.Logger
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
        Logger.enter("ProfileViewModel", "init")
        Logger.logBusinessEvent("ProfileViewModel", "profile_view_model_initialized")
        loadCurrentUser()
        Logger.exit("ProfileViewModel", "init")
    }

    private fun loadCurrentUser() {
        Logger.enter("ProfileViewModel", "loadCurrentUser")
        val startTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            Logger.d("ProfileViewModel", "Starting to load current user profile")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                userRepository.getCurrentUser().collect { user ->
                    Logger.d("ProfileViewModel", "User profile loaded successfully: ${user?.id}")
                    Logger.logBusinessEvent("ProfileViewModel", "profile_loaded", mapOf(
                        "user_id" to (user?.id ?: "null"),
                        "has_environmental_impact" to (user?.environmentalImpact != null).toString(),
                        "achievements_count" to (user?.environmentalImpact?.achievements?.size ?: 0).toString()
                    ))
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        error = null,
                        environmentalImpact = user?.environmentalImpact,
                        monthlyGoals = user?.environmentalImpact?.monthlyGoals,
                        recentAchievements = user?.environmentalImpact?.achievements?.takeLast(3) ?: emptyList()
                    )
                    
                    Logger.i("ProfileViewModel", "Profile UI state updated successfully")
                }
            } catch (e: Exception) {
                Logger.e("ProfileViewModel", "Failed to load user profile", e)
                Logger.logBusinessEvent("ProfileViewModel", "profile_load_failed", mapOf(
                    "error_type" to e.javaClass.simpleName,
                    "error_message" to (e.message ?: "Unknown error")
                ))
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load profile"
                )
            }
        }
        
        Logger.logPerformance("ProfileViewModel", "loadCurrentUser", System.currentTimeMillis() - startTime)
        Logger.exit("ProfileViewModel", "loadCurrentUser")
    }

    fun signOut() {
        Logger.enter("ProfileViewModel", "signOut")
        Logger.logUserAction("ProfileViewModel", "sign_out_initiated")
        val startTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            Logger.d("ProfileViewModel", "Starting sign out process")
            _uiState.value = _uiState.value.copy(isSigningOut = true, error = null)
            
            try {
                userRepository.signOut()
                Logger.i("ProfileViewModel", "User signed out successfully")
                Logger.logBusinessEvent("ProfileViewModel", "user_signed_out")
                // Navigation will be handled by the parent component
            } catch (e: Exception) {
                Logger.e("ProfileViewModel", "Failed to sign out user", e)
                Logger.logBusinessEvent("ProfileViewModel", "sign_out_failed", mapOf(
                    "error_type" to e.javaClass.simpleName,
                    "error_message" to (e.message ?: "Unknown error")
                ))
                
                _uiState.value = _uiState.value.copy(
                    isSigningOut = false,
                    error = e.message ?: "Failed to sign out"
                )
            }
        }
        
        Logger.logPerformance("ProfileViewModel", "signOut", System.currentTimeMillis() - startTime)
        Logger.exit("ProfileViewModel", "signOut")
    }

    fun clearError() {
        Logger.enter("ProfileViewModel", "clearError")
        Logger.logUserAction("ProfileViewModel", "clear_error")
        _uiState.value = _uiState.value.copy(error = null)
        Logger.d("ProfileViewModel", "Error state cleared")
        Logger.exit("ProfileViewModel", "clearError")
    }

    fun refreshProfile() {
        Logger.enter("ProfileViewModel", "refreshProfile")
        Logger.logUserAction("ProfileViewModel", "refresh_profile")
        Logger.d("ProfileViewModel", "Refreshing profile data")
        loadCurrentUser()
        Logger.exit("ProfileViewModel", "refreshProfile")
    }
}