package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MixgraphColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DarkBg,
    primaryContainer = DarkSurfaceAlt,
    onPrimaryContainer = TextPrimary,
    secondary = NeonGreen,
    onSecondary = DarkBg,
    tertiary = NeonPurple,
    onTertiary = TextPrimary,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceAlt,
    onSurfaceVariant = TextSecondary,
    outline = TextMuted,
    error = Color(0xFFFF5252)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force premium dark mode as default
    dynamicColor: Boolean = false, // Force our custom high-fidelity branding
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MixgraphColorScheme,
        typography = Typography,
        content = content
    )
}
