package com.pixelpal.app.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.pixelpal.app.presentation.theme.CompanionColors

/**
 * Maps a companion's chosen color/pattern names to real render tints.
 * Body tint recolors the Lottie "Pixel Body" + ear layers; accent tint
 * (pattern) recolors the cheek/sparkle layers. `null` = keep baked art.
 */
object PetTint {

    /** Layers that carry the main body color in every pet_*.json file. */
    val BODY_LAYERS = listOf("Pixel Body", "Left Ear", "Right Ear")

    /** Layers that carry the pattern accent (blush + sparkles). */
    val ACCENT_LAYERS = listOf(
        "Left Cheek", "Right Cheek",
        "Sparkle 1", "Sparkle 2", "Sparkle 3"
    )

    fun bodyColorFor(colorName: String): Color =
        CompanionColors.forName(colorName)

    /** Matches the pattern preview swatches in CustomizeScreen. */
    fun accentColorFor(pattern: String): Color? = when (pattern.lowercase()) {
        "stripes" -> Color(0xFF7E57C2)
        "spots" -> Color(0xFFEF6C00)
        "patches" -> Color(0xFF2E7D32)
        else -> null // "plain" keeps the baked blush/sparkles
    }

    fun bodyArgbFor(colorName: String): Int =
        bodyColorFor(colorName).toArgb()

    fun accentArgbFor(pattern: String): Int? =
        accentColorFor(pattern)?.toArgb()
}
