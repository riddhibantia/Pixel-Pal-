package com.pixelpal.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.presentation.navigation.PixelPalNavGraph
import com.pixelpal.app.presentation.navigation.Screen
import com.pixelpal.app.presentation.theme.PixelPalTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelPalTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val isFirstLaunch by preferencesManager.isFirstLaunch.collectAsState(initial = true)

                    val startDestination = if (isFirstLaunch) {
                        Screen.Onboarding.route
                    } else {
                        Screen.Home.route
                    }

                    PixelPalNavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}