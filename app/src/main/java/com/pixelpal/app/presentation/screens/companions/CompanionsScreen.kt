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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.presentation.components.AppTopBar
import com.pixelpal.app.presentation.components.PetRenderer
import com.pixelpal.app.presentation.components.PrimaryButton
import com.pixelpal.app.presentation.components.SectionHeader
import com.pixelpal.app.presentation.navigation.Screen
import com.pixelpal.app.presentation.theme.Radius
import com.pixelpal.app.presentation.theme.Sizing
import com.pixelpal.app.presentation.theme.Spacing
import com.pixelpal.app.util.Constants

@Composable
fun CompanionsScreen(
    navController: NavController,
    viewModel: CompanionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Companions", subtitle = "Manage all your companions")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(bottom = Spacing.lg)
        ) {
            Spacer(modifier = Modifier.height(Spacing.sm))

            if (uiState.companions.isEmpty() && !uiState.loading) {
                Text(
                    text = "No companions yet. Create your first one!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                SectionHeader(
                    title = "Active (${uiState.companions.size}/${Constants.MAX_ACTIVE_COMPANIONS})"
                )
                uiState.companions.forEach { companion ->
                    CompanionCardRow(
                        companion = companion,
                        active = companion.id == uiState.activeCompanionId,
                        onClick = {
                            navController.navigate(Screen.companionWorkspace(companion.id))
                        },
                        onSetActive = { viewModel.setActive(companion.id) },
                        onArchive = { viewModel.archive(companion) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            PrimaryButton(
                text = "New Companion",
                onClick = { navController.navigate(Screen.CreateCompanion.route) },
                enabled = uiState.canCreate,
                modifier = Modifier.fillMaxWidth()
            )
            if (!uiState.canCreate) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "Companion limit reached. Archive one to make room.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            ArchivedSection(
                companions = uiState.archivedCompanions,
                onRestore = viewModel::restore
            )
        }
    }
}

@Composable
private fun CompanionCardRow(
    companion: Companion,
    active: Boolean,
    onClick: () -> Unit,
    onSetActive: () -> Unit,
    onArchive: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.xs),
        colors = CardDefaults.cardColors(
            containerColor = if (active) {
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
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(Radius.medium)
                    ),
                contentAlignment = Alignment.Center
            ) {
                PetRenderer(
                    petType = companion.petType,
                    animationState = AnimationState.HAPPY,
                    size = 48.dp
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = companion.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (companion.isFavorite) {
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = companion.role.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (active) {
                Text(
                    text = "Active",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                TextButton(onClick = onSetActive) {
                    Text("Set active")
                }
            }

            IconButton(onClick = onArchive) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = "Archive ${companion.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ArchivedSection(
    companions: List<Companion>,
    onRestore: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Archived (${companions.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (expanded) {
            if (companions.isEmpty()) {
                Text(
                    text = "Nothing archived.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                companions.forEach { companion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = companion.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = companion.role.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onRestore(companion.id) }) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(Sizing.icon)
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text("Restore")
                        }
                    }
                }
            }
        }
    }
}