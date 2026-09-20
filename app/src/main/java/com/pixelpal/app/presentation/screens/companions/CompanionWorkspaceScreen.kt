package com.pixelpal.app.presentation.screens.companions

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.domain.model.AgentConnection
import com.pixelpal.app.domain.model.Task
import com.pixelpal.app.presentation.components.AppTextField
import com.pixelpal.app.presentation.components.AppTopBar
import com.pixelpal.app.presentation.components.BaseCompanionAvatar
import com.pixelpal.app.presentation.components.ConfirmationDialog
import com.pixelpal.app.presentation.components.DestructiveButton
import com.pixelpal.app.presentation.components.GroupDivider
import com.pixelpal.app.presentation.components.LoadingState
import com.pixelpal.app.presentation.components.PixelPalSnackbarHost
import com.pixelpal.app.presentation.components.PrimaryButton
import com.pixelpal.app.presentation.components.SecondaryButton
import com.pixelpal.app.presentation.components.SectionHeader
import com.pixelpal.app.presentation.components.SnackbarEvent
import com.pixelpal.app.presentation.navigation.Screen
import com.pixelpal.app.presentation.theme.Radius
import com.pixelpal.app.presentation.theme.Spacing
import kotlinx.coroutines.launch
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
    val commandFeedback by viewModel.commandFeedback.collectAsState()
    val geminiKeyOverride by viewModel.geminiKeyOverride.collectAsState()

    val voiceLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            viewModel.sendAgentCommand(spoken)
        }
    }
    val voiceCommand: () -> Unit = {
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak your command to the agent")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            viewModel.reportVoiceUnavailable()
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val state = uiState

    // Collect snackbar events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                when (event) {
                    is SnackbarEvent.AgentDisconnected -> viewModel.undoDisconnect(event.connection)
                    else -> {}
                }
            }
        }
    }

    if (state.loading || state.companion == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = "AI Agent", onBack = { navController.popBackStack() })
            LoadingState()
        }
        return
    }

    val companion = state.companion

    Scaffold(
        bottomBar = {
            com.pixelpal.app.presentation.components.PixelPalBottomBar(
                navController = navController,
                selected = Screen.CompanionWorkspace
            )
        },
        snackbarHost = { PixelPalSnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AppTopBar(title = "AI Agent", onBack = { navController.popBackStack() })

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
                SecondaryButton(
                    text = "Customize Companion",
                    onClick = { navController.navigate(Screen.Customize.route) }
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // ── AI Agent Connection — the workspace's sole purpose ──
                AgentConnectionSection(
                    connection = state.agentConnection,
                    checking = checkingAgent,
                    feedbackMessage = commandFeedback,
                    geminiKeyOverride = geminiKeyOverride,
                    onSave = viewModel::saveAgentConnection,
                    onCheckNow = viewModel::refreshAgentStatus,
                    onDisconnect = viewModel::disconnectAgent,
                    onSendCommand = viewModel::sendAgentCommand,
                    onVoiceCommand = voiceCommand,
                    onSaveGeminiKey = viewModel::saveGeminiKey
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    companion: com.pixelpal.app.domain.model.Companion,
    bond: com.pixelpal.app.domain.model.Bond?,
    viewModel: CompanionWorkspaceViewModel
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.large),
        color = MaterialTheme.colorScheme.surface
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
                    .clip(RoundedCornerShape(Radius.large))
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(Radius.large)
                    ),
                contentAlignment = Alignment.Center
            ) {
                BaseCompanionAvatar(
                    companion = companion,
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
private fun AgentConnectionSection(
    connection: AgentConnection?,
    checking: Boolean,
    feedbackMessage: String?,
    geminiKeyOverride: String,
    onSave: (AgentConnection) -> Unit,
    onCheckNow: () -> Unit,
    onDisconnect: () -> Unit,
    onSendCommand: (String) -> Unit,
    onVoiceCommand: () -> Unit,
    onSaveGeminiKey: (String) -> Unit
) {
    SectionHeader(title = "AI Agent Connection")

    val currentProvider = com.pixelpal.app.domain.model.AgentProviders.normalize(connection?.provider ?: "")
    var endpoint by remember(connection?.companionId) {
        mutableStateOf(connection?.endpointUrl ?: "")
    }
    var commandEndpoint by remember(connection?.companionId) {
        mutableStateOf(connection?.commandUrl ?: "")
    }
    var agentName by remember(connection?.companionId) {
        mutableStateOf(connection?.agentName ?: "")
    }
    var provider by remember(connection?.companionId, currentProvider) {
        mutableStateOf(currentProvider)
    }
    var intervalMinutes by remember(connection?.companionId) {
        mutableStateOf(connection?.pollingIntervalMinutes ?: com.pixelpal.app.util.Constants.DEFAULT_AGENT_POLL_INTERVAL_MIN)
    }
    var geminiKeyInput by remember(connection?.companionId, geminiKeyOverride) {
        mutableStateOf(geminiKeyOverride)
    }
    var commandText by remember { mutableStateOf("") }
    var pollingEnabled by remember(connection?.companionId) {
        mutableStateOf(connection?.pollingEnabled ?: false)
    }
    var showDisconnectConfirm by remember { mutableStateOf(false) }

    val isGemini = provider == com.pixelpal.app.domain.model.AgentProviders.GEMINI
    val isWebSocket = provider == com.pixelpal.app.domain.model.AgentProviders.WEBSOCKET
    // Gemini is configured without any endpoint; others need their URL.
    val isConfigured = if (isGemini) true else endpoint.trim().isNotBlank()
    // Check Now acts on the SAVED connection, not the draft form.
    val canCheck = !checking && (connection?.isGemini == true || connection?.endpointUrl?.isNotBlank() == true)

    if (showDisconnectConfirm) {
        ConfirmationDialog(
            title = "Disconnect Agent?",
            message = "This will clear the endpoint and stop polling. Your provider choice is kept so you can reconnect later.",
            confirmLabel = "Disconnect",
            dismissLabel = "Cancel",
            destructive = true,
            onConfirm = {
                showDisconnectConfirm = false
                onDisconnect()
            },
            onDismiss = { showDisconnectConfirm = false }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.large),
        color = MaterialTheme.colorScheme.surface
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

            Text(
                text = "Provider",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                com.pixelpal.app.domain.model.AgentProviders.ALL.forEach { option ->
                    androidx.compose.material3.FilterChip(
                        selected = provider == option,
                        onClick = { provider = option },
                        label = {
                            Text(com.pixelpal.app.domain.model.AgentProviders.displayName(option))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            AppTextField(
                value = agentName,
                onValueChange = { agentName = it },
                label = "Agent name",
                placeholder = "e.g. OpenCode Development Agent"
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            if (isGemini) {
                AppTextField(
                    value = geminiKeyInput,
                    onValueChange = {
                        geminiKeyInput = it
                        onSaveGeminiKey(it)
                    },
                    label = "Gemini API key",
                    placeholder = "Paste from Google AI Studio",
                    supportingText = if (geminiKeyOverride.isBlank()) {
                        "No key saved on this device — using the bundled key, if any. Stored only on this device."
                    } else {
                        "Key saved on this device. Clear the field to fall back to the bundled key."
                    }
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "Gemini needs no endpoint — chat and status run directly against Google AI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                AppTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = if (isWebSocket) "WebSocket URL" else "Status endpoint URL",
                    placeholder = if (isWebSocket) "wss://example.com/agent" else "https://example.com/status",
                    supportingText = if (isWebSocket) {
                        "Must start with ws:// or wss://. Live messages stream while this screen is open."
                    } else {
                        "HTTPS required (cleartext only for 127.0.0.1 / 10.0.2.2 / localhost). Must return {status, message?, currentTask?, progress?}."
                    }
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                AppTextField(
                    value = commandEndpoint,
                    onValueChange = { commandEndpoint = it },
                    label = "Command endpoint (optional)",
                    placeholder = "Defaults to the status URL"
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "Check interval",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                com.pixelpal.app.util.Constants.AGENT_POLL_INTERVAL_OPTIONS_MIN.forEach { option ->
                    androidx.compose.material3.FilterChip(
                        selected = intervalMinutes == option,
                        onClick = { intervalMinutes = option },
                        label = { Text(if (option >= 60) "${option / 60}h" else "${option}m") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Polling enabled", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Checks every ${intervalMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = pollingEnabled,
                    onCheckedChange = { pollingEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            PrimaryButton(
                text = if (connection?.endpointUrl.isNullOrBlank() && !isGemini) "Connect" else "Save",
                onClick = {
                    val companionId = connection?.companionId ?: return@PrimaryButton
                    onSave(
                        (connection ?: AgentConnection(companionId = companionId)).copy(
                            agentName = agentName.trim(),
                            provider = provider,
                            endpointUrl = if (isGemini) "" else endpoint.trim(),
                            commandUrl = if (isGemini) null else commandEndpoint.trim().takeIf { it.isNotEmpty() },
                            pollingEnabled = pollingEnabled,
                            pollingIntervalMinutes = intervalMinutes
                        )
                    )
                },
                enabled = !checking && isConfigured
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            SecondaryButton(
                text = if (checking) "Checking…" else "Check Now",
                onClick = onCheckNow,
                enabled = canCheck
            )

            // ── Talk to your agent: typed or spoken commands ──
            if (isConfigured) {
                Spacer(modifier = Modifier.height(Spacing.md))
                GroupDivider()
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Talk to your agent",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppTextField(
                        value = commandText,
                        onValueChange = { commandText = it },
                        label = "Send a command…",
                        placeholder = "e.g. run the test suite",
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onVoiceCommand) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Speak a command",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            onSendCommand(commandText)
                            commandText = ""
                        },
                        enabled = commandText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send command",
                            tint = if (commandText.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                feedbackMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.startsWith("Couldn't")) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
            if (isConfigured) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                DestructiveButton(
                    text = "Disconnect",
                    onClick = { showDisconnectConfirm = true }
                )
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
        connection.isGemini && connection.connectionStatus == com.pixelpal.app.domain.model.ConnectionStatus.ERROR ->
            "Gemini error"
        connection.isGemini && connection.currentStatus == com.pixelpal.app.domain.model.AgentState.DISCONNECTED ->
            "Gemini not configured — add an API key"
        connection.isGemini -> "Gemini AI — ${connection.currentStatus.displayName}"
        connection.endpointUrl.isBlank() -> "Not connected"
        connection.connectionStatus == com.pixelpal.app.domain.model.ConnectionStatus.ERROR ->
            "Connection error"
        else -> "● Connected — ${connection.currentStatus.displayName}"
    }
