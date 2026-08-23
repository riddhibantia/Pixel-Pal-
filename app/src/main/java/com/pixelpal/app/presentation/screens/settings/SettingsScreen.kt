package com.pixelpal.app.presentation.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.BuildConfig
import com.pixelpal.app.presentation.components.AppTopBar
import com.pixelpal.app.presentation.components.GroupDivider
import com.pixelpal.app.presentation.components.PixelAvatar
import com.pixelpal.app.presentation.components.SectionHeader
import com.pixelpal.app.presentation.components.SettingsGroup
import com.pixelpal.app.presentation.components.SettingsRow
import com.pixelpal.app.presentation.components.ThemePickerSheet
import com.pixelpal.app.presentation.components.PixelPalBottomBar
import com.pixelpal.app.presentation.navigation.Screen
import com.pixelpal.app.presentation.theme.Radius
import com.pixelpal.app.presentation.theme.Spacing

/**
 * Settings = ME + APP behavior.
 * Profile / General / Companion / Privacy & Data / Support.
 * Companion customization lives in Customize — not here.
 */
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userName by viewModel.userName.collectAsState()
    val avatarSeed by viewModel.avatarSeed.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    var showThemeSheet by remember { mutableStateOf(false) }

    fun openNotificationSettings() {
        val intent = Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.parse("package:${context.packageName}")
            }
        }
        context.startActivity(intent)
    }

    fun openFeedbackEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, "PixelPal Feedback")
        }
        context.startActivity(Intent.createChooser(intent, "Send feedback"))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.lg)
        ) {
            AppTopBar(title = "Settings")

            Column(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)) {

                // ── PROFILE ──
                Surface(
                    shape = RoundedCornerShape(Radius.large),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = "Edit profile") {
                            navController.navigate(Screen.Profile.route)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelAvatar(seed = avatarSeed, size = 56.dp)
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userName.ifBlank { "Your Profile" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Personal account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── GENERAL ──
                SectionHeader(title = "General")
                SettingsGroup {
                    SettingsRow(
                        title = "Notifications",
                        description = "Manage reminder notifications",
                        icon = Icons.Default.Notifications,
                        onClick = { openNotificationSettings() }
                    )
                    GroupDivider()
                    SettingsRow(
                        title = "Appearance",
                        value = currentTheme.replaceFirstChar { it.uppercase() },
                        icon = Icons.Default.Palette,
                        onClick = { showThemeSheet = true }
                    )
                }

                // ── COMPANION ──
                SectionHeader(title = "Companion")
                SettingsGroup {
                    SettingsRow(
                        title = "Manage Companions",
                        description = "Create, switch and archive companions",
                        icon = Icons.Default.Groups,
                        onClick = { navController.navigate(Screen.Companions.route) }
                    )
                    GroupDivider()
                    SettingsRow(
                        title = "Archived Companions",
                        description = "Restore or view archived companions",
                        icon = Icons.Default.Archive,
                        onClick = { navController.navigate(Screen.Companions.route) }
                    )
                    GroupDivider()
                    SettingsRow(
                        title = "Screen Companion Overlay",
                        description = "Show Pixel on top of other apps",
                        icon = Icons.Default.Pets,
                        onClick = { navController.navigate(Screen.OverlaySettings.route) }
                    )
                    GroupDivider()
                    SettingsRow(
                        title = "Background Activity",
                        description = "Keep Pixel alive in the background",
                        icon = Icons.Default.Layers,
                        onClick = { navController.navigate(Screen.OverlaySettings.route) }
                    )
                }

                // ── PRIVACY & DATA ──
                SectionHeader(title = "Privacy & Data")
                SettingsGroup {
                    SettingsRow(
                        title = "Permissions",
                        description = "Overlay, notifications and alarms",
                        icon = Icons.Default.Security,
                        onClick = { navController.navigate(Screen.Permissions.route) }
                    )
                }

                // ── SUPPORT ──
                SectionHeader(title = "Support")
                SettingsGroup {
                    SettingsRow(
                        title = "Help & Feedback",
                        icon = Icons.Default.Mail,
                        onClick = { openFeedbackEmail() }
                    )
                    GroupDivider()
                    SettingsRow(
                        title = "About PixelPal",
                        value = "v${BuildConfig.VERSION_NAME}",
                        icon = Icons.Default.Info,
                        onClick = { navController.navigate(Screen.About.route) }
                    )
                }
            }
        }

        PixelPalBottomBar(navController = navController, selected = Screen.Settings)
    }

    if (showThemeSheet) {
        ThemePickerSheet(
            currentTheme = currentTheme,
            onSelectTheme = {
                viewModel.selectTheme(it)
                showThemeSheet = false
            },
            onDismiss = { showThemeSheet = false }
        )
    }
}
