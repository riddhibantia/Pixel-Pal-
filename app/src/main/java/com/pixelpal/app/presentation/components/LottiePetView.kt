package com.pixelpal.app.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pixelpal.app.animation.AnimationState

/**
 * Lottie-only pet renderer (`res/raw/pet_{type}_{state}.json`).
 * No vector-drawable fallback: while the composition loads, nothing renders.
 * `key` on the raw id + state restarts one-shots on every (re-)trigger.
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
                }
            }
        }
    }
}
