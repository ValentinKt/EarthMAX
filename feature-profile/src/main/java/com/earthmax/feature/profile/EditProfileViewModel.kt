package com.earthmax.feature.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.core.models.User
import com.earthmax.core.models.ProfileTheme
import com.earthmax.core.models.ProfileVisibility
import com.earthmax.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUpdateSuccessful: Boolean = false,
    val currentUser: User? = null,
    val displayName: String = "",
    val bio: String = "",
    val profileImageUri: Uri? = null,
    val isFormValid: Boolean = false,
    val selectedTheme: ProfileTheme = ProfileTheme.FOREST,
    val selectedVisibility: ProfileVisibility = ProfileVisibility.PUBLIC,
    val showImpactStats: Boolean = true
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
        observeFormValidation()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                userRepository.getCurrentUser().collect { user ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            currentUser = user,
                            displayName = user?.displayName ?: "",
                            bio = user?.bio ?: "",
                            selectedTheme = user?.profileCustomization?.theme ?: ProfileTheme.FOREST,
                            selectedVisibility = user?.profileCustomization?.profileVisibility ?: ProfileVisibility.PUBLIC,
                            showImpactStats = user?.profileCustomization?.showImpactStats ?: true,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load profile"
                    )
                }
            }
        }
    }

    private fun observeFormValidation() {
        viewModelScope.launch {
            _uiState.collect { state ->
                val isValid = state.displayName.isNotBlank()
                
                if (state.isFormValid != isValid) {
                    _uiState.update { it.copy(isFormValid = isValid) }
                }
            }
        }
    }

    fun updateDisplayName(displayName: String) {
        _uiState.update { it.copy(displayName = displayName) }
    }

    fun updateBio(bio: String) {
        _uiState.update { it.copy(bio = bio) }
    }

    fun updateProfileImage(uri: Uri) {
        _uiState.update { it.copy(profileImageUri = uri) }
    }

    fun updateTheme(theme: ProfileTheme) {
        _uiState.update { it.copy(selectedTheme = theme) }
    }

    fun updateVisibility(visibility: ProfileVisibility) {
        _uiState.update { it.copy(selectedVisibility = visibility) }
    }

    fun updateShowImpactStats(show: Boolean) {
        _uiState.update { it.copy(showImpactStats = show) }
    }

    fun updateProfile() {
        val state = _uiState.value
        val currentUser = state.currentUser
        
        if (!state.isFormValid || currentUser == null) {
            _uiState.update { it.copy(error = "Please fill in all required fields") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Create updated profile customization
                val updatedProfileCustomization = currentUser.profileCustomization.copy(
                    theme = state.selectedTheme,
                    profileVisibility = state.selectedVisibility,
                    showImpactStats = state.showImpactStats
                )
                
                // Create updated user object with all changes
                val updatedUser = currentUser.copy(
                    displayName = state.displayName,
                    bio = state.bio,
                    profileCustomization = updatedProfileCustomization,
                    updatedAt = java.util.Date()
                )

                // Update user profile
                val result = userRepository.updateUser(updatedUser)
                
                if (result.isSuccess) {
                    // Upload profile image if selected
                    state.profileImageUri?.let { uri ->
                        userRepository.updateUserProfileImage(currentUser.id, uri.toString())
                    }
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isUpdateSuccessful = true,
                            error = null
                        )
                    }
                } else {
                    throw result.exceptionOrNull() ?: Exception("Failed to update profile")
                }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Failed to update profile"
                    ) 
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetUpdateStatus() {
        _uiState.update { it.copy(isUpdateSuccessful = false) }
    }
}