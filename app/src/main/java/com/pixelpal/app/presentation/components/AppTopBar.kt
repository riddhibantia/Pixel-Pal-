package com.pixelpal.app.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelpal.app.presentation.theme.Spacing

/**
 * The single top-bar style for the whole app.
 *
 * Primary screens: large title only.
 * Secondary screens: back arrow + title (used by Profile, Overlay Settings,
 * About, New Reminder, …).
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null
) {
    val isSecondary = onBack != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                start = if (isSecondary) Spacing.xs else Spacing.screenHorizontal,
                end = Spacing.screenHorizontal
            )
            .padding(vertical = if (isSecondary) Spacing.sm else Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSecondary) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (isSecondary) Spacing.xs else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = if (isSecondary) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.headlineMedium
                    },
                    fontWeight = if (isSecondary) FontWeight.SemiBold else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
