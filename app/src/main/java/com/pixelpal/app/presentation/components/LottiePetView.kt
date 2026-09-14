package com.pixelpal.app.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
 * Modern Lottie-driven pet rendering component (primary renderer).
 *
 * Lottie `res/raw` first, vector drawable fallback while loading or when
 * no Lottie file exists for the state. `key` on the raw id restarts
 * one-shot animations every time the state is (re-)triggered.
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
    // Resolved once: drawable fallback while the composition loads and
    // when no Lottie file exists for this state.
    val drawableRes = animationState.getDrawableResId(petType, context).let { res ->
        if (res != 0) res
        else AnimationState.IDLE.getDrawableResId(petType, context)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (lottieRawRes != 0) {
            key(lottieRawRes, animationState) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(lottieRawRes)
                )
                if (composition != null) {
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
                } else if (drawableRes != 0) {
                    Image(
                        painter = painterResource(id = drawableRes),
                        contentDescription = "$petType companion in $animationState state",
                        modifier = Modifier.size(size)
                    )
                }
            }
        } else if (drawableRes != 0) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = "$petType companion in $animationState state",
                modifier = Modifier.size(size)
            )
        }
    }
}
