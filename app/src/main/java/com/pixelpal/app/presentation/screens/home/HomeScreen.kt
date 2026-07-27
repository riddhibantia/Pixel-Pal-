package com.pixelpal.app.presentation.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.presentation.components.PetRenderer
import com.pixelpal.app.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val petName by viewModel.petName.collectAsState()
    val petType by viewModel.selectedPetType.collectAsState()
    val overlayEnabled by viewModel.overlayEnabled.collectAsState()
    val currentAnim by viewModel.currentAnimation.collectAsState()
    val currentEmotion by viewModel.currentEmotion.collectAsState()
    val bond by viewModel.bond.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PixelPal", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Reminders.route) },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Reminders") },
                    label = { Text("Reminders") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Customize.route) },
                    icon = { Icon(Icons.Default.Palette, contentDescription = "Customize") },
                    label = { Text("Customize") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Settings.route) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            PetRenderer(
                petType = petType,
                animationState = currentAnim,
                size = 180.dp,
                modifier = Modifier.clickable { viewModel.tapPet() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = petName,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Emotion: ${currentEmotion.name.lowercase().capitalize()}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Friendship Bond", fontWeight = FontWeight.SemiBold)
                        Text("Level ${bond.level}/100", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { bond.level / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total Interactions: ${bond.totalInteractions}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.feedPet() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🍎 Feed $petName")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Screen Companion Overlay", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (overlayEnabled) "Active on screen" else "Disabled",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = overlayEnabled,
                        onCheckedChange = { viewModel.toggleOverlay(context) }
                    )
                }
            }
        }
    }
}

private fun String.capitalize(): String = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
