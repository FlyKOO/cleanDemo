package com.example.cleanup.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// 深色主题配色方案
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = OnBackgroundDark,
    primaryContainer = PrimaryBlue,
    onPrimaryContainer = OnBackgroundDark,
    
    secondary = SuccessGreen,
    onSecondary = OnBackgroundDark,
    secondaryContainer = SuccessGreenAlpha,
    onSecondaryContainer = SuccessGreen,
    
    tertiary = WarningOrange,
    onTertiary = OnBackgroundDark,
    tertiaryContainer = WarningOrangeAlpha,
    onTertiaryContainer = WarningOrange,
    
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    
    error = ErrorRed,
    onError = OnBackgroundDark,
    errorContainer = ErrorRed.copy(alpha = 0.1f),
    onErrorContainer = ErrorRed,
    
    outline = OnSurfaceVariantDark,
    outlineVariant = OnSurfaceVariantDark.copy(alpha = 0.5f),
    
    scrim = BackgroundDark.copy(alpha = 0.8f)
)

@Composable
fun CleanupTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}