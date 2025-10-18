package com.earthmax.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.core.preferences.ThemePreferences
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
        observeThemeSettings()
    }

    private fun observeThemeSettings() {
        viewModelScope.launch {
            combine(
                themePreferences.isDarkMode,
                themePreferences.isHighContrast,
                themePreferences.followSystemTheme
            ) { isDarkMode, isHighContrast, followSystemTheme ->
                Triple(isDarkMode, isHighContrast, followSystemTheme)
            }.collect { (isDarkMode, isHighContrast, followSystemTheme) ->
                _uiState.update { currentState ->
                    currentState.copy(
                        darkModeEnabled = isDarkMode,
                        highContrastEnabled = isHighContrast,
                        followSystemTheme = followSystemTheme
                    )
                }
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        // TODO: Implement actual notification preferences persistence
    }

    fun toggleLocation(enabled: Boolean) {
        _uiState.update { it.copy(locationEnabled = enabled) }
        // TODO: Implement actual location preferences persistence
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                themePreferences.setDarkMode(enabled)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleHighContrast(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                themePreferences.setHighContrast(enabled)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleFollowSystemTheme(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                themePreferences.setFollowSystemTheme(enabled)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}