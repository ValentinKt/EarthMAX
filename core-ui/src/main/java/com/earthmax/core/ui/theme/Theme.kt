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
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0BEC5),
    onError = Color.Black,
    outline = Color(0xFF4A5568),
    outlineVariant = Color(0xFF2D3748),
    scrim = OverlayDark,
    inverseSurface = SurfaceLight,
    inverseOnSurface = Color.Black,
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
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF455A64),
    onError = Color.White,
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = OverlayDark,
    inverseSurface = SurfaceDark,
    inverseOnSurface = Color.White,
    inversePrimary = Teal80,
    surfaceTint = Teal40,
    // Enhanced semantic colors
    primaryContainer = Color(0xFFE0F2F1),
    onPrimaryContainer = Color(0xFF002020),
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF001F24),
    tertiaryContainer = Color(0xFFE8F5E8),
    onTertiaryContainer = Color(0xFF0D2818)
)

@Composable
fun EarthMaxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to maintain environmental theme consistency
    content: @Composable () -> Unit
) {
    val colorScheme = when {
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