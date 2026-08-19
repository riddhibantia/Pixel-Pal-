package com.pixelpal.app.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pixelpal.app.presentation.screens.customize.CustomizeScreen
import com.pixelpal.app.presentation.screens.home.HomeScreen
import com.pixelpal.app.presentation.screens.onboarding.OnboardingScreen
import com.pixelpal.app.presentation.screens.reminders.CreateReminderScreen
import com.pixelpal.app.presentation.screens.reminders.ReminderListScreen
import com.pixelpal.app.presentation.screens.settings.AboutScreen
import com.pixelpal.app.presentation.screens.settings.OverlaySettingsScreen
import com.pixelpal.app.presentation.screens.settings.PermissionsScreen
import com.pixelpal.app.presentation.screens.settings.ProfileScreen
import com.pixelpal.app.presentation.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Reminders : Screen("reminders")
    object CreateReminder : Screen("create_reminder")
    object Customize : Screen("customize")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object OverlaySettings : Screen("overlay_settings")
    object About : Screen("about")
    object Permissions : Screen("permissions")
}

@Composable
fun PixelPalNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(220)) +
                slideInHorizontally(animationSpec = tween(220)) { it / 8 }
        },
        exitTransition = { fadeOut(animationSpec = tween(180)) },
        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
        popExitTransition = {
            fadeOut(animationSpec = tween(180)) +
                slideOutHorizontally(animationSpec = tween(220)) { it / 8 }
        }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Reminders.route) {
            ReminderListScreen(navController = navController)
        }
        composable(Screen.CreateReminder.route) {
            CreateReminderScreen(navController = navController)
        }
        composable(Screen.Customize.route) {
            CustomizeScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
        composable(Screen.OverlaySettings.route) {
            OverlaySettingsScreen(navController = navController)
        }
        composable(Screen.About.route) {
            AboutScreen(navController = navController)
        }
        composable(Screen.Permissions.route) {
            PermissionsScreen(navController = navController)
        }
    }
}
