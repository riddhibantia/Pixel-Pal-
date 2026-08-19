package com.pixelpal.app.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.pixelpal.app.presentation.theme.Sizing
import com.pixelpal.app.presentation.theme.Spacing

/**
 * The single reusable settings row.
 *
 * Variants:
 *  - navigation:  icon + title [+ description] + chevron
 *  - toggle:      icon + title + description + switch
 *  - selection:   icon + title + current value + chevron
 *  - action:      icon + title + chevron
 *  - destructive: red semantic styling
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    destructive: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    locked: Boolean = false
) {
    val contentColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val descriptionColor = if (destructive) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizing.settingsRow)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClickLabel = title) { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = if (destructive) 1f else 0.85f),
                modifier = Modifier.size(Sizing.icon)
            )
            Spacer(modifier = Modifier.width(Spacing.md))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (locked) {
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = descriptionColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = descriptionColor
                )
            }
        }

        if (value != null) {
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = descriptionColor
            )
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        when {
            trailing != null -> trailing()
            onClick != null -> Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Sizing.icon)
            )
        }
    }
}

/** Toggle variant with themed switch and accessible state description. */
@Composable
fun ToggleSettingsRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null
) {
    SettingsRow(
        title = title,
        icon = icon,
        description = description,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier.semantics(mergeDescendants = true) {
            stateDescription = if (checked) "On" else "Off"
        },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    )
}
