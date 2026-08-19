package com.pixelpal.app.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.presentation.components.AppTopBar
import com.pixelpal.app.presentation.components.GroupDivider
import com.pixelpal.app.presentation.components.SectionHeader
import com.pixelpal.app.presentation.components.SettingsGroup
import com.pixelpal.app.presentation.components.SettingsRow
import com.pixelpal.app.presentation.components.ToggleSettingsRow
import com.pixelpal.app.presentation.theme.Spacing
import com.pixelpal.app.util.PermissionHelper

/**
 * Screen Companion Overlay settings — master toggle, position reset and
 * background activity. All actions reuse existing logic from SettingsViewModel.
 */
@Composable
fun OverlaySettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val overlayEnabled by viewModel.overlayEnabled.collectAsState()

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
                    title = "Show Pixel on top of other apps",
                    description = "Pixel appears as a small companion while you use other apps.",
                    icon = Icons.Default.Pets,
                    checked = overlayEnabled,
                    onCheckedChange = { viewModel.toggleOverlay(context) }
                )
            }

            SectionHeader(title = "Position")
            SettingsGroup {
                SettingsRow(
                    title = "Reset Overlay Position",
                    description = "Move Pixel back to the default spot",
                    icon = Icons.Default.ScreenRotation,
                    onClick = { viewModel.resetOverlayPosition() }
                )
            }

            SectionHeader(title = "Background Activity")
            SettingsGroup {
                SettingsRow(
                    title = "Keep Pixel alive in background",
                    description = "Allows Pixel to stay on screen longer by excluding the app " +
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
