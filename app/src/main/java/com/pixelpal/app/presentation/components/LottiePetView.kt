package com.pixelpal.app.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.LottieDynamicProperties
import com.airbnb.lottie.compose.LottieDynamicProperty
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.model.KeyPath
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.domain.model.PetTint

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
    modifier: Modifier = Modifier,
    /** Null = keep the baked body color. Non-null recolors body+ear layers. */
    bodyColor: Color? = null,
    /** Null = keep baked blush/sparkles. Non-null recolors them (pattern). */
    accentColor: Color? = null
) {
    val context = LocalContext.current
    val lottieRawRes = animationState.getLottieRawResId(petType, context)

    // Plain remember (not the remember*DynamicProperty composables): the
    // property count varies with nullability, which would break Compose
    // call-order rules. LottieDynamicProperty's constructor is public.
    val dynamicProperties = remember(bodyColor, accentColor) {
        LottieDynamicProperties(
            buildList {
                bodyColor?.let { c ->
                    val filter = SimpleColorFilter(c.toArgb())
                    PetTint.BODY_LAYERS.forEach { layer ->
                        add(
                            LottieDynamicProperty<android.graphics.ColorFilter>(
                                LottieProperty.COLOR_FILTER,
                                KeyPath(layer, "**"),
                                filter
                            )
                        )
                    }
                }
                accentColor?.let { c ->
                    val filter = SimpleColorFilter(c.toArgb())
                    PetTint.ACCENT_LAYERS.forEach { layer ->
                        add(
                            LottieDynamicProperty<android.graphics.ColorFilter>(
                                LottieProperty.COLOR_FILTER,
                                KeyPath(layer, "**"),
                                filter
                            )
                        )
                    }
                }
            }
        )
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
                        dynamicProperties = dynamicProperties,
                        modifier = Modifier.size(size)
                    )
                }
            }
        }
    }
}
