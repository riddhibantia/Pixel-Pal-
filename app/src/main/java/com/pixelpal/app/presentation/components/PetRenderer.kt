package com.pixelpal.app.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelpal.app.animation.AnimationState

/**
 * Renders the companion pet, Lottie-only via [LottiePetView]
 * (`res/raw/pet_{type}_{state}.json`).
 * Optional tints recolor body/ears (color) and blush/sparkles (pattern).
 * Existing callers pass nothing and keep baked art.
 */
@Composable
fun PetRenderer(
    petType: String,
    animationState: AnimationState,
    size: Dp = 200.dp,
    modifier: Modifier = Modifier,
    bodyColor: Color? = null,
    accentColor: Color? = null
) {
    LottiePetView(
        petType = petType,
        animationState = animationState,
        size = size,
        modifier = modifier,
        bodyColor = bodyColor,
        accentColor = accentColor
    )
}
