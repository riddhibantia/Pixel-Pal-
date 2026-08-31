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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.domain.model.AgentState
import com.pixelpal.app.presentation.components.BaseCompanionAvatar
import com.pixelpal.app.presentation.components.EmptyState
import com.pixelpal.app.presentation.components.LoadingState
import com.pixelpal.app.presentation.components.PixelPalBottomBar
import com.pixelpal.app.presentation.components.PrimaryButton
import com.pixelpal.app.presentation.components.SecondaryButton
import com.pixelpal.app.presentation.components.SectionHeader
import com.pixelpal.app.presentation.components.SettingsGroup
import com.pixelpal.app.presentation.components.SettingsRow
import com.pixelpal.app.presentation.navigation.Screen
import com.pixelpal.app.presentation.theme.Radius
import com.pixelpal.app.presentation.theme.Spacing
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

                    SectionHeader(title = "Today")
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

    // -- Interaction animation state --
    var interactionTick by remember { mutableIntStateOf(0) }
    var feedbackText by remember { mutableStateOf<String?>(null) }
    var showParticles by remember { mutableStateOf(false) }
    var currentExpression by remember { mutableStateOf(AnimationState.IDLE) }

    val scaleAnim = remember { Animatable(1f) }
    val rotationAnim = remember { Animatable(0f) }
    val glowAlphaAnim = remember { Animatable(0f) }

    val feedbackAlpha by animateFloatAsState(
        targetValue = if (feedbackText != null) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "feedbackAlpha"
    )

    val reactionMessages = remember(companion.name) {
        listOf(
            "${companion.name} is happy!",
            "That made me smile!",
            "Bond strengthened!",
            "${companion.name} enjoyed that!",
            "A happy moment!",
            "So much love!",
            "You made my day!",
            "We are getting closer!"
        )
    }

    LaunchedEffect(interactionTick) {
        if (interactionTick == 0) return@LaunchedEffect
        // Pick random feedback and expression for varied reactions
        feedbackText = reactionMessages.random()
        showParticles = true
        // Randomly choose happy smile or blink for this interaction
        val reactionType = (0..3).random()
        currentExpression = when (reactionType) {
            0 -> AnimationState.HAPPY  // bounce + happy smile + sparkles
            1 -> AnimationState.BLINK  // blink + bounce
            2 -> AnimationState.HAPPY  // wiggle + happy
            else -> AnimationState.HAPPY // heart glow
        }

        // Bounce + wiggle + glow sequence — slight variation per reaction
        launch {
            when (reactionType) {
                0 -> {
                    scaleAnim.animateTo(1.18f, animationSpec = tween(140, easing = FastOutSlowInEasing))
                    scaleAnim.animateTo(0.94f, animationSpec = tween(120))
                    scaleAnim.animateTo(1.06f, animationSpec = tween(100))
                    scaleAnim.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                }
                1 -> {
                    scaleAnim.animateTo(1.08f, tween(100))
                    scaleAnim.animateTo(1f, tween(120))
                    scaleAnim.animateTo(1.05f, tween(80))
                    scaleAnim.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium))
                }
                else -> {
                    scaleAnim.animateTo(1.12f, tween(130))
                    scaleAnim.animateTo(0.98f, tween(110))
                    scaleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                }
            }
        }
        launch {
            glowAlphaAnim.animateTo(0.55f, animationSpec = tween(180))
            glowAlphaAnim.animateTo(0f, animationSpec = tween(500, delayMillis = 400))
        }
        launch {
            // Wiggle varies per reaction
            if (reactionType == 2) {
                rotationAnim.animateTo(-7f, tween(90))
                rotationAnim.animateTo(7f, tween(110))
                rotationAnim.animateTo(-4f, tween(90))
                rotationAnim.animateTo(4f, tween(80))
                rotationAnim.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
            } else if (reactionType == 1) {
                // Subtle blink doesn't wiggle much
                rotationAnim.animateTo(-2f, tween(80))
                rotationAnim.animateTo(2f, tween(80))
                rotationAnim.animateTo(0f, tween(80))
            }
        }
        delay(900)
        currentExpression = AnimationState.IDLE
        delay(300)
        showParticles = false
        delay(900)
        feedbackText = null
    }

    fun triggerInteract() {
        interactionTick++
        onInteract()
    }

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

            // -- Large centered companion - the visual heart of the app --
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                // Soft glow behind companion during reaction
                if (glowAlphaAnim.value > 0.01f) {
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .alpha(glowAlphaAnim.value)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                CircleShape
                            )
                    )
                }

                // Sparkles / stars / hearts overlay - positioned around the pet
                if (showParticles) {
                    SparkleOverlay(alpha = glowAlphaAnim.value)
                }

                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .clickable(onClickLabel = "Interact with ${companion.name}") { triggerInteract() }
                        .graphicsLayer(
                            scaleX = scaleAnim.value,
                            scaleY = scaleAnim.value,
                            rotationZ = rotationAnim.value
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    BaseCompanionAvatar(
                        companion = companion,
                        size = 180.dp,
                        expression = currentExpression
                    )
                }
            }

            // Temporary feedback text
            if (feedbackText != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = feedbackText!!,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(feedbackAlpha)
                        .padding(horizontal = Spacing.md)
                )
            } else {
                Spacer(modifier = Modifier.height(Spacing.sm))
                // Reserve space so layout doesn't jump - invisible placeholder
                Text(
                    text = " ",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.height(18.dp)
                )
            }

            Text(
                text = companion.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${state.stateLabel()} \u00B7 Bond Level ${bond?.level ?: 0} \u00B7 ${bond?.streakDays ?: 0}d streak",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Primary interaction - compact, centered with press feedback
            PrimaryButton(
                text = "Interact",
                onClick = { triggerInteract() },
                modifier = Modifier.fillMaxWidth(0.55f)
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            SecondaryButton(
                text = "Open Workspace",
                onClick = onOpenWorkspace,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // -- Compact stats footer --
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

@Composable
private fun SparkleOverlay(alpha: Float) {
    Box(
        modifier = Modifier
            .size(220.dp)
            .alpha(alpha)
    ) {
        // Top-left sparkle (vector, not text emoji)
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.pixelpal.app.R.drawable.particle_sparkle),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 12.dp, y = 18.dp)
                .size(20.dp)
        )
        // Top-right star
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.pixelpal.app.R.drawable.particle_star),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-14).dp, y = 22.dp)
                .size(18.dp)
        )
        // Bottom-left heart
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.pixelpal.app.R.drawable.particle_heart),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 20.dp, y = (-16).dp)
                .size(16.dp)
        )
        // Bottom-right sparkle
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.pixelpal.app.R.drawable.particle_sparkle),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-18).dp, y = (-20).dp)
                .size(16.dp)
        )
        // Mid-right dot glow
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.pixelpal.app.R.drawable.particle_dot),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-6).dp)
                .size(14.dp)
        )
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
