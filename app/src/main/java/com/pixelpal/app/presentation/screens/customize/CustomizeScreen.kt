package com.pixelpal.app.presentation.screens.customize

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.domain.model.PetType
import com.pixelpal.app.presentation.components.AppTextField
import com.pixelpal.app.presentation.components.AppTopBar
import com.pixelpal.app.presentation.components.PetRenderer
import com.pixelpal.app.presentation.components.PixelPalBottomBar
import com.pixelpal.app.presentation.components.SectionHeader
import com.pixelpal.app.presentation.components.ThemeCard
import com.pixelpal.app.presentation.components.themeOptions
import com.pixelpal.app.presentation.navigation.Screen
import com.pixelpal.app.presentation.theme.Radius
import com.pixelpal.app.presentation.theme.Sizing
import com.pixelpal.app.presentation.theme.Spacing

/**
 * Customize = PIXEL + APPEARANCE.
 * The single authoritative place for the companion name, the companion
 * selection and the app theme. Settings stays for ME + APP behavior.
 */
@Composable
fun CustomizeScreen(
    navController: NavController,
    viewModel: CustomizeViewModel = hiltViewModel()
) {
    val selectedPetId by viewModel.selectedPetType.collectAsState()
    val petName by viewModel.petName.collectAsState()
    val bond by viewModel.bond.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()

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
            AppTopBar(title = "Customize")

            Column(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)) {

                // ── COMPANION ──
                SectionHeader(title = "Companion")

                // Large living preview
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
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    CircleShape
                                )
                        )
                        PetRenderer(
                            petType = selectedPetId,
                            animationState = AnimationState.HAPPY,
                            size = 170.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                AppTextField(
                    value = petName,
                    onValueChange = { if (it.length <= 20) viewModel.updatePetName(it) },
                    label = "Companion Name",
                    placeholder = "Name your companion"
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // ── SELECT COMPANION ──
                SectionHeader(title = "Select Companion")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(PetType.entries) { pet ->
                        CompanionCard(
                            pet = pet,
                            bondLevel = bond.level,
                            selected = selectedPetId.equals(pet.id, ignoreCase = true),
                            onClick = { viewModel.selectPet(pet) }
                        )
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

/** Visual companion card: sprite, name, lock state + bond requirement, selected ring + check. */
@Composable
private fun CompanionCard(
    pet: PetType,
    bondLevel: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locked = !pet.hasFullAnimationSet || bondLevel < pet.unlockBondLevel

    Column(
        modifier = modifier
            .width(120.dp)
            .clickable(onClickLabel = pet.displayName, enabled = !locked) { onClick() }
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(Radius.large)
            )
            .border(
                BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                RoundedCornerShape(Radius.large)
            )
            .padding(Spacing.md)
            .semantics {
                contentDescription = buildString {
                    append(pet.displayName)
                    append(", ")
                    append(
                        when {
                            locked -> "locked"
                            selected -> "selected"
                            else -> "available"
                        }
                    )
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sprite or placeholder
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    if (locked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(Radius.medium)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (pet.hasFullAnimationSet) {
                PetRenderer(
                    petType = pet.id,
                    animationState = AnimationState.HAPPY,
                    size = 56.dp
                )
            } else {
                Icon(
                    imageVector = if (locked) Icons.Default.Lock else Icons.Default.Pets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Sizing.icon)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pet.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (selected) {
                Spacer(modifier = Modifier.width(Spacing.xs))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = when {
                !pet.hasFullAnimationSet -> "Coming Soon"
                bondLevel < pet.unlockBondLevel -> "Bond Lvl ${pet.unlockBondLevel}"
                selected -> "Selected"
                else -> "Unlocked"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (locked) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.primary
        )
    }
}