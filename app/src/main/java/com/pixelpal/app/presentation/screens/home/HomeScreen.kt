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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.presentation.components.AppTopBar
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

/**
 * Home — the companion is the visual hero.
 * Greeting → companion stage (pet + emotion + bond) → stats → friendship
 * progress → actions → compact overlay row (navigates to Overlay settings).
 */
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val petName by viewModel.petName.collectAsState()
    val petType by viewModel.selectedPetType.collectAsState()
    val overlayEnabled by viewModel.overlayEnabled.collectAsState()
    val currentAnim by viewModel.currentAnimation.collectAsState()
    val currentEmotion by viewModel.currentEmotion.collectAsState()
    val bond by viewModel.bond.collectAsState()

    val greeting = getGreeting()
    val emotionLabel = emotionLabelFor(currentEmotion.name)

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
            AppTopBar(title = greeting, subtitle = petName)

            Column(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)) {

                // ── COMPANION STAGE ──
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.large),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            )
                            PetRenderer(
                                petType = petType,
                                animationState = currentAnim,
                                size = 170.dp,
                                modifier = Modifier.clickable { viewModel.tapPet() }
                            )
                        }

                        // Emotion + bond summary inside the same stage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(Radius.small),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pets,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = emotionLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Bond Lvl ${bond.level}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // ── STATS + FRIENDSHIP PROGRESS ──
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.large),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatItem(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Favorite,
                                label = "Bond",
                                value = "Lvl ${bond.level}",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            StatItem(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.TouchApp,
                                label = "Interactions",
                                value = "${bond.totalInteractions}",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            StatItem(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.LocalFireDepartment,
                                label = "Streak",
                                value = "${bond.streakDays}d",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(horizontal = Spacing.xs),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))

                        // Friendship progress
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Friendship Level",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${bond.level}%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        LinearProgressIndicator(
                            progress = { (bond.level.coerceIn(0, 100)) / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = "${100 - bond.level}% to next level",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                // ── ACTIONS ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    PrimaryButton(
                        text = "Feed",
                        onClick = { viewModel.feedPet() },
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(
                        text = "Play",
                        onClick = { viewModel.tapPet() },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                // ── OVERLAY (compact row → Overlay settings) ──
                SettingsGroup {
                    SettingsRow(
                        title = "Screen Overlay",
                        description = if (overlayEnabled) "Pixel is active on top of other apps" else "Pixel is paused",
                        value = if (overlayEnabled) "Active" else "Paused",
                        icon = Icons.Default.Pets,
                        onClick = { navController.navigate(Screen.OverlaySettings.route) }
                    )
                }
            }
        }
    }
}

/** One stat column on the shared stats surface. */
@Composable
private fun StatItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = tint
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun emotionLabelFor(name: String): String {
    return when (name) {
        "HAPPY" -> "Happy"
        "CURIOUS" -> "Curious"
        "SLEEPY" -> "Sleepy"
        "HUNGRY" -> "Hungry"
        "LONELY" -> "Lonely"
        "EXCITED" -> "Excited"
        "CALM" -> "Calm"
        "THINKING" -> "Thinking"
        "SAD" -> "Sad"
        else -> name.lowercase().replaceFirstChar { it.uppercase() }
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