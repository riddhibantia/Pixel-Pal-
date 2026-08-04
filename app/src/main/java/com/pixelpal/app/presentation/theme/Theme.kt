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

private val SpringColorScheme = lightColorScheme(
    primary = SpringPrimary,
    onPrimary = SpringOn,
    primaryContainer = SpringSurfaceVariant,
    secondary = SpringSecondary,
    onSecondary = SpringOn,
    tertiary = SpringAccent,
    background = SpringBackground,
    onBackground = SpringOn,
    surface = SpringSurface,
    onSurface = SpringOn,
    surfaceVariant = SpringSurfaceVariant,
    onSurfaceVariant = SpringSecondary,
    error = PixelError,
    onError = SpringSurface,
    outline = SpringPrimary
)

private val AutumnColorScheme = darkColorScheme(
    primary = AutumnPrimary,
    onPrimary = AutumnOnDarkPrimary,
    primaryContainer = AutumnSurfaceVariant,
    secondary = AutumnSecondary,
    onSecondary = AutumnOn,
    tertiary = AutumnAccent,
    background = AutumnBackground,
    onBackground = AutumnOn,
    surface = AutumnSurface,
    onSurface = AutumnOn,
    surfaceVariant = AutumnSurfaceVariant,
    onSurfaceVariant = AutumnOn,
    error = PixelError,
    onError = AutumnOnDarkPrimary,
    outline = AutumnPrimary
)

@Composable
fun PixelPalTheme(
    theme: String = "dark",
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme.lowercase()) {
        "light" -> LightColorScheme
        "spring" -> SpringColorScheme
        "autumn" -> AutumnColorScheme
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PixelPalTypography,
        shapes = PixelPalShapes,
        content = content
    )
}