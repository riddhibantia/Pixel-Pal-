package com.pixelpal.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkPalette.Primary,
    onPrimary = DarkPalette.OnPrimary,
    primaryContainer = DarkPalette.PrimaryContainer,
    onPrimaryContainer = DarkPalette.OnPrimaryContainer,
    secondary = DarkPalette.Secondary,
    onSecondary = DarkPalette.OnSecondary,
    secondaryContainer = DarkPalette.SecondaryContainer,
    onSecondaryContainer = DarkPalette.OnSecondaryContainer,
    tertiary = DarkPalette.Secondary,
    onTertiary = DarkPalette.OnSecondary,
    tertiaryContainer = DarkPalette.SecondaryContainer,
    onTertiaryContainer = DarkPalette.OnSecondaryContainer,
    background = DarkPalette.Background,
    onBackground = DarkPalette.TextPrimary,
    surface = DarkPalette.Surface,
    onSurface = DarkPalette.TextPrimary,
    surfaceVariant = DarkPalette.SurfaceElevated,
    onSurfaceVariant = DarkPalette.TextSecondary,
    outline = DarkPalette.Outline,
    outlineVariant = DarkPalette.Divider,
    error = DarkPalette.Error,
    onError = DarkPalette.OnError
)

private val LightColorScheme = lightColorScheme(
    primary = LightPalette.Primary,
    onPrimary = LightPalette.OnPrimary,
    primaryContainer = LightPalette.PrimaryContainer,
    onPrimaryContainer = LightPalette.OnPrimaryContainer,
    secondary = LightPalette.Secondary,
    onSecondary = LightPalette.OnSecondary,
    secondaryContainer = LightPalette.SecondaryContainer,
    onSecondaryContainer = LightPalette.OnSecondaryContainer,
    tertiary = LightPalette.Secondary,
    onTertiary = LightPalette.OnSecondary,
    tertiaryContainer = LightPalette.SecondaryContainer,
    onTertiaryContainer = LightPalette.OnSecondaryContainer,
    background = LightPalette.Background,
    onBackground = LightPalette.TextPrimary,
    surface = LightPalette.Surface,
    onSurface = LightPalette.TextPrimary,
    surfaceVariant = LightPalette.SurfaceElevated,
    onSurfaceVariant = LightPalette.TextSecondary,
    outline = LightPalette.Outline,
    outlineVariant = LightPalette.Divider,
    error = LightPalette.Error,
    onError = LightPalette.OnError
)

private val SpringColorScheme = lightColorScheme(
    primary = SpringPalette.Primary,
    onPrimary = SpringPalette.OnPrimary,
    primaryContainer = SpringPalette.PrimaryContainer,
    onPrimaryContainer = SpringPalette.OnPrimaryContainer,
    secondary = SpringPalette.Secondary,
    onSecondary = SpringPalette.OnSecondary,
    secondaryContainer = SpringPalette.SecondaryContainer,
    onSecondaryContainer = SpringPalette.OnSecondaryContainer,
    tertiary = SpringPalette.Secondary,
    onTertiary = SpringPalette.OnSecondary,
    tertiaryContainer = SpringPalette.SecondaryContainer,
    onTertiaryContainer = SpringPalette.OnSecondaryContainer,
    background = SpringPalette.Background,
    onBackground = SpringPalette.TextPrimary,
    surface = SpringPalette.Surface,
    onSurface = SpringPalette.TextPrimary,
    surfaceVariant = SpringPalette.SurfaceElevated,
    onSurfaceVariant = SpringPalette.TextSecondary,
    outline = SpringPalette.Outline,
    outlineVariant = SpringPalette.Divider,
    error = SpringPalette.Error,
    onError = SpringPalette.OnError
)

private val AutumnColorScheme = lightColorScheme(
    primary = AutumnPalette.Primary,
    onPrimary = AutumnPalette.OnPrimary,
    primaryContainer = AutumnPalette.PrimaryContainer,
    onPrimaryContainer = AutumnPalette.OnPrimaryContainer,
    secondary = AutumnPalette.Secondary,
    onSecondary = AutumnPalette.OnSecondary,
    secondaryContainer = AutumnPalette.SecondaryContainer,
    onSecondaryContainer = AutumnPalette.OnSecondaryContainer,
    tertiary = AutumnPalette.Secondary,
    onTertiary = AutumnPalette.OnSecondary,
    tertiaryContainer = AutumnPalette.SecondaryContainer,
    onTertiaryContainer = AutumnPalette.OnSecondaryContainer,
    background = AutumnPalette.Background,
    onBackground = AutumnPalette.TextPrimary,
    surface = AutumnPalette.Surface,
    onSurface = AutumnPalette.TextPrimary,
    surfaceVariant = AutumnPalette.SurfaceElevated,
    onSurfaceVariant = AutumnPalette.TextSecondary,
    outline = AutumnPalette.Outline,
    outlineVariant = AutumnPalette.Divider,
    error = AutumnPalette.Error,
    onError = AutumnPalette.OnError
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
