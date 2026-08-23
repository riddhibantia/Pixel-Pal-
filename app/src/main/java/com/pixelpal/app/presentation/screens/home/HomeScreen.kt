package com.pixelpal.app.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

import com.pixelpal.app.domain.model.CompanionRole
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
import com.pixelpal.app.util.Constants
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

                uiState.cards.isEmpty() -> EmptyState(
                    title = "No companions yet",
                    message = "Create your first companion to get started.",
                    icon = Icons.Default.Pets,
                    content = {
                        PrimaryButton(
                            text = "Create companion",
                            onClick = { navController.navigate(Screen.CreateCompanion.route) }
                        )
                    }
                )

                else -> Column(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)) {
                    SectionTitle("Today")

                    uiState.cards.forEach { card ->
                        CompanionDashboardCard(
                            card = card,
                            onInteract = { viewModel.interactWith(card.companion.id) },
                            onFeed = { viewModel.feedCompanion(card.companion.id) },
                            onOpenWorkspace = {
                                navController.navigate(Screen.companionWorkspace(card.companion.id))
                            },
                            onAddReminder = {
                                navController.navigate(Screen.reminderForCompanion(card.companion.id))
                            }
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }

                    if (uiState.canCreateCompanion) {
                        AddCompanionAction(
                            onClick = { navController.navigate(Screen.CreateCompanion.route) }
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                    } else {
                        Text(
                            text = "Companion limit reached (${Constants.MAX_ACTIVE_COMPANIONS}). " +
                                "Archive one to make room.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Spacing.sm)
                        )
                    }

                    OverlayStatusRow(
                        overlayEnabled = uiState.overlayEnabled,
                        activePetName = uiState.cards.firstOrNull()?.companion?.name ?: "Pixel",
                        onClick = { navController.navigate(Screen.OverlaySettings.route) }
                    )
                }
            }
        }
    }
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
                    contentDescription = "Activity center",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CompanionDashboardCard(
    card: CompanionCardUi,
    onInteract: () -> Unit,
    onFeed: () -> Unit,
    onOpenWorkspace: () -> Unit,
    onAddReminder: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open ${card.companion.name} workspace") { onOpenWorkspace() },
        shape = RoundedCornerShape(Radius.large),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {

            // ── Identity + state ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable(onClickLabel = "Interact with ${card.companion.name}") { onInteract() },
                    contentAlignment = Alignment.Center
                ) {
                    PetRenderer(
                        petType = card.companion.petType,
                        animationState = com.pixelpal.app.animation.AnimationState.IDLE,
                        size = 48.dp
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = card.companion.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (card.companion.isFavorite) {
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                text = "★",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    Text(
                        text = roleSubtitle(card.companion.role),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Compact state line: Calm • Bond Lvl 3 • Streak 2
                    Text(
                        text = "${card.stateLabel()} • Bond Lvl ${card.bondLevel} • Streak ${card.streakDays}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    roleSecondaryInfo(card)?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Role-specific primary action(s) ──
            RoleActionsRow(
                role = card.companion.role,
                onInteract = onInteract,
                onFeed = onFeed,
                onOpenWorkspace = onOpenWorkspace,
                onAddReminder = onAddReminder
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Compact stats footer ──
            CompactStatsFooter(card = card)
        }
    }
}

@Composable
private fun RoleActionsRow(
    role: CompanionRole,
    onInteract: () -> Unit,
    onFeed: () -> Unit,
    onOpenWorkspace: () -> Unit,
    onAddReminder: () -> Unit
) {
    when (role) {
        CompanionRole.GENERAL -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            PrimaryButton(text = "Feed", onClick = onFeed, modifier = Modifier.weight(1f))
            SecondaryButton(text = "Play", onClick = onInteract, modifier = Modifier.weight(1f))
        }
        CompanionRole.TASK -> PrimaryButton(
            text = "Open Checklist",
            onClick = onOpenWorkspace,
            modifier = Modifier.fillMaxWidth()
        )
        CompanionRole.REMINDER -> PrimaryButton(
            text = "Add Reminder",
            onClick = onAddReminder,
            modifier = Modifier.fillMaxWidth()
        )
        CompanionRole.AI_AGENT -> PrimaryButton(
            text = "Check Status",
            onClick = onOpenWorkspace,
            modifier = Modifier.fillMaxWidth()
        )
        CompanionRole.CUSTOM -> PrimaryButton(
            text = "Open Workspace",
            onClick = onOpenWorkspace,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Role-specific secondary line under the state line. Null when nothing to add. */
@Composable
private fun roleSecondaryInfo(card: CompanionCardUi): String? = when (card.companion.role) {
    CompanionRole.GENERAL -> null
    CompanionRole.TASK -> when {
        card.pendingTasks > 0 && card.completedTodayTasks > 0 ->
            "${card.pendingTasks} pending • ${card.completedTodayTasks} done today"
        card.pendingTasks > 0 -> "${card.pendingTasks} pending tasks"
        else -> "All caught up"
    }
    CompanionRole.REMINDER -> card.nextReminderTime?.let { "Next: ${formatShortDateTime(it)}" }
        ?: "No reminders yet"
    CompanionRole.AI_AGENT -> when (val state = card.agentState) {
        null -> "Agent not configured"
        else -> "${state.displayName} • ${relativeTime(card.agentLastCheckedAt ?: 0L)}"
    }
    CompanionRole.CUSTOM -> card.companion.description?.ifBlank { null } ?: "Your custom companion"
}

private fun roleSubtitle(role: CompanionRole): String = when (role) {
    CompanionRole.AI_AGENT -> role.displayName
    else -> "${role.displayName} Companion"
}

@Composable
private fun CompactStatsFooter(card: CompanionCardUi) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatText("Lv ${card.bondLevel}")
            StatText("${card.totalInteractions} interactions")
            StatText("${card.streakDays}d streak")
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        if (card.isMaxBond) {
            Text(
                text = "Max Bond reached",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            LinearProgressIndicator(
                progress = { card.bondLevel / 100f },
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
        modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs, bottom = Spacing.sm)
    )
}

@Composable
private fun AddCompanionAction(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.large))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(Radius.large)
            )
            .clickable(onClickLabel = "Add companion") { onClick() },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(Radius.large)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = "Add Companion",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun OverlayStatusRow(
    overlayEnabled: Boolean,
    activePetName: String,
    onClick: () -> Unit
) {
    SettingsGroup {
        SettingsRow(
            title = "Screen Overlay",
            description = if (overlayEnabled) "$activePetName is active on top of other apps"
            else "The overlay companion is paused",
            value = if (overlayEnabled) "Active" else "Paused",
            icon = Icons.Default.Pets,
            onClick = onClick
        )
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

private fun formatShortDateTime(time: Long): String {    val cal = Calendar.getInstance().apply { timeInMillis = time }
    val now = Calendar.getInstance()
    val dayPrefix = when {
        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
            cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> "Today"
        else -> "Later"
    }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val amPm = if (hour < 12) "AM" else "PM"
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return "$dayPrefix, $h12:${minute.toString().padStart(2, '0')} $amPm"
}

private fun relativeTime(time: Long): String {
    if (time <= 0L) return "never"
    val diff = System.currentTimeMillis() - time
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        TimeUnit.MILLISECONDS.toHours(diff) < 24 -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
        else -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
    }
}
