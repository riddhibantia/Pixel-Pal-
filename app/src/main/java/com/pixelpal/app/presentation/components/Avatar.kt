package com.pixelpal.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelpal.app.presentation.theme.Spacing

/**
 * Cozy deterministic avatar derived from [seed] (the stored avatarSeed).
 *
 * Picks one of a small set of warm, PixelPal-flavored color/icon pairs —
 * no image upload required. The same seed always produces the same avatar.
 */
private data class AvatarStyle(val containerColorHex: Long, val icon: String)

private val avatarStyles = listOf(
    AvatarStyle(0xFFF6C453, "🐾"),
    AvatarStyle(0xFF8B6CF6, "⭐"),
    AvatarStyle(0xFFE07B4A, "🍁"),
    AvatarStyle(0xFF6D8E70, "🌿"),
    AvatarStyle(0xFFD98CB3, "🌸"),
    AvatarStyle(0xFF5ABFBF, "🫧"),
    AvatarStyle(0xFFC98A2E, "🌙"),
    AvatarStyle(0xFFB0563F, "🔥")
)

@Composable
fun PixelAvatar(
    seed: String,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val style = avatarStyles[
        (if (seed.isEmpty()) 0 else seed.hashCode().let { if (it < 0) -it else it }) % avatarStyles.size
    ]
    val bg = androidx.compose.ui.graphics.Color(style.containerColorHex)

    Box(
        modifier = modifier
            .size(size)
            .background(bg, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = style.icon,
            fontSize = (size.value * 0.45f).sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

/** Circular initial-based avatar used when a name is known but no seed. */
@Composable
fun InitialAvatar(
    name: String,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "P"
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
