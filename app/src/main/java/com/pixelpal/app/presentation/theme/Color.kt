package com.pixelpal.app.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * PixelPal semantic color palettes.
 *
 * Screens and components must consume colors through [androidx.compose.material3.MaterialTheme]
 * colorScheme slots — never raw Color values. These palettes exist to feed Theme.kt mappings.
 *
 * Identity: modern neutral dark (near-black zinc surfaces, white text) with a
 * gold primary accent and purple secondary.
 */

/** Semantic state colors shared by every theme. */
val PixelSuccess = Color(0xFF6BCB77)
val PixelWarning = Color(0xFFFFD166)

object DarkPalette {
    val Background = Color(0xFF0F0F13)
    val Surface = Color(0xFF17171C)
    val SurfaceElevated = Color(0xFF1E1E25)

    val Primary = Color(0xFFF6C453)
    val OnPrimary = Color(0xFF1C1607)
    val PrimaryContainer = Color(0xFF3A2E10)
    val OnPrimaryContainer = Color(0xFFF8D47E)

    val Secondary = Color(0xFF8B6CF6)
    val OnSecondary = Color(0xFFF4F4F5)
    val SecondaryContainer = Color(0xFF2B2545)
    val OnSecondaryContainer = Color(0xFFCFC2FA)

    val TextPrimary = Color(0xFFF4F4F5)
    val TextSecondary = Color(0xFFA1A1AA)

    val Outline = Color(0xFF3F3F46)
    val Divider = Color(0xFF27272A)

    val Error = Color(0xFFFF6B6B)
    val OnError = Color(0xFF200D0D)
}

object LightPalette {
    val Background = Color(0xFFF8F1E5)
    val Surface = Color(0xFFFFF9EF)
    val SurfaceElevated = Color(0xFFFFFFFF)

    val Primary = Color(0xFFD99F2B)
    val OnPrimary = Color(0xFF2A1F16)
    val PrimaryContainer = Color(0xFFF3E1B8)
    val OnPrimaryContainer = Color(0xFF6B4E10)

    val Secondary = Color(0xFF7658C7)
    val OnSecondary = Color(0xFFFFF9EF)
    val SecondaryContainer = Color(0xFFE6DFF7)
    val OnSecondaryContainer = Color(0xFF4A3899)

    val TextPrimary = Color(0xFF2A1F16)
    val TextSecondary = Color(0xFF6F6254)

    val Outline = Color(0xFFCDBFAE)
    val Divider = Color(0xFFE7DCCB)

    val Error = Color(0xFFFF6B6B)
    val OnError = Color(0xFF2A1F16)
}

object SpringPalette {
    val Background = Color(0xFFF2F6EA)
    val Surface = Color(0xFFE8F0DE)
    val SurfaceElevated = Color(0xFFFAFCF5)

    val Primary = Color(0xFFD5A93A)
    val OnPrimary = Color(0xFF2A2515)
    val PrimaryContainer = Color(0xFFEDE3C0)
    val OnPrimaryContainer = Color(0xFF6B5716)

    val Secondary = Color(0xFF6D8E70)
    val OnSecondary = Color(0xFFFAFCF5)
    val SecondaryContainer = Color(0xFFD8E4D4)
    val OnSecondaryContainer = Color(0xFF3E5342)

    val TextPrimary = Color(0xFF283126)
    val TextSecondary = Color(0xFF657061)

    val Outline = Color(0xFFBFCBB7)
    val Divider = Color(0xFFDCE5D6)

    val Error = Color(0xFFFF6B6B)
    val OnError = Color(0xFF2A2515)
}

object AutumnPalette {
    val Background = Color(0xFFF4E4CF)
    val Surface = Color(0xFFEAD3B8)
    val SurfaceElevated = Color(0xFFFAEBDD)

    val Primary = Color(0xFFC98A2E)
    val OnPrimary = Color(0xFF2A1A0D)
    val PrimaryContainer = Color(0xFFEFD3A8)
    val OnPrimaryContainer = Color(0xFF6B4615)

    val Secondary = Color(0xFF80506A)
    val OnSecondary = Color(0xFFFAEBDD)
    val SecondaryContainer = Color(0xFFE5D0DA)
    val OnSecondaryContainer = Color(0xFF5A3648)

    val TextPrimary = Color(0xFF342318)
    val TextSecondary = Color(0xFF715E4D)

    val Outline = Color(0xFFC6AA8B)
    val Divider = Color(0xFFDEC8AC)

    val Error = Color(0xFFFF6B6B)
    val OnError = Color(0xFF2A1A0D)
}
