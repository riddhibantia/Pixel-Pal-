package com.pixelpal.app.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.presentation.components.AppTopBar
import com.pixelpal.app.presentation.components.GroupDivider
import com.pixelpal.app.presentation.components.SectionHeader
import com.pixelpal.app.presentation.components.SettingsGroup
import com.pixelpal.app.presentation.components.SettingsRow
import com.pixelpal.app.presentation.components.ToggleSettingsRow
import com.pixelpal.app.presentation.theme.Spacing
import com.pixelpal.app.util.Constants
import com.pixelpal.app.util.PermissionHelper

/**
 * Screen Companion Overlay settings — master toggle, per-companion selection
 * (max [Constants.MAX_SIMULTANEOUS_OVERLAYS] simultaneous overlays), position
 * reset and background activity.
 */
@Composable
fun OverlaySettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val overlayEnabled by viewModel.overlayEnabled.collectAsState()
    val companions by viewModel.companions.collectAsState()
    val selectedIds by viewModel.overlayCompanionIds.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = Spacing.lg)
    ) {
        AppTopBar(title = "Screen Companion Overlay", onBack = { navController.popBackStack() })

        Column(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)) {

            SectionHeader(title = "Overlay")
            SettingsGroup {
                ToggleSettingsRow(
                    title = "Show companion on top of other apps",
                    description = "Your companions appear as small pets while you use other apps.",
                    icon = Icons.Default.Pets,
                    checked = overlayEnabled,
                    onCheckedChange = { viewModel.toggleOverlay(context) }
                )
            }

            SectionHeader(title = "Companions on screen")
            SettingsGroup {
                if (companions.isEmpty()) {
                    Text(
                        text = "Create a companion first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Spacing.md)
                    )
                } else {
                    companions.forEachIndexed { index, companion ->
                        if (index > 0) GroupDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = companion.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = companion.role.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Checkbox(
                                checked = companion.id in selectedIds,
                                onCheckedChange = { viewModel.toggleOverlayCompanion(companion.id) },
                                enabled = overlayEnabled &&
                                    (companion.id in selectedIds ||
                                        selectedIds.size < Constants.MAX_SIMULTANEOUS_OVERLAYS)
                            )
                        }
                    }
                    GroupDivider()
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Text(
                            text = if (selectedIds.isEmpty()) {
                                "No selection — the active companion is shown by default."
                            } else {
                                "${selectedIds.size} of ${Constants.MAX_SIMULTANEOUS_OVERLAYS} overlays active"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SectionHeader(title = "Position")
            SettingsGroup {
                SettingsRow(
                    title = "Reset Overlay Positions",
                    description = "Move all companions back to their default spots",
                    icon = Icons.Default.ScreenRotation,
                    onClick = { viewModel.resetOverlayPosition() }
                )
            }

            SectionHeader(title = "Background Activity")
            SettingsGroup {
                SettingsRow(
                    title = "Keep companions alive in background",
                    description = "Allows pets to stay on screen longer by excluding the app " +
                        "from battery optimization. Opens the system request.",
                    icon = Icons.Default.BatteryChargingFull,
                    onClick = {
                        (context as? android.app.Activity)?.let {
                            PermissionHelper.requestIgnoreBatteryOptimizations(it)
                        }
                    }
                )
            }
        }
    }
}