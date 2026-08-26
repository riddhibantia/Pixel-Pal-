package com.pixelpal.app.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.CompanionAppearance

/**
 * Phase 1 base avatar — renders ONLY the existing original PixelPal cat.
 *
 * This is the reusable foundation for future layers:
 *   Base Companion
 *     + Color Layer
 *     + Ear Layer
 *     + Fur Layer
 *     + Eye Layer
 *     + Expression Layer
 *     + Pattern Layer
 *
 * For now, every layer beyond [CompanionAppearance.species] is null and
 * intentionally ignored, so output is pixel-identical to the original.
 */
@Composable
fun BaseCompanionAvatar(
    companion: Companion,
    size: Dp = 170.dp,
    expression: AnimationState = AnimationState.HAPPY,
    modifier: Modifier = Modifier
) {
    BaseCompanionAvatar(
        appearance = CompanionAppearance.fromCompanion(companion),
        size = size,
        expression = expression,
        modifier = modifier
    )
}

@Composable
fun BaseCompanionAvatar(
    appearance: CompanionAppearance,
    size: Dp = 170.dp,
    expression: AnimationState = AnimationState.HAPPY,
    modifier: Modifier = Modifier
) {
    // Phase 1: delegate straight to the existing vector pipeline.
    // Future phases will branch on appearance.baseColor / earStyle / etc.
    // and draw tinted/patterned layers here while reusing this same call-site.
    PetRenderer(
        petType = appearance.species,
        animationState = expression,
        size = size,
        modifier = modifier
    )
}
