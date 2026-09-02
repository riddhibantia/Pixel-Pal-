package com.pixelpal.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.data.remote.firebase.FirebaseAuthManager
import com.pixelpal.app.presentation.navigation.PixelPalNavGraph
import com.pixelpal.app.presentation.navigation.Screen
import com.pixelpal.app.presentation.theme.PixelPalTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var authManager: FirebaseAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold the splash until the start destination is known; swapping
        // startDestination on a live NavHost is unsupported.
        var isNavReady = false
        splashScreen.setKeepOnScreenCondition { !isNavReady }

        setContent {
            val currentTheme by preferencesManager.currentTheme.collectAsState(initial = "dark")
            PixelPalTheme(theme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Android 13+ runtime notification permission — without it
                    // reminder alarms play sound but show no notification.
                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { }
                    LaunchedEffect(Unit) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // Auth gate: onboarding on first launch, then every user
                    // must sign in (own profile) before reaching the app.
                    var start by remember { mutableStateOf<String?>(null) }
                    LaunchedEffect(Unit) {
                        val isFirstLaunch = preferencesManager.isFirstLaunch.first()
                        start = when {
                            isFirstLaunch -> Screen.Onboarding.route
                            authManager.currentUser == null -> Screen.Auth.route
                            else -> Screen.Home.route
                        }
                        isNavReady = true
                    }

                    val startRoute = start
                    if (startRoute != null) {
                        val navController = rememberNavController()
                        PixelPalNavGraph(
                            navController = navController,
                            startDestination = startRoute
                        )
                    }
                }
            }
        }
    }
}
