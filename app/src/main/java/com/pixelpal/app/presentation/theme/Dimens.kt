package com.pixelpal.app.presentation.theme

import androidx.compose.ui.unit.dp

/**
 * PixelPal spacing / sizing / radius scale.
 *
 * Use these tokens instead of arbitrary padding values so spacing stays
 * consistent across every screen.
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp

    /** Standard horizontal padding for screen content. */
    val screenHorizontal = 20.dp
}

object Radius {
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val xlarge = 24.dp
}

object Sizing {
    /** Standard icon size inside rows and buttons. */
    val icon = 24.dp

    /** Minimum touch target per accessibility guidance. */
    val touchMin = 48.dp

    /** Comfortable height for a settings row (56–64dp). */
    val settingsRow = 60.dp

    /** Standard button height. */
    val button = 52.dp
}
