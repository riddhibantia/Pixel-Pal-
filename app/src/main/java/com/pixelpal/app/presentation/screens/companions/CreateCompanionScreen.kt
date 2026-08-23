package com.pixelpal.app.presentation.screens.companions

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
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.domain.model.CompanionRole
import com.pixelpal.app.domain.model.PetType
import com.pixelpal.app.presentation.components.AppTextField
import com.pixelpal.app.presentation.components.AppTopBar
import com.pixelpal.app.presentation.components.PetRenderer
import com.pixelpal.app.presentation.components.PrimaryButton
import com.pixelpal.app.presentation.components.SecondaryButton
import com.pixelpal.app.presentation.components.SectionHeader
import com.pixelpal.app.presentation.navigation.Screen
import com.pixelpal.app.presentation.theme.Radius
import com.pixelpal.app.presentation.theme.Sizing
import com.pixelpal.app.presentation.theme.Spacing

@Composable
fun CreateCompanionScreen(
    navController: NavController,
    viewModel: CreateCompanionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = Spacing.lg)
    ) {
        AppTopBar(
            title = "New Companion",
            onBack = {
                if (uiState.step == 0 || uiState.createdCompanion != null) navController.popBackStack()
                else viewModel.previousStep()
            }
        )

        if (uiState.step <= 2) {
            StepIndicator(step = uiState.step)
        }

        Column(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)) {
            when (uiState.step) {
                0 -> RoleStep(
                    state = uiState,
                    onSelect = viewModel::selectRole,
                    onNext = viewModel::nextStep,
                    onBack = { navController.popBackStack() }
                )
                1 -> DetailsStep(state = uiState, viewModel = viewModel)
                2 -> ReviewStep(state = uiState)
                else -> DoneStep(companion = uiState.createdCompanion, navController = navController)
            }

            if (uiState.step < 2) {
                Spacer(modifier = Modifier.height(Spacing.lg))
            }

            uiState.error?.let {
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.step == 2) {
                Spacer(modifier = Modifier.height(Spacing.lg))
                PrimaryButton(
                    text = if (uiState.isCreating) "Creating…" else "Create Companion",
                    onClick = viewModel::create,
                    enabled = uiState.isStepValid && !uiState.isCreating,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                SecondaryButton(
                    text = "Back",
                    onClick = viewModel::previousStep,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val labels = listOf("Role", "Details", "Review")
        labels.forEachIndexed { index, label ->
            val active = index == step
            val done = index < step
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = when {
                            active || done -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (index < labels.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = if (done) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun RoleStep(
    state: CreateCompanionUiState,
    onSelect: (CompanionRole) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    SectionHeader(title = "What is this companion for?")
    Text(
        text = "Each companion can be given a role. You can change it later.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(Spacing.md))

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        CompanionRole.entries.forEach { role ->
            val selected = role == state.role
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(role) }
                    .border(
                        BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        RoundedCornerShape(Radius.large)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = role.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = role.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Sizing.icon)
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(Spacing.lg))
    PrimaryButton(
        text = "Continue",
        onClick = onNext,
        enabled = state.isStepValid,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(Spacing.sm))
    SecondaryButton(
        text = "Back",
        onClick = onBack,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DetailsStep(
    state: CreateCompanionUiState,
    viewModel: CreateCompanionViewModel
) {
    SectionHeader(title = "Name & pet")
    AppTextField(
        value = state.name,
        onValueChange = viewModel::setName,
        label = "Name",
        placeholder = "Leave blank for a default name"
    )
    Spacer(modifier = Modifier.height(Spacing.md))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(Radius.large)
            ),
        contentAlignment = Alignment.Center
    ) {
        PetRenderer(
            petType = state.petType,
            animationState = AnimationState.HAPPY,
            size = 150.dp
        )
    }

    Spacer(modifier = Modifier.height(Spacing.md))

    SectionHeader(title = "Pet type")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(PetType.entries) { pet ->
            val selected = pet.id == state.petType
            Column(
                modifier = Modifier
                    .width(96.dp)
                    .clickable { viewModel.selectPetType(pet.id) }
                    .background(
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(Radius.medium)
                    )
                    .border(
                        BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        RoundedCornerShape(Radius.medium)
                    )
                    .padding(Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (pet.hasFullAnimationSet) {
                    PetRenderer(
                        petType = pet.id,
                        animationState = AnimationState.HAPPY,
                        size = 56.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Text(
                    text = pet.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(Spacing.lg))
    PrimaryButton(
        text = "Continue",
        onClick = viewModel::nextStep,
        enabled = state.isStepValid,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(Spacing.sm))
    SecondaryButton(
        text = "Back",
        onClick = viewModel::previousStep,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ReviewStep(state: CreateCompanionUiState) {
    SectionHeader(title = "Review")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(Radius.medium)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    PetRenderer(
                        petType = state.petType,
                        animationState = AnimationState.HAPPY,
                        size = 64.dp
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.md))
                Column {
                    Text(
                        text = state.effectiveName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.role.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "Appearance can be customized any time from the Customize screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DoneStep(
    companion: com.pixelpal.app.domain.model.Companion?,
    navController: NavController
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = "Your companion is ready!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = companion?.name ?: "Pixel",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            PetRenderer(
                petType = companion?.petType ?: "cat",
                animationState = AnimationState.HAPPY,
                size = 150.dp
            )
        }
        Spacer(modifier = Modifier.height(Spacing.lg))
        PrimaryButton(
            text = "Open workspace",
            onClick = {
                companion?.let {
                    navController.navigate(Screen.companionWorkspace(it.id)) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        SecondaryButton(
            text = "Back to Home",
            onClick = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}