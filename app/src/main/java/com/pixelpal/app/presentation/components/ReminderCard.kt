package com.pixelpal.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.presentation.theme.Radius
import com.pixelpal.app.presentation.theme.Sizing
import com.pixelpal.app.presentation.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Light reminder card: clear title/time hierarchy, compact Complete + Delete
 * actions. Uses centralized theme tokens and the standard radius scale.
 */
@Composable
fun ReminderCard(
    reminder: Reminder,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(reminder.triggerTime))
    val dayLabel = dayLabelFor(reminder.triggerTime)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.large),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                reminder.message?.takeIf { it.isNotEmpty() }?.let { message ->
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Surface(
                        shape = RoundedCornerShape(Radius.small),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (reminder.status == "PENDING") {
                    IconButton(
                        onClick = onComplete,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.medium))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Complete",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.medium))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Sizing.icon)
                    )
                }
            }
        }
    }
}

private fun dayLabelFor(triggerTime: Long): String {
    val target = Calendar.getInstance().apply { timeInMillis = triggerTime }
    val today = Calendar.getInstance()
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return when {
        target.before(startOfToday) -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(triggerTime))
        target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
        target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) + 1 -> "Tomorrow"
        else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(triggerTime))
    }
}