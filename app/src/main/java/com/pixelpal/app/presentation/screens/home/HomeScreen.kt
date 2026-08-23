package com.pixelpal.app.presentation.screens.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.domain.model.AgentState
import com.pixelpal.app.presentation.components.EmptyState
import com.pixelpal.app.presentation.components.LoadingState
import com.pixelpal.app.presentation.components.PetRenderer
import com.pixelpal.app.presentation.components.PixelPalBottomBar
import com.pixelpal.app.presentation.components.PrimaryButton
import com.pixelpal.app.presentation.components.SecondaryButton
import com.pixelpal.app.presentation.components.SettingsGroup
import com.pixelpal.app.presentation.components.SettingsRow
import com.pixelpal.app.presentation.navigation.Screen
import com.pixelpal.app.presentation.theme.Radius
import com.pixelpal.app.presentation.theme.Spacing
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            PixelPalBottomBar(navController = navController, selected = Screen.Home)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.lg)
        ) {
            HomeHeader(
                userName = uiState.userName,
                unreadCount = uiState.unreadActivityCount,
                onBellClick = { navController.navigate(Screen.ActivityCenter.route) }
            )

            when {
                uiState.isLoading -> LoadingState(modifier = Modifier.padding(top = Spacing.xl))

                uiState.companion == null && !uiState.isLoading -> EmptyState(
                    title = "No companion yet",
                    message = "Your digital companion will appear here once created.",
                    icon = Icons.Default.SmartToy
                )

                else -> Column(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)) {

                    val companion = uiState.companion

                    CompanionHeroCard(
                        state = uiState,
                        onInteract = { viewModel.interactWithCompanion() },
                        onFeed = { viewModel.feedCompanion() },
                        onOpenWorkspace = {
                            companion?.let {
                                navController.navigate(Screen.CompanionWorkspace.route)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(Spacing.md))

                    SectionTitle("Today")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        QuickStatusCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Checklist,
                            label = "Tasks",
                            value = "${uiState.pendingTasks.size} remaining",
                            onClick = { navController.navigate(Screen.CompanionWorkspace.route) }
                        )
                        QuickStatusCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Alarm,
                            label = "Reminders",
                            value = uiState.nextReminder?.let { "1 upcoming" } ?: "None",
                            onClick = { navController.navigate(Screen.Reminders.route) }
                        )
                        QuickStatusCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.SmartToy,
                            label = "Agent",
                            value = agentChipValue(uiState.agentConnection?.currentStatus),
                            onClick = { navController.navigate(Screen.CompanionWorkspace.route) }
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.md))

                    SettingsGroup {
                        SettingsRow(
                            title = "Screen Overlay",
                            description = if (uiState.overlayEnabled) {
                                "${companion?.name ?: "Pixel"} is active on top of other apps"
                            } else {
                                "The overlay companion is paused"
                            },
                            value = if (uiState.overlayEnabled) "Active" else "Paused",
                            icon = Icons.Default.SmartToy,
                            onClick = { navController.navigate(Screen.OverlaySettings.route) }
                        )
                    }
                }
            }
        }
    }
}

private fun agentChipValue(state: AgentState?): String = when (state) {
    null, AgentState.DISCONNECTED -> "Off"
    AgentState.WORKING -> "Working"
    AgentState.WAITING_FOR_INPUT, AgentState.ERROR, AgentState.OFFLINE -> "Attention"
    else -> state.displayName
}

@Composable
private fun HomeHeader(
    userName: String,
    unreadCount: Int,
    onBellClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = getGreeting(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (userName.isNotBlank()) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onBellClick) {
            BadgedBox(badge = {
                if (unreadCount > 0) {
                    Badge { Text(text = unreadCount.coerceAtMost(9).toString()) }
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CompanionHeroCard(
    state: HomeUiState,
    onInteract: () -> Unit,
    onFeed: () -> Unit,
    onOpenWorkspace: () -> Unit
) {
    val companion = state.companion ?: return
    val bond = state.bond

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.large),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.lg, horizontal = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Large centered companion — the visual heart of the app ──
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .clickable(onClickLabel = "Interact with ${companion.name}") { onInteract() },
                contentAlignment = Alignment.Center
            ) {
                PetRenderer(
                    petType = companion.effectiveSpecies,
                    animationState = AnimationState.IDLE,
                    size = 180.dp
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = companion.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${state.stateLabel()} • Bond Level ${bond?.level ?: 0} • ${bond?.streakDays ?: 0}d streak",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Primary interaction — compact, centered
            PrimaryButton(
                text = "Interact",
                onClick = onInteract,
                modifier = Modifier.fillMaxWidth(0.55f)
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            SecondaryButton(
                text = "Open Workspace",
                onClick = onOpenWorkspace,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Compact stats footer ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatText("Lv ${bond?.level ?: 0}")
                StatText("${bond?.totalInteractions ?: 0} interactions")
                StatText("${bond?.streakDays ?: 0}d streak")
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
            if (state.isMaxBond) {
                Text(
                    text = "Max Bond reached",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                LinearProgressIndicator(
                    progress = { (bond?.level ?: 0) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

private fun speciesLabel(species: String): String =
    species.replaceFirstChar { it.uppercase() }

@Composable
private fun StatText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.sm)
    )
}

@Composable
private fun QuickStatusCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClickLabel = label) { onClick() },
        shape = RoundedCornerShape(Radius.medium),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        hour < 21 -> "Good evening"
        else -> "Good night"
    }
}