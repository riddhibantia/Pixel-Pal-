package com.pixelpal.app.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.CompanionAppearance
import com.pixelpal.app.domain.model.PetTint

/**
 * Lottie-first avatar: every species renders its full 12-state set from
 * `res/raw/pet_{species}_{state}.json` via [PetRenderer]. The companion's
 * chosen color recolors body+ears, the pattern recolors blush/sparkles.
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
    PetRenderer(
        petType = appearance.species,
        animationState = expression,
        size = size,
        modifier = modifier
    )
}

