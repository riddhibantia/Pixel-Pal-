package com.pixelpal.app.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import com.pixelpal.app.presentation.navigation.Screen

private data class NavDestination(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

private val destinations = listOf(
    NavDestination(Screen.Home, Icons.Default.Home, "Home"),
    NavDestination(Screen.Reminders, Icons.Default.Notifications, "Reminders"),
    NavDestination(Screen.Customize, Icons.Default.Palette, "Customize"),
    NavDestination(Screen.Settings, Icons.Default.Settings, "Settings")
)

@Composable
fun PixelPalBottomBar(
    navController: NavController,
    selected: Screen
) {
    NavigationBar {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination.screen,
                onClick = { navController.navigate(destination.screen.route) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}