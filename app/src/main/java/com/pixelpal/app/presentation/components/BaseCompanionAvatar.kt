package com.pixelpal.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.CompanionAppearance
import com.pixelpal.app.presentation.theme.CompanionColors

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
    // Increment 2: base color layer is now live. If no custom color is set,
    // delegate to the original vector so the cat stays pixel-identical.
    // When a baseColor is present, render the face with that color while
    // keeping eyes/highlights/blush/mouth exactly as in the original.
    val baseColor = appearance.baseColor
    if (baseColor.isNullOrBlank()) {
        PetRenderer(
            petType = appearance.species,
            animationState = expression,
            size = size,
            modifier = modifier
        )
    } else {
        Canvas(
            modifier = modifier.size(size)
        ) {
            val w = size.toPx()
            val scale = w / 120f
            drawContext.canvas.save()
            drawContext.canvas.scale(scale, scale)
            val faceColor = colorForName(baseColor)
            // Face rect with selected base color (the customizable layer)
            drawPath(
                path = PathParser.createPathFromPathData(
                    "M 30,30 L 90,30 L 90,90 L 30,90 Z"
                ).asComposePath(),
                color = faceColor
            )
            // Ears - same shape as original, tinted with face color (slightly darker for depth)
            val earColor = darker(faceColor, 0.9f)
            drawPath(
                path = PathParser.createPathFromPathData(
                    "M 30,15 L 45,15 L 45,30 L 30,30 Z"
                ).asComposePath(),
                color = earColor
            )
            drawPath(
                path = PathParser.createPathFromPathData(
                    "M 75,15 L 90,15 L 90,30 L 75,30 Z"
                ).asComposePath(),
                color = earColor
            )
            // Inner ear - keep original pink for now (not yet customizable)
            drawPath(
                path = PathParser.createPathFromPathData(
                    "M 34,19 L 41,19 L 41,26 L 34,26 Z"
                ).asComposePath(),
                color = Color(0xFFF8BBD0)
            )
            drawPath(
                path = PathParser.createPathFromPathData(
                    "M 79,19 L 86,19 L 86,26 L 79,26 Z"
                ).asComposePath(),
                color = Color(0xFFF8BBD0)
            )
            // Eyes - keep original dark and white highlights exactly
            drawPath(
                path = PathParser.createPathFromPathData(
                    "M 42,54 A 4,6 0 1,1 50,54 A 4,6 0 1,1 42,54 Z"
                ).asComposePath(),
                color = Color(0xFF18181D)
            )
            drawPath(
                path = PathParser.createPathFromPathData(
                    "M 44,51 A 1.5,2 0 1,1 47,51 A 1.5,2 0 1,1 44,51 Z"
                ).asComposePath(),
                color = Color.White
            )
            drawPath(
                path = PathParser.createPathFromPathData(
                    "M 70,54 A 4,6 0 1,1 78,54 A 4,6 0 1,1 70,54 Z"
                ).asComposePath(),
                color = Color(0xFF18181D)
            )
            drawPath(
                path = PathParser.createPathFromPathData(
                    "M 72,51 A 1.5,2 0 1,1 75,51 A 1.5,2 0 1,1 72,51 Z"
                ).asComposePath(),
                color = Color.White
            )
            // Blush - keep original
            drawPath(
                path = PathParser.createPathFromPathData(
                    "M 32,64 L 40,64 L 40,68 L 32,68 Z"
                ).asComposePath(),
                color = Color(0xFFF3A696)
            )
            drawPath(
                path = PathParser.createPathFromPathData(
                    "M 80,64 L 88,64 L 88,68 L 80,68 Z"
                ).asComposePath(),
                color = Color(0xFFF3A696)
            )
            // Nose - keep original
            drawPath(
                path = PathParser.createPathFromPathData(
                    "M 57,66 L 63,66 L 63,70 L 57,70 Z"
                ).asComposePath(),
                color = Color(0xFFF3A696)
            )
            // Mouth - small w under nose (keep original cute smile)
            drawPath(
                path = Path().apply {
                    moveTo(54f, 74f)
                    quadraticTo(57f, 77f, 60f, 74f)
                    quadraticTo(63f, 77f, 66f, 74f)
                },
                color = Color(0xFF3E2723),
                style = Stroke(
                    width = 1.8f,
                    cap = StrokeCap.Round
                )
            )
            drawContext.canvas.restore()
        }
    }
}

private fun colorForName(name: String): Color =
    CompanionColors.forName(name)

private fun darker(color: Color, factor: Float = 0.9f): Color {
    return Color(
        red = (color.red * factor).coerceIn(0f, 1f),
        green = (color.green * factor).coerceIn(0f, 1f),
        blue = (color.blue * factor).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}
