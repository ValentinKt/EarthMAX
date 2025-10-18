package com.earthmax.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    secondary = TealGrey80,
    tertiary = Green80,
    background = SurfaceDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceDarkVariant,
    error = ErrorDark,
    errorContainer = ErrorContainer,
    onPrimary = HighContrastBackground,  // Use high contrast white
    onSecondary = HighContrastBackground,
    onTertiary = HighContrastBackground,
    onBackground = HighContrastBackground,
    onSurface = HighContrastBackground,
    onSurfaceVariant = Color(0xFFE0E0E0),  // Improved contrast
    onError = HighContrastText,  // Use high contrast black
    outline = Color(0xFF6A7A6A),  // Improved contrast
    outlineVariant = Color(0xFF3A4A3A),  // Improved contrast
    scrim = OverlayDark,
    inverseSurface = SurfaceLight,
    inverseOnSurface = HighContrastText,
    inversePrimary = Teal40,
    surfaceTint = Teal80
)

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    secondary = TealGrey40,
    tertiary = Green40,
    background = SurfaceLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceLightVariant,
    error = ErrorLight,
    errorContainer = ErrorContainer,
    onPrimary = HighContrastBackground,  // Use high contrast white
    onSecondary = HighContrastBackground,
    onTertiary = HighContrastBackground,
    onBackground = HighContrastText,  // Use high contrast black
    onSurface = HighContrastText,
    onSurfaceVariant = Color(0xFF2A3A2A),  // Improved contrast
    onError = HighContrastBackground,
    outline = Color(0xFF5A6A5A),  // Improved contrast
    outlineVariant = Color(0xFFAABAA0),  // Improved contrast
    scrim = OverlayDark,
    inverseSurface = SurfaceDark,
    inverseOnSurface = HighContrastBackground,
    inversePrimary = Teal80,
    surfaceTint = Teal40,
    // Enhanced semantic colors with improved contrast
    primaryContainer = Color(0xFFE0F2F1),
    onPrimaryContainer = HighContrastPrimary,  // Use high contrast primary
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF001F24),
    tertiaryContainer = Color(0xFFE8F5E8),
    onTertiaryContainer = HighContrastSecondary  // Use high contrast secondary
)

// High contrast theme for accessibility
private val HighContrastLightColorScheme = lightColorScheme(
    primary = HighContrastPrimary,
    secondary = HighContrastSecondary,
    tertiary = HighContrastSecondary,
    background = HighContrastBackground,
    surface = HighContrastBackground,
    surfaceVariant = Color(0xFFF5F5F5),
    error = Color(0xFF8B0000),  // Dark red for high contrast
    errorContainer = Color(0xFFFFE6E6),
    onPrimary = HighContrastBackground,
    onSecondary = HighContrastBackground,
    onTertiary = HighContrastBackground,
    onBackground = HighContrastText,
    onSurface = HighContrastText,
    onSurfaceVariant = HighContrastText,
    onError = HighContrastBackground,
    outline = HighContrastText,
    outlineVariant = Color(0xFF666666),
    scrim = Color(0xCC000000),
    inverseSurface = HighContrastText,
    inverseOnSurface = HighContrastBackground,
    inversePrimary = Color(0xFF66CCCC),
    surfaceTint = HighContrastPrimary,
    primaryContainer = Color(0xFFE6F7F7),
    onPrimaryContainer = HighContrastText,
    secondaryContainer = Color(0xFFE6F2E6),
    onSecondaryContainer = HighContrastText,
    tertiaryContainer = Color(0xFFE6F2E6),
    onTertiaryContainer = HighContrastText
)

private val HighContrastDarkColorScheme = darkColorScheme(
    primary = Color(0xFF66CCCC),  // Bright teal for high contrast
    secondary = Color(0xFF66CC66),  // Bright green for high contrast
    tertiary = Color(0xFF66CC66),
    background = HighContrastText,
    surface = HighContrastText,
    surfaceVariant = Color(0xFF1A1A1A),
    error = Color(0xFFFF6666),  // Bright red for high contrast
    errorContainer = Color(0xFF330000),
    onPrimary = HighContrastText,
    onSecondary = HighContrastText,
    onTertiary = HighContrastText,
    onBackground = HighContrastBackground,
    onSurface = HighContrastBackground,
    onSurfaceVariant = HighContrastBackground,
    onError = HighContrastText,
    outline = HighContrastBackground,
    outlineVariant = Color(0xFF999999),
    scrim = Color(0xCC000000),
    inverseSurface = HighContrastBackground,
    inverseOnSurface = HighContrastText,
    inversePrimary = HighContrastPrimary,
    surfaceTint = Color(0xFF66CCCC),
    primaryContainer = Color(0xFF003333),
    onPrimaryContainer = HighContrastBackground,
    secondaryContainer = Color(0xFF003300),
    onSecondaryContainer = HighContrastBackground,
    tertiaryContainer = Color(0xFF003300),
    onTertiaryContainer = HighContrastBackground
)

@Composable
fun EarthMaxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to maintain environmental theme consistency
    highContrast: Boolean = false, // New parameter for high contrast mode
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrast && darkTheme -> HighContrastDarkColorScheme
        highContrast && !darkTheme -> HighContrastLightColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}