package com.pixelpal.app.presentation.screens.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.core.content.ContextCompat
import com.pixelpal.app.presentation.components.AppTopBar
import com.pixelpal.app.presentation.components.GroupDivider
import com.pixelpal.app.presentation.components.SectionHeader
import com.pixelpal.app.presentation.components.SettingsGroup
import com.pixelpal.app.presentation.components.SettingsRow
import com.pixelpal.app.presentation.theme.Sizing
import com.pixelpal.app.presentation.theme.Spacing
import com.pixelpal.app.util.PermissionHelper

private fun notificationGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

/**
 * Privacy & Data → Permissions: live status of overlay, notification and
 * exact alarm permissions with enable actions (reusing PermissionHelper).
 * State is shown with icon + text — never color alone.
 */
@Composable
fun PermissionsScreen(
    navController: NavController
) {
    val context = LocalContext.current

    var overlayGranted by remember { mutableStateOf(PermissionHelper.canDrawOverlays(context)) }
    var notificationGrantedState by remember { mutableStateOf(notificationGranted(context)) }
    var alarmGranted by remember { mutableStateOf(PermissionHelper.canScheduleExactAlarms(context)) }

    // Re-check statuses whenever the screen resumes to the foreground.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val observer = remember {
        androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                overlayGranted = PermissionHelper.canDrawOverlays(context)
                notificationGrantedState = notificationGranted(context)
                alarmGranted = PermissionHelper.canScheduleExactAlarms(context)
            }
        }
    }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGrantedState = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = Spacing.lg)
    ) {
        AppTopBar(title = "Permissions", onBack = { navController.popBackStack() })

        Column(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)) {

            SectionHeader(title = "Current Status")
            SettingsGroup {
                PermissionStatusRow(
                    title = "Screen Overlay",
                    description = "Needed for Pixel to appear over other apps",
                    icon = Icons.Default.Pets,
                    granted = overlayGranted,
                    onRequest = {
                        (context as? Activity)?.let { PermissionHelper.requestOverlayPermission(it) }
                    }
                )
                GroupDivider()
                PermissionStatusRow(
                    title = "Notifications",
                    description = "Needed for reminders to alert you",
                    icon = Icons.Default.Notifications,
                    granted = notificationGrantedState,
                    onRequest = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    showAction = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                )
                GroupDivider()
                PermissionStatusRow(
                    title = "Exact Alarms",
                    description = "Needed for reminders to fire at the exact time",
                    icon = Icons.Default.Alarm,
                    granted = alarmGranted,
                    onRequest = {
                        (context as? Activity)?.let { PermissionHelper.requestExactAlarmPermission(it) }
                    },
                    showAction = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    granted: Boolean,
    onRequest: () -> Unit,
    showAction: Boolean = true
) {
    if (granted) {
        SettingsRow(
            title = title,
            description = description,
            icon = icon,
            trailing = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Allowed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Sizing.icon)
                )
            }
        )
    } else {
        SettingsRow(
            title = title,
            description = description,
            icon = icon,
            value = if (showAction) "Not allowed" else "Managed by system",
            onClick = if (showAction) onRequest else null,
            trailing = if (!showAction) {
                {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Managed by system",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Sizing.icon)
                    )
                }
            } else null
        )
    }
}
