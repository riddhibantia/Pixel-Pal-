package com.pixelpal.app.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.domain.model.AvatarOptions
import com.pixelpal.app.domain.model.AvatarSlot
import com.pixelpal.app.domain.model.Companion
import java.io.File

/**
 * Renders a generated avatar combo (`pet_avatar_<hash>_<state>.json` from
 * the in-app combo cache dir). Unknown/missing combo -> base cat via
 * [PetRenderer]. Aura renders as a Compose ring behind the Lottie.
 */
@Composable
fun AvatarComboView(
    companion: Companion,
    animationState: AnimationState,
    comboDir: File?,
    size: Dp = 170.dp,
    modifier: Modifier = Modifier
) {
    val eyes = AvatarOptions.fromId(companion.eyeStyle)?.id ?: "classic"
    val ears = AvatarOptions.fromId(companion.earStyle)?.id ?: "pointy-cat"
    val headwear = AvatarOptions.fromId(companion.hatId)?.id ?: "none-headwear"
    val scarf = AvatarOptions.fromId(companion.outfitId)?.id ?: "none-scarf"
    val aura = AvatarOptions.fromId(companion.accessoryId)?.id ?: "none-aura"
    val hash = listOf(eyes, ears, headwear, scarf).joinToString("|").hashCode().toString(36)
        .replace("-", "n")
    val comboFile = comboDir?.resolve("pet_avatar_${hash}_${animationState.stateName}.json")
    val auraColor = when (aura) {
        "gold-sparkle" -> androidx.compose.ui.graphics.Color(0xFFF6C453)
        "pink-glow" -> androidx.compose.ui.graphics.Color(0xFFEC407A)
        else -> null
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (auraColor != null) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
                drawCircle(color = auraColor, alpha = 0.18f, radius = size.toPx() / 2)
            }
        }
        if (comboFile != null && comboFile.exists()) {
            key(hash, animationState) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.JsonString(comboFile.readText())
                )
                if (composition != null) {
                    val progress by animateLottieCompositionAsState(
                        composition = composition,
                        iterations = if (animationState.loops) LottieConstants.IterateForever else 1
                    )
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier.size(size)
                    )
                } else {
                    PetRenderer(
                        petType = companion.effectiveSpecies,
                        animationState = animationState,
                        size = size
                    )
                }
            }
        } else {
            PetRenderer(
                petType = companion.effectiveSpecies,
                animationState = animationState,
                size = size
            )
        }
    }
}
