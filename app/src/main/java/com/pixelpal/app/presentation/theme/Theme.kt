package com.pixelpal.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PixelPrimary,
    onPrimary = PixelOnDark,
    primaryContainer = PixelDarkSurfaceVariant,
    secondary = PixelSecondary,
    onSecondary = PixelOnDark,
    secondaryContainer = PixelSecondaryVariant,
    tertiary = PixelAccent,
    onTertiary = PixelOnDark,
    background = PixelDarkBackground,
    onBackground = PixelOnDark,
    surface = PixelDarkSurface,
    onSurface = PixelOnDark,
    surfaceVariant = PixelDarkSurfaceVariant,
    onSurfaceVariant = PixelOnDarkMuted,
    error = PixelError,
    onError = PixelOnDark,
    outline = PixelBubbleBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PixelLightPrimary,
    onPrimary = PixelLightSurface,
    primaryContainer = PixelLightSurfaceVariant,
    secondary = PixelLightSecondary,
    onSecondary = PixelLightSurface,
    background = PixelLightBackground,
    onBackground = PixelLightOnLight,
    surface = PixelLightSurface,
    onSurface = PixelLightOnLight,
    surfaceVariant = PixelLightSurfaceVariant,
    onSurfaceVariant = PixelLightOnLightMuted,
    error = PixelError,
    onError = PixelLightSurface,
    outline = PixelLightPrimary
)

@Composable
fun PixelPalTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PixelPalTypography,
        shapes = PixelPalShapes,
        content = content
    )
}