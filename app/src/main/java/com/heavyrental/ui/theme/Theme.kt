package com.heavyrental.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3a2500),
    background = Background,
    onBackground = Foreground,
    surface = Surface,
    onSurface = Foreground,
    surfaceVariant = Card,
    onSurfaceVariant = MutedForeground,
    outline = Border,
    error = RedAccent,
    onError = Color.Black,
    secondary = Color(0xFF888888),
    onSecondary = Foreground,
    tertiary = GreenAccent,
)

@Composable
fun HeavyRentalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = HeavyRentalTypography,
        content = content
    )
}
