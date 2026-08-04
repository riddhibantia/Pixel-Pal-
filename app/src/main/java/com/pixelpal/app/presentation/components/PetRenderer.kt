package com.pixelpal.app.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelpal.app.animation.AnimationState

/**
 * Renders the companion pet using native vector drawables.
 *
 * Resolution is fully data-driven:
 *   PetType + AnimationState  →  R.drawable.pet_{type}_{state}
 *
 * Falls back to IDLE if the requested state has no drawable.
 */
@Composable
fun PetRenderer(
    petType: String,
    animationState: AnimationState,
    size: Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val drawableRes = animationState.getDrawableResId(petType, context).let { res ->
        if (res != 0) res
        else AnimationState.IDLE.getDrawableResId(petType, context)
    }

    if (drawableRes != 0) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = "Your PixelPal Companion",
            modifier = modifier.size(size)
        )
    }
}
