package com.earthmax.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

@Singleton
class ThemePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val FOLLOW_SYSTEM = booleanPreferencesKey("follow_system")
    }

    val isDarkMode: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DARK_MODE] ?: false
        }

    val isHighContrast: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.HIGH_CONTRAST] ?: false
        }

    val followSystemTheme: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.FOLLOW_SYSTEM] ?: true
        }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = enabled
            // When manually setting dark mode, disable follow system
            preferences[PreferencesKeys.FOLLOW_SYSTEM] = false
        }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIGH_CONTRAST] = enabled
        }
    }

    suspend fun setFollowSystemTheme(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOLLOW_SYSTEM] = enabled
        }
    }

    data class ThemeSettings(
        val isDarkMode: Boolean = false,
        val isHighContrast: Boolean = false,
        val followSystemTheme: Boolean = true
    )

    val themeSettings: Flow<ThemeSettings> = dataStore.data
        .map { preferences ->
            ThemeSettings(
                isDarkMode = preferences[PreferencesKeys.DARK_MODE] ?: false,
                isHighContrast = preferences[PreferencesKeys.HIGH_CONTRAST] ?: false,
                followSystemTheme = preferences[PreferencesKeys.FOLLOW_SYSTEM] ?: true
            )
        }
}