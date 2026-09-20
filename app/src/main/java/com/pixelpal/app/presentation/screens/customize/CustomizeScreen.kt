package com.pixelpal.app.presentation.screens.customize

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.presentation.theme.CompanionColors
import com.pixelpal.app.domain.model.SpeciesStyle
import com.pixelpal.app.presentation.components.AppTextField
import com.pixelpal.app.presentation.components.AppTopBar
import com.pixelpal.app.presentation.components.BaseCompanionAvatar
import com.pixelpal.app.presentation.components.PetRenderer
import com.pixelpal.app.presentation.components.PixelPalBottomBar
import com.pixelpal.app.presentation.components.SectionHeader
import com.pixelpal.app.presentation.components.ThemeCard
import com.pixelpal.app.presentation.components.themeOptions
import com.pixelpal.app.presentation.navigation.Screen
import com.pixelpal.app.presentation.theme.Radius
import com.pixelpal.app.presentation.theme.Spacing

/**
 * Customize = transform THE companion's appearance (species × color × pattern)
 * plus app theme. Nothing here creates a second companion.
 */
@Composable
fun CustomizeScreen(
    navController: NavController,
    viewModel: CustomizeViewModel = hiltViewModel()
) {
    val companion by viewModel.companion.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()

    val species = companion?.effectiveSpecies ?: "cat"
    val color = companion?.color ?: "orange"
    val pattern = companion?.pattern ?: "plain"

    Scaffold(
        bottomBar = {
            PixelPalBottomBar(navController = navController, selected = Screen.Customize)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.lg)
        ) {
            AppTopBar(title = "Customize Companion")

            Column(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)) {

                SectionHeader(title = "Your Companion")

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.large),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.runtime.key(species) {
                            BaseCompanionAvatar(
                                companion = companion ?: com.pixelpal.app.domain.model.Companion(
                                    species = species,
                                    color = color,
                                    pattern = pattern
                                ),
                                size = 170.dp,
                                expression = AnimationState.IDLE
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.md))

                AppTextField(
                    value = companion?.name ?: "",
                    onValueChange = { if (it.length <= 20) viewModel.updatePetName(it) },
                    label = "Companion Name",
                    placeholder = "Name your companion"
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                Text(
                    text = "Transforming your companion never affects your bond, tasks, " +
                        "reminders, or agent connection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // ── SPECIES ──
                SectionHeader(title = "Species")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(SpeciesStyle.SPECIES) { candidate ->
                        OptionCard(
                            label = speciesLabel(candidate),
                            selected = species == candidate,
                            onClick = {
                                viewModel.transformAppearance(
                                    SpeciesStyle(candidate, color, pattern)
                                )
                            }
                        ) {
                            PetRenderer(
                                petType = candidate,
                                animationState = AnimationState.IDLE,
                                size = 56.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // ── APP APPEARANCE ──
                SectionHeader(title = "App Appearance")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    themeOptions.forEach { option ->
                        ThemeCard(
                            option = option,
                            selected = currentTheme.equals(option.id, ignoreCase = true),
                            onClick = { viewModel.selectTheme(option.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private fun speciesLabel(value: String): String =
    value.replaceFirstChar { it.uppercase() }

@Composable
private fun OptionCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .width(104.dp)
            .clickable { onClick() }
            .background(
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = RoundedCornerShape(Radius.medium)
            )
            .border(
                androidx.compose.foundation.BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                RoundedCornerShape(Radius.medium)
            )
            .padding(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(Radius.small)
                ),
            contentAlignment = Alignment.Center
        ) { content() }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
