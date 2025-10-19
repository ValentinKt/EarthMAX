package com.earthmax.feature.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.core.models.User
import com.earthmax.core.models.ProfileTheme
import com.earthmax.core.models.ProfileVisibility
import com.earthmax.data.repository.UserRepository
import com.earthmax.core.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUpdating: Boolean = false,
    val updateSuccess: Boolean = false,
    val currentUser: User? = null,
    val displayName: String = "",
    val bio: String = "",
    val selectedImageUri: String? = null,
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
        Logger.enter("EditProfileViewModel", "init")
        Logger.logBusinessEvent("EditProfileViewModel", "edit_profile_view_model_initialized")
        loadCurrentUser()
        observeFormValidation()
        Logger.exit("EditProfileViewModel", "init")
    }

    private fun loadCurrentUser() {
        Logger.enter("EditProfileViewModel", "loadCurrentUser")
        val startTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            Logger.d("EditProfileViewModel", "Starting to load current user for editing")
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                userRepository.getCurrentUser().collect { user ->
                    Logger.d("EditProfileViewModel", "User loaded for editing: ${user?.id}")
                    Logger.logBusinessEvent("EditProfileViewModel", "edit_profile_user_loaded")
                    
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
                    
                    Logger.i("EditProfileViewModel", "Edit profile form initialized with user data")
                }
            } catch (e: Exception) {
                Logger.e("EditProfileViewModel", "Failed to load user for editing", e)
                Logger.logBusinessEvent("EditProfileViewModel", "edit_profile_load_failed")
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load profile"
                    )
                }
            }
        }
        
        Logger.logPerformance("EditProfileViewModel", "loadCurrentUser", System.currentTimeMillis() - startTime)
        Logger.exit("EditProfileViewModel", "loadCurrentUser")
    }

    private fun observeFormValidation() {
        Logger.enter("EditProfileViewModel", "observeFormValidation")
        
        viewModelScope.launch {
            Logger.d("EditProfileViewModel", "Setting up form validation observers")
            
            combine(
                _uiState.map { it.displayName },
                _uiState.map { it.bio }
            ) { displayName, bio ->
                val isValid = displayName.isNotBlank() && displayName.length <= 50 && bio.length <= 200
                Logger.d("EditProfileViewModel", "Form validation result: $isValid (displayName: ${displayName.length} chars, bio: ${bio.length} chars)")
                isValid
            }.collect { isValid ->
                _uiState.update { it.copy(isFormValid = isValid) }
                Logger.d("EditProfileViewModel", "Form validity updated: $isValid")
            }
        }
        
        Logger.exit("EditProfileViewModel", "observeFormValidation")
    }

    fun updateDisplayName(displayName: String) {
        Logger.enter("EditProfileViewModel", "updateDisplayName")
        Logger.logUserAction("EditProfileViewModel", "edit_profile_display_name_updated")
        
        _uiState.update { it.copy(displayName = displayName) }
        Logger.d("EditProfileViewModel", "Display name updated: ${displayName.length} characters")
        Logger.exit("EditProfileViewModel", "updateDisplayName")
    }

    fun updateBio(bio: String) {
        Logger.enter("EditProfileViewModel", "updateBio")
        Logger.logUserAction("EditProfileViewModel", "edit_profile_bio_updated")
        
        _uiState.update { it.copy(bio = bio) }
        Logger.d("EditProfileViewModel", "Bio updated: ${bio.length} characters")
        Logger.exit("EditProfileViewModel", "updateBio")
    }

    fun updateProfileImage(imageUri: String?) {
        Logger.enter("EditProfileViewModel", "updateProfileImage")
        Logger.logUserAction("EditProfileViewModel", "edit_profile_image_updated")
        
        _uiState.update { it.copy(selectedImageUri = imageUri) }
        Logger.d("EditProfileViewModel", "Profile image updated: ${if (imageUri != null) "set" else "cleared"}")
        Logger.exit("EditProfileViewModel", "updateProfileImage")
    }

    fun updateTheme(theme: ProfileTheme) {
        Logger.enter("EditProfileViewModel", "updateTheme")
        Logger.logUserAction("EditProfileViewModel", "edit_profile_theme_updated", mapOf(
            "new_theme" to theme.name,
            "previous_theme" to _uiState.value.selectedTheme.name
        ))
        
        _uiState.update { it.copy(selectedTheme = theme) }
        Logger.d("EditProfileViewModel", "Theme updated to: ${theme.name}")
        Logger.exit("EditProfileViewModel", "updateTheme")
    }

    fun updateVisibility(visibility: ProfileVisibility) {
        Logger.enter("EditProfileViewModel", "updateVisibility")
        Logger.logUserAction("EditProfileViewModel", "edit_profile_visibility_updated", mapOf(
            "new_visibility" to visibility.name,
            "previous_visibility" to _uiState.value.selectedVisibility.name
        ))
        
        _uiState.update { it.copy(selectedVisibility = visibility) }
        Logger.d("EditProfileViewModel", "Profile visibility updated to: ${visibility.name}")
        Logger.exit("EditProfileViewModel", "updateVisibility")
    }

    fun updateShowImpactStats(show: Boolean) {
        Logger.enter("EditProfileViewModel", "updateShowImpactStats")
        Logger.logUserAction("EditProfileViewModel", "edit_profile_impact_stats_toggled", mapOf(
            "show_stats" to show.toString(),
            "previous_value" to _uiState.value.showImpactStats.toString()
        ))
        
        _uiState.update { it.copy(showImpactStats = show) }
        Logger.d("EditProfileViewModel", "Show impact stats updated to: $show")
        Logger.exit("EditProfileViewModel", "updateShowImpactStats")
    }

    fun updateProfile() {
        Logger.enter("EditProfileViewModel", "updateProfile")
        val startTime = System.currentTimeMillis()
        
        val currentState = _uiState.value
        Logger.logUserAction("EditProfileViewModel", "edit_profile_update_initiated", mapOf(
            "display_name_length" to currentState.displayName.length.toString(),
            "bio_length" to currentState.bio.length.toString(),
            "theme" to currentState.selectedTheme.name,
            "visibility" to currentState.selectedVisibility.name,
            "show_impact_stats" to currentState.showImpactStats.toString(),
            "has_image" to (currentState.selectedImageUri != null).toString()
        ))
        
        val currentUser = currentState.currentUser
        
        if (!currentState.isFormValid || currentUser == null) {
            Logger.w("EditProfileViewModel", "Attempted to update profile with invalid form or null user")
            Logger.logBusinessEvent("EditProfileViewModel", "edit_profile_update_blocked", mapOf(
                "reason" to if (!currentState.isFormValid) "invalid_form" else "null_user"
            ))
            _uiState.update { it.copy(error = "Please fill in all required fields") }
            return
        }

        viewModelScope.launch {
            Logger.d("EditProfileViewModel", "Starting profile update process")
            _uiState.update { it.copy(isUpdating = true, error = null) }

            try {
                val currentUser = currentState.currentUser
                if (currentUser == null) {
                    Logger.e("EditProfileViewModel", "Cannot update profile: current user is null")
                    Logger.logBusinessEvent("EditProfileViewModel", "edit_profile_update_failed", mapOf(
                        "error_type" to "null_user",
                        "error_message" to "Current user is null"
                    ))
                    
                    _uiState.update { 
                        it.copy(
                            isUpdating = false,
                            error = "User not found"
                        )
                    }
                    return@launch
                }

                Logger.d("EditProfileViewModel", "Updating user profile for user: ${currentUser.id}")
                
                val updatedUser = currentUser.copy(
                    displayName = currentState.displayName,
                    bio = currentState.bio,
                    profileCustomization = currentUser.profileCustomization?.copy(
                        theme = currentState.selectedTheme,
                        profileVisibility = currentState.selectedVisibility,
                        showImpactStats = currentState.showImpactStats
                    ) ?: com.earthmax.core.models.ProfileCustomization(
                        theme = currentState.selectedTheme,
                        profileVisibility = currentState.selectedVisibility,
                        showImpactStats = currentState.showImpactStats
                    )
                )

                userRepository.updateUser(updatedUser)
                
                Logger.i("EditProfileViewModel", "Profile updated successfully")
                Logger.logBusinessEvent("EditProfileViewModel", "edit_profile_update_successful", mapOf(
                    "user_id" to currentUser.id,
                    "display_name_changed" to (currentUser.displayName != currentState.displayName).toString(),
                    "bio_changed" to (currentUser.bio != currentState.bio).toString(),
                    "theme_changed" to (currentUser.profileCustomization?.theme != currentState.selectedTheme).toString(),
                    "visibility_changed" to (currentUser.profileCustomization?.profileVisibility != currentState.selectedVisibility).toString(),
                    "impact_stats_changed" to (currentUser.profileCustomization?.showImpactStats != currentState.showImpactStats).toString()
                ))

                _uiState.update { 
                    it.copy(
                        isUpdating = false,
                        updateSuccess = true,
                        error = null
                    )
                }

            } catch (e: Exception) {
                Logger.e("EditProfileViewModel", "Failed to update profile", e)
                Logger.logBusinessEvent("EditProfileViewModel", "edit_profile_update_failed", mapOf(
                    "error_type" to e.javaClass.simpleName,
                    "error_message" to (e.message ?: "Unknown error")
                ))
                
                _uiState.update { 
                    it.copy(
                        isUpdating = false,
                        error = e.message ?: "Failed to update profile"
                    )
                }
            }
        }
        
        Logger.logPerformance("EditProfileViewModel", "updateProfile", System.currentTimeMillis() - startTime)
        Logger.exit("EditProfileViewModel", "updateProfile")
    }

    fun clearError() {
        Logger.enter("EditProfileViewModel", "clearError")
        Logger.logUserAction("EditProfileViewModel", "edit_profile_error_cleared")
        
        _uiState.update { it.copy(error = null) }
        Logger.d("EditProfileViewModel", "Error state cleared")
        Logger.exit("EditProfileViewModel", "clearError")
    }

    fun resetUpdateStatus() {
        Logger.enter("EditProfileViewModel", "resetUpdateStatus")
        Logger.logUserAction("EditProfileViewModel", "edit_profile_update_status_reset")
        
        _uiState.update { it.copy(updateSuccess = false) }
        Logger.d("EditProfileViewModel", "Update status reset")
        Logger.exit("EditProfileViewModel", "resetUpdateStatus")
    }
}