package com.pixelpal.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
                    var isFirstLaunch by remember { mutableStateOf<Boolean?>(null) }
                    LaunchedEffect(Unit) {
                        isFirstLaunch = preferencesManager.isFirstLaunch.first()
                        isNavReady = true
                    }

                    val start = isFirstLaunch?.let { first ->
                        if (first) Screen.Onboarding.route else Screen.Home.route
                    }

                    if (start != null) {
                        val navController = rememberNavController()
                        PixelPalNavGraph(
                            navController = navController,
                            startDestination = start
                        )
                    }
                }
            }
        }
    }
}