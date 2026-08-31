package com.pixelpal.app.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pixelpal.app.animation.AnimationState

/**
 * Modern Lottie-driven pet rendering component.
 *
 * Supports dynamic state machines, continuous vector transitions, and
 * graceful fallback to vector drawables / static sprites.
 */
@Composable
fun LottiePetView(
    petType: String,
    animationState: AnimationState,
    size: Dp = 200.dp,
    speed: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lottieRawRes = animationState.getLottieRawResId(petType, context)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (lottieRawRes != 0) {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(lottieRawRes)
            )
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = if (animationState.loops) LottieConstants.IterateForever else 1,
                speed = speed
            )

            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(size)
            )
        } else {
            // Graceful fallback to vector drawable
            val drawableRes = animationState.getDrawableResId(petType, context).let { res ->
                if (res != 0) res
                else AnimationState.IDLE.getDrawableResId(petType, context)
            }

            if (drawableRes != 0) {
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = "$petType companion in $animationState state",
                    modifier = Modifier.size(size)
                )
            }
        }
    }
}
