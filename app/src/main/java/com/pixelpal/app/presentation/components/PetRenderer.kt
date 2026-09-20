package com.pixelpal.app.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelpal.app.animation.AnimationState

/**
 * Renders the companion pet, Lottie-only via [LottiePetView]
 * (`res/raw/pet_{type}_{state}.json`).
 * Same signature as before — existing callers unchanged.
 */
@Composable
fun PetRenderer(
    petType: String,
    animationState: AnimationState,
    size: Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    LottiePetView(
        petType = petType,
        animationState = animationState,
        size = size,
        modifier = modifier
    )
}
