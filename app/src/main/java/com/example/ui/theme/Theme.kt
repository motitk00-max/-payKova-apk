package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KovaColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = VoidBlack,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = CyberCyan,
    secondary = NeonViolet,
    onSecondary = Color.White,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = NeonViolet,
    tertiary = PulseBlue,
    onTertiary = VoidBlack,
    background = VoidBlack,
    onBackground = TextPrimary,
    surface = DarkCanvas,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = CriticalRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve distinct cyber identity
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KovaColorScheme,
        typography = Typography,
        content = content
    )
}

