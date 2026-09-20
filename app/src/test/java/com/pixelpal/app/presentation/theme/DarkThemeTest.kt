package com.pixelpal.app.presentation.theme

import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertTrue

/**
 * Guards the modern neutral dark theme: surfaces and text must stay
 * low-saturation (no yellow/brown tint). The gold accent is exempt.
 */
class DarkThemeTest {

    private fun maxChannelSpread(color: androidx.compose.ui.graphics.Color): Float {
        val r = color.red * 255
        val g = color.green * 255
        val b = color.blue * 255
        return maxOf(r, g, b) - minOf(r, g, b)
    }

    private fun luminance(color: androidx.compose.ui.graphics.Color): Float =
        0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue

    @Test
    fun darkSurfaces_areNeutral() {
        listOf(DarkPalette.Background, DarkPalette.Surface, DarkPalette.SurfaceElevated).forEach {
            assertTrue(
                maxChannelSpread(it) <= 14f,
                "Dark surface $it has a color tint (spread ${maxChannelSpread(it)})"
            )
        }
    }

    @Test
    fun darkText_isNeutral() {
        assertTrue(maxChannelSpread(DarkPalette.TextPrimary) <= 14f, "Primary text is tinted")
        assertTrue(maxChannelSpread(DarkPalette.TextSecondary) <= 14f, "Secondary text is tinted")
    }

    @Test
    fun darkSurfaces_elevateUpwards() {
        assertTrue(
            luminance(DarkPalette.Surface) > luminance(DarkPalette.Background),
            "Surface must be lighter than background"
        )
        assertTrue(
            luminance(DarkPalette.SurfaceElevated) > luminance(DarkPalette.Surface),
            "Elevated surface must be lighter than surface"
        )
    }

    @Test
    fun darkPrimary_staysGoldAccent() {
        // Brand accent: strong red+green, weak blue.
        assertTrue(DarkPalette.Primary.red > 0.85f, "Primary should stay a vivid gold")
        assertTrue(DarkPalette.Primary.blue < 0.45f, "Primary should stay a vivid gold")
    }

    @Test
    fun darkOutline_isNeutral() {
        assertTrue(maxChannelSpread(DarkPalette.Outline) <= 14f, "Outline is tinted")
        assertTrue(maxChannelSpread(DarkPalette.Divider) <= 14f, "Divider is tinted")
    }
}
