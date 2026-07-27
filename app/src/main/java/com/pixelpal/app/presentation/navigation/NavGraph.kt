package com.pixelpal.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pixelpal.app.presentation.screens.customize.CustomizeScreen
import com.pixelpal.app.presentation.screens.home.HomeScreen
import com.pixelpal.app.presentation.screens.onboarding.OnboardingScreen
import com.pixelpal.app.presentation.screens.reminders.CreateReminderScreen
import com.pixelpal.app.presentation.screens.reminders.ReminderListScreen
import com.pixelpal.app.presentation.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Reminders : Screen("reminders")
    object CreateReminder : Screen("create_reminder")
    object Customize : Screen("customize")
    object Settings : Screen("settings")
}

@Composable
fun PixelPalNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
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
    }
}
