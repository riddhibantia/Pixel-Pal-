package com.pixelpal.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pixelpal.app.presentation.navigation.Screen

private data class NavDestination(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
)

private val destinations = listOf(
    NavDestination(Screen.Home, Icons.Filled.Home, Icons.Outlined.Home, "Home"),
    NavDestination(Screen.Tasks, Icons.Filled.Checklist, Icons.Outlined.Checklist, "Tasks"),
    NavDestination(Screen.CompanionWorkspace, Icons.Filled.SmartToy, Icons.Outlined.SmartToy, "Agent"),
    NavDestination(Screen.Reminders, Icons.Filled.Notifications, Icons.Outlined.Notifications, "Reminders"),
    NavDestination(Screen.Settings, Icons.Filled.Settings, Icons.Outlined.Settings, "Settings")
)

/**
 * Primary bottom navigation: Home / Tasks / Agent / Reminders / Customize.
 * Settings lives behind the home-header gear.
 * Customize stays reachable from the AI Agent screen header.
 * Subtle selected pill, consistent icons and labels; tabs use
 * popUpTo + saveState / launchSingleTop / restoreState for predictable behavior.
 */
@Composable
fun PixelPalBottomBar(
    navController: NavController,
    selected: Screen
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            destinations.forEach { destination ->
                val isSelected = selected == destination.screen
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(200),
                    label = "navIconColor"
                )
                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        navController.navigate(destination.screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = null,
                            tint = iconColor
                        )
                    },
                    label = {
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

