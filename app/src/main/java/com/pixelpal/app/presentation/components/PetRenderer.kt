package com.pixelpal.app.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixelpal.app.animation.AnimationState

@Composable
fun PetRenderer(
    petType: String,
    animationState: AnimationState,
    size: Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val drawableRes = animationState.getDrawableResId(petType, context)

    if (drawableRes != 0) {
        AsyncImage(
            model = drawableRes,
            contentDescription = "Your PixelPal Companion",
            modifier = modifier.size(size)
        )
    }
}
