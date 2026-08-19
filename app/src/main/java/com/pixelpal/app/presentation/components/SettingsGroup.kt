package com.pixelpal.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelpal.app.presentation.theme.Radius
import com.pixelpal.app.presentation.theme.Spacing

/**
 * Groups related settings rows onto one standard surface.
 *
 * Rows are placed directly inside; use [GroupDivider] between rows (and only
 * between rows) so groups stay visually consistent.
 *
 * Semantic: uses [MaterialTheme.colorScheme.surface] — a standard grouped
 * surface, not an elevated card.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.large),
        color = MaterialTheme.colorScheme.surface,
        contentColor = contentColorFor(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(vertical = Spacing.xs)) {
            content()
        }
    }
}

/** Subtle divider for use between rows inside a [SettingsGroup]. */
@Composable
fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = Spacing.md),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
