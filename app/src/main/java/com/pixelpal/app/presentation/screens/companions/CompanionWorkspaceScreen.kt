package com.pixelpal.app.presentation.screens.companions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.domain.model.AgentConfig
import com.pixelpal.app.domain.model.AgentStatus
import com.pixelpal.app.domain.model.CompanionRole
import com.pixelpal.app.domain.model.Personality
import com.pixelpal.app.domain.model.Task
import com.pixelpal.app.presentation.components.AppTextField
import com.pixelpal.app.presentation.components.AppTopBar
import com.pixelpal.app.presentation.components.PetRenderer
import com.pixelpal.app.presentation.components.PrimaryButton
import com.pixelpal.app.presentation.components.SectionHeader
import com.pixelpal.app.presentation.navigation.Screen
import com.pixelpal.app.presentation.theme.Radius
import com.pixelpal.app.presentation.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

@Composable
fun CompanionWorkspaceScreen(
    navController: NavController,
    viewModel: CompanionWorkspaceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val checkingAgent by viewModel.checkingAgent.collectAsState()

    val state = uiState
    if (state.loading || state.companion == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val companion = state.companion

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Workspace", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(bottom = Spacing.lg)
        ) {
            HeaderCard(companion = companion, isActive = uiState.isActive, bond = uiState.bond, viewModel = viewModel)

            Spacer(modifier = Modifier.height(Spacing.md))

            // Role-aware sections: only what's relevant to this companion's role.
            when (companion.role) {
                CompanionRole.GENERAL -> {
                    BondSection(bond = uiState.bond)
                    Spacer(modifier = Modifier.height(Spacing.md))
                    uiState.personality?.let { personality ->
                        PersonalitySection(personality = personality)
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }
                }
                CompanionRole.TASK -> {
                    TasksSection(tasks = uiState.tasks, viewModel = viewModel)
                    Spacer(modifier = Modifier.height(Spacing.md))
                }
                CompanionRole.REMINDER -> {
                    RemindersSection(
                        reminders = uiState.reminders,
                        onAddReminder = {
                            navController.navigate(Screen.reminderForCompanion(companion.id))
                        }
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                }
                CompanionRole.AI_AGENT -> {
                    AgentSection(
                        config = uiState.agentConfig,
                        status = uiState.agentStatus,
                        checking = checkingAgent,
                        onSave = viewModel::saveAgentConfig,
                        onCheckNow = viewModel::refreshAgentStatus
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                }
                CompanionRole.CUSTOM -> {
                    CustomNotesSection(
                        description = companion.description,
                        viewModel = viewModel
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                }
            }

            ActivitySection(activities = uiState.recentActivity)
        }
    }
}

@Composable
private fun HeaderCard(
    companion: com.pixelpal.app.domain.model.Companion,
    isActive: Boolean,
    bond: com.pixelpal.app.domain.model.Bond?,
    viewModel: CompanionWorkspaceViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(Radius.large)
                    ),
                contentAlignment = Alignment.Center
            ) {
                PetRenderer(
                    petType = companion.petType,
                    animationState = AnimationState.HAPPY,
                    size = 72.dp
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = companion.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Toggle favorite",
                            tint = if (companion.isFavorite) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                Text(
                    text = "${companion.role.displayName} · ${petLabel(companion.petType)}" +
                        (bond?.let { " · Bond Lvl ${it.level}" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                if (isActive) {
                    Text(
                        text = "Active companion",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    TextButton(onClick = viewModel::setActive) {
                        Text("Make active")
                    }
                }
            }

            TextButton(onClick = viewModel::archive) {
                Text(
                    text = "Archive",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun BondSection(bond: com.pixelpal.app.domain.model.Bond?) {
    SectionHeader(title = "Bond")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Level ${bond?.level ?: 0}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${bond?.totalInteractions ?: 0} interactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            LinearProgressIndicator(
                progress = { (bond?.level ?: 0) / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "${bond?.tapsToday ?: 0} taps · ${bond?.feedsToday ?: 0} feeds today · ${bond?.streakDays ?: 0}-day streak",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PersonalitySection(personality: Personality) {
    SectionHeader(title = "Personality")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            val traits = listOf(
                "Friendliness" to personality.friendliness,
                "Curiosity" to personality.curiosity,
                "Playfulness" to personality.playfulness,
                "Confidence" to personality.confidence
            )
            traits.forEachIndexed { index, (label, value) ->
                if (index > 0) Spacer(modifier = Modifier.height(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(96.dp)
                    )
                    LinearProgressIndicator(
                        progress = { value.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RemindersSection(
    reminders: List<com.pixelpal.app.domain.model.Reminder>,
    onAddReminder: () -> Unit
) {
    SectionHeader(title = "Reminders")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            if (reminders.isEmpty()) {
                Text(
                    text = "No pending reminders for this companion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                reminders.forEachIndexed { index, reminder ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))
            PrimaryButton(
                text = "Add Reminder",
                onClick = onAddReminder,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TasksSection(
    tasks: List<Task>,
    viewModel: CompanionWorkspaceViewModel
) {
    var newTask by remember { mutableStateOf("") }

    SectionHeader(title = "Tasks")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppTextField(
                    value = newTask,
                    onValueChange = { newTask = it },
                    label = "Add a task",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                TextButton(
                    onClick = {
                        viewModel.addTask(newTask)
                        newTask = ""
                    },
                    enabled = newTask.isNotBlank()
                ) {
                    Text("Add")
                }
            }

            if (tasks.isEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "No tasks yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                tasks.forEachIndexed { index, task ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(Spacing.xs))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleTask(task) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = task.isDone, onCheckedChange = { viewModel.toggleTask(task) })
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                            color = if (task.isDone) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentSection(
    config: AgentConfig?,
    status: AgentStatus?,
    checking: Boolean,
    onSave: (Boolean, String, Long) -> Unit,
    onCheckNow: () -> Unit
) {
    SectionHeader(title = "AI Agent")

    var endpoint by remember { mutableStateOf(config?.endpointUrl ?: "") }
    var enabled by remember { mutableStateOf(config?.enabled ?: false) }
    var interval by remember { mutableStateOf(config?.pollIntervalMinutes ?: 15L) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            AppTextField(
                value = endpoint,
                onValueChange = { endpoint = it },
                label = "Status endpoint URL",
                placeholder = "https://example.com/agent/status"
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Polling enabled", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Checks every $interval min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        onSave(enabled, endpoint, interval)
                    }
                ) {
                    Text("Save")
                }
                TextButton(
                    onClick = onCheckNow,
                    enabled = !checking
                ) {
                    Text(if (checking) "Checking…" else "Check now")
                }
            }

            status?.let {
                Spacer(modifier = Modifier.height(Spacing.sm))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = statusColor(status),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Column {
                        Text(
                            text = status.state.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        status.message?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomNotesSection(
    description: String?,
    viewModel: CompanionWorkspaceViewModel
) {
    var notes by remember { mutableStateOf(description ?: "") }

    SectionHeader(title = "Custom Purpose")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            AppTextField(
                value = notes,
                onValueChange = { if (it.length <= 140) notes = it },
                label = "What is this companion for?",
                placeholder = "Describe its purpose or notes"
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            TextButton(
                onClick = { viewModel.setDescription(notes) },
                enabled = notes != description
            ) {
                Text("Save notes")
            }
        }
    }
}

@Composable
private fun ActivitySection(activities: List<com.pixelpal.app.domain.model.ActivityEvent>) {
    SectionHeader(title = "Activity")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            if (activities.isEmpty()) {
                Text(
                    text = "No activity yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                activities.forEachIndexed { index, event ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            event.description?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = timeFormat.format(Date(event.createdAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun statusColor(status: AgentStatus): androidx.compose.ui.graphics.Color {
    return if (status.state.needsAttention) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.primary
}

private fun petLabel(petType: String): String =
    com.pixelpal.app.domain.model.PetType.fromId(petType).displayName