package com.earthmax.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.core.preferences.ThemePreferences
import com.earthmax.core.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val locationEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val highContrastEnabled: Boolean = false,
    val followSystemTheme: Boolean = true,
    val isLoading: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        Logger.enter("SettingsViewModel", "init")
        Logger.logBusinessEvent("SettingsViewModel", "settings_view_model_initialized")
        observeThemeSettings()
        Logger.exit("SettingsViewModel", "init")
    }

    private fun observeThemeSettings() {
        Logger.enter("SettingsViewModel", "observeThemeSettings")
        
        viewModelScope.launch {
            Logger.d("SettingsViewModel", "Starting to observe theme settings")
            combine(
                themePreferences.isDarkMode,
                themePreferences.isHighContrast,
                themePreferences.followSystemTheme
            ) { isDarkMode, isHighContrast, followSystemTheme ->
                Triple(isDarkMode, isHighContrast, followSystemTheme)
            }.collect { (isDarkMode, isHighContrast, followSystemTheme) ->
                Logger.d("SettingsViewModel", "Theme settings updated - Dark: $isDarkMode, HighContrast: $isHighContrast, FollowSystem: $followSystemTheme")
                Logger.logBusinessEvent("SettingsViewModel", "theme_settings_updated", mapOf(
                    "dark_mode" to isDarkMode.toString(),
                    "high_contrast" to isHighContrast.toString(),
                    "follow_system" to followSystemTheme.toString()
                ))
                
                _uiState.update { currentState ->
                    currentState.copy(
                        darkModeEnabled = isDarkMode,
                        highContrastEnabled = isHighContrast,
                        followSystemTheme = followSystemTheme
                    )
                }
            }
        }
        
        Logger.exit("SettingsViewModel", "observeThemeSettings")
    }

    fun toggleNotifications(enabled: Boolean) {
        Logger.enter("SettingsViewModel", "toggleNotifications", "enabled" to enabled)
        Logger.logUserAction("SettingsViewModel", "toggle_notifications", mapOf("enabled" to enabled.toString()))
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        Logger.d("SettingsViewModel", "Notifications toggled to: $enabled")
        // TODO: Implement actual notification preferences persistence
        Logger.exit("SettingsViewModel", "toggleNotifications")
    }

    fun toggleLocation(enabled: Boolean) {
        Logger.enter("SettingsViewModel", "toggleLocation", "enabled" to enabled)
        Logger.logUserAction("SettingsViewModel", "toggle_location", mapOf("enabled" to enabled.toString()))
        _uiState.update { it.copy(locationEnabled = enabled) }
        Logger.d("SettingsViewModel", "Location toggled to: $enabled")
        // TODO: Implement actual location preferences persistence
        Logger.exit("SettingsViewModel", "toggleLocation")
    }

    fun toggleDarkMode(enabled: Boolean) {
        Logger.enter("SettingsViewModel", "toggleDarkMode", "enabled" to enabled)
        Logger.logUserAction("SettingsViewModel", "toggle_dark_mode", mapOf("enabled" to enabled.toString()))
        val startTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            Logger.d("SettingsViewModel", "Starting dark mode toggle to: $enabled")
            _uiState.update { it.copy(isLoading = true) }
            try {
                themePreferences.setDarkMode(enabled)
                Logger.i("SettingsViewModel", "Dark mode preference saved successfully")
                Logger.logBusinessEvent("SettingsViewModel", "dark_mode_changed", mapOf("enabled" to enabled.toString()))
            } catch (e: Exception) {
                Logger.e("SettingsViewModel", "Failed to save dark mode preference", e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        
        Logger.logPerformance("SettingsViewModel", "toggleDarkMode", System.currentTimeMillis() - startTime)
        Logger.exit("SettingsViewModel", "toggleDarkMode")
    }

    fun toggleHighContrast(enabled: Boolean) {
        Logger.enter("SettingsViewModel", "toggleHighContrast", "enabled" to enabled)
        Logger.logUserAction("SettingsViewModel", "toggle_high_contrast", mapOf("enabled" to enabled.toString()))
        val startTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            Logger.d("SettingsViewModel", "Starting high contrast toggle to: $enabled")
            _uiState.update { it.copy(isLoading = true) }
            try {
                themePreferences.setHighContrast(enabled)
                Logger.i("SettingsViewModel", "High contrast preference saved successfully")
                Logger.logBusinessEvent("SettingsViewModel", "high_contrast_changed", mapOf("enabled" to enabled.toString()))
            } catch (e: Exception) {
                Logger.e("SettingsViewModel", "Failed to save high contrast preference", e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        
        Logger.logPerformance("SettingsViewModel", "toggleHighContrast", System.currentTimeMillis() - startTime)
        Logger.exit("SettingsViewModel", "toggleHighContrast")
    }

    fun toggleFollowSystemTheme(enabled: Boolean) {
        Logger.enter("SettingsViewModel", "toggleFollowSystemTheme", "enabled" to enabled)
        Logger.logUserAction("SettingsViewModel", "toggle_follow_system_theme", mapOf("enabled" to enabled.toString()))
        val startTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            Logger.d("SettingsViewModel", "Starting follow system theme toggle to: $enabled")
            _uiState.update { it.copy(isLoading = true) }
            try {
                themePreferences.setFollowSystemTheme(enabled)
                Logger.i("SettingsViewModel", "Follow system theme preference saved successfully")
                Logger.logBusinessEvent("SettingsViewModel", "follow_system_theme_changed", mapOf("enabled" to enabled.toString()))
            } catch (e: Exception) {
                Logger.e("SettingsViewModel", "Failed to save follow system theme preference", e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        
        Logger.logPerformance("SettingsViewModel", "toggleFollowSystemTheme", System.currentTimeMillis() - startTime)
        Logger.exit("SettingsViewModel", "toggleFollowSystemTheme")
    }
}