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
import com.pixelpal.app.domain.model.AgentConnection
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
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = "Workspace", onBack = { navController.popBackStack() })
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
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
            // ── Profile ──
            ProfileHeader(companion = companion, bond = state.bond, viewModel = viewModel)
            Spacer(modifier = Modifier.height(Spacing.sm))
            TextButton(onClick = { navController.navigate(Screen.Customize.route) }) {
                Text("Customize Companion")
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Bond & progress ──
            BondSection(bond = state.bond)

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Tasks ──
            TasksSection(tasks = state.tasks, viewModel = viewModel)
            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Reminders ──
            RemindersSection(
                reminders = state.reminders,
                onAddReminder = { navController.navigate(Screen.CreateReminder.route) }
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            // ── AI Agent Connection ──
            AgentConnectionSection(
                connection = state.agentConnection,
                checking = checkingAgent,
                onSave = viewModel::saveAgentConnection,
                onCheckNow = viewModel::refreshAgentStatus,
                onDisconnect = viewModel::disconnectAgent
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    companion: com.pixelpal.app.domain.model.Companion,
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
                    .size(72.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(Radius.large)
                    ),
                contentAlignment = Alignment.Center
            ) {
                PetRenderer(
                    petType = companion.effectiveSpecies,
                    animationState = AnimationState.HAPPY,
                    size = 64.dp
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
                    text = "${speciesLabel(companion.effectiveSpecies)} Companion • Bond Level ${bond?.level ?: 0}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun speciesLabel(species: String): String =
    species.replaceFirstChar { it.uppercase() }

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
                    text = "${bond?.totalInteractions ?: 0} meaningful interactions",
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
                text = if ((bond?.level ?: 0) >= 100) {
                    "Max Bond reached — ${bond?.streakDays ?: 0}-day streak"
                } else {
                    "${bond?.streakDays ?: 0}-day streak • keep completing tasks and reminders to grow your bond"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    text = "No pending reminders",
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
                    Text(
                        text = timeFormat.format(Date(reminder.triggerTime)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun AgentConnectionSection(
    connection: AgentConnection?,
    checking: Boolean,
    onSave: (AgentConnection) -> Unit,
    onCheckNow: () -> Unit,
    onDisconnect: () -> Unit
) {
    SectionHeader(title = "AI Agent Connection")

    var endpoint by remember(connection?.companionId) {
        mutableStateOf(connection?.endpointUrl ?: "")
    }
    var agentName by remember(connection?.companionId) {
        mutableStateOf(connection?.agentName ?: "")
    }
    var pollingEnabled by remember(connection?.companionId) {
        mutableStateOf(connection?.pollingEnabled ?: false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            // Status line
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = statusColor(connection),
                            shape = RoundedCornerShape(5.dp)
                        )
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = connectionStatusText(connection),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    connection?.currentTask?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            connection?.progress?.let { progress ->
                Spacer(modifier = Modifier.height(Spacing.sm))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "$progress% • ${connection.currentStatus.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            connection?.lastCheckedAt?.let {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "Last update: ${timeFormat.format(Date(it))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            AppTextField(
                value = agentName,
                onValueChange = { agentName = it },
                label = "Agent name",
                placeholder = "e.g. OpenCode Development Agent"
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
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
                        text = "Checks every ${connection?.pollingIntervalMinutes ?: 15} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = pollingEnabled,
                    onCheckedChange = { pollingEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row {
                TextButton(
                    onClick = {
                        val companionId = connection?.companionId ?: return@TextButton
                        onSave(
                            (connection ?: AgentConnection(companionId = companionId)).copy(
                                agentName = agentName.trim(),
                                endpointUrl = endpoint.trim(),
                                pollingEnabled = pollingEnabled
                            )
                        )
                    }
                ) {
                    Text(if (connection?.endpointUrl.isNullOrBlank()) "Connect" else "Save")
                }
                TextButton(onClick = onCheckNow, enabled = !checking && !connection?.endpointUrl.isNullOrBlank()) {
                    Text(if (checking) "Checking…" else "Check Now")
                }
                if (!connection?.endpointUrl.isNullOrBlank()) {
                    TextButton(onClick = onDisconnect) {
                        Text("Disconnect", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            connection?.errorMessage?.let {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun statusColor(connection: AgentConnection?): androidx.compose.ui.graphics.Color =
    when {
        connection == null -> MaterialTheme.colorScheme.onSurfaceVariant
        connection.connectionStatus == com.pixelpal.app.domain.model.ConnectionStatus.ERROR ->
            MaterialTheme.colorScheme.error
        connection.isConnected &&
            connection.currentStatus in setOf(
                com.pixelpal.app.domain.model.AgentState.WORKING,
                com.pixelpal.app.domain.model.AgentState.ONLINE,
                com.pixelpal.app.domain.model.AgentState.IDLE
            ) -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
private fun connectionStatusText(connection: AgentConnection?): String =
    when {
        connection == null -> "Not connected"
        connection.endpointUrl.isBlank() -> "Not connected"
        connection.connectionStatus == com.pixelpal.app.domain.model.ConnectionStatus.ERROR ->
            "Connection error"
        else -> "● Connected — ${connection.currentStatus.displayName}"
    }

