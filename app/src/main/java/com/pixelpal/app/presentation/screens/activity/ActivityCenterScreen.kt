package com.pixelpal.app.presentation.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.domain.model.ActivityEvent
import com.pixelpal.app.domain.model.ActivityType
import com.pixelpal.app.presentation.components.AppTopBar
import com.pixelpal.app.presentation.components.EmptyState
import com.pixelpal.app.presentation.components.GroupDivider
import com.pixelpal.app.presentation.theme.Radius
import com.pixelpal.app.presentation.theme.Spacing
import java.util.concurrent.TimeUnit

@Composable
fun ActivityCenterScreen(
    navController: NavController,
    viewModel: ActivityCenterViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState()
    val companion by viewModel.companion.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Activity", onBack = { navController.popBackStack() })

        // Companion filter chips (All + one per companion)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { viewModel.filterBy(null) },
                label = { Text("All") }
            )
            companion?.let { c ->
                FilterChip(
                    selected = selectedFilter == c.id,
                    onClick = { viewModel.filterBy(c.id) },
                    label = { Text(c.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        if (events.isEmpty()) {
            EmptyState(
                title = "No activity yet",
                message = "Meaningful moments — bond milestones, completed tasks and " +
                    "reminders, agent updates — will appear here.",
                icon = Icons.Default.History,
                modifier = Modifier.weight(1f)
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal)
                    .padding(bottom = Spacing.lg)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        events.forEachIndexed { index, event ->
                            if (index > 0) {
                                GroupDivider()
                            }
                            ActivityEventRow(event = event)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityEventRow(event: ActivityEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = eventIcon(event.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            event.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = relativeTimestamp(event.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}



private fun eventIcon(type: ActivityType): ImageVector = when (type) {
    ActivityType.BOND_LEVEL_UP -> Icons.AutoMirrored.Filled.TrendingUp
    ActivityType.REMINDER_COMPLETED -> Icons.Default.Alarm
    ActivityType.TASK_ADDED, ActivityType.TASK_COMPLETED -> Icons.Default.Checklist
    ActivityType.AGENT_CHECKED, ActivityType.AGENT_STATUS_CHANGED -> Icons.Default.SmartToy
    ActivityType.COMPANION_ARCHIVED -> Icons.Default.Archive
    ActivityType.COMPANION_RESTORED -> Icons.Default.Restore
    else -> Icons.Default.Pets
}

private fun relativeTimestamp(time: Long): String {
    val diff = System.currentTimeMillis() - time
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        TimeUnit.MILLISECONDS.toHours(diff) < 24 -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
        TimeUnit.MILLISECONDS.toDays(diff) == 1L -> "Yesterday"
        else -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
    }
}