package com.pixelpal.app.presentation.screens.customize

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.domain.model.PetType
import com.pixelpal.app.presentation.components.PetRenderer
import com.pixelpal.app.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeScreen(
    navController: NavController,
    viewModel: CustomizeViewModel = hiltViewModel()
) {
    val selectedPetId by viewModel.selectedPetType.collectAsState()
    val petName by viewModel.petName.collectAsState()
    val bond by viewModel.bond.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Customize", fontWeight = FontWeight.Bold) }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Home.route) },
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
                    selected = true,
                    onClick = { },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PetRenderer(petType = selectedPetId, animationState = AnimationState.HAPPY, size = 150.dp)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = petName,
                onValueChange = { viewModel.updatePetName(it) },
                label = { Text("Companion Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Select Companion", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(PetType.entries) { pet ->
                    val isUnlocked = bond.level >= pet.unlockBondLevel
                    val isSelected = selectedPetId.equals(pet.id, ignoreCase = true)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .clickable(enabled = isUnlocked) {
                                viewModel.selectPet(pet)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isUnlocked) pet.displayName else "🔒 ${pet.displayName}",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isUnlocked) "Unlocked" else "Bond Lvl ${pet.unlockBondLevel}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("App Theme", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("dark", "light", "spring", "autumn").forEach { theme ->
                    val isSelected = currentTheme.equals(theme, ignoreCase = true)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.selectTheme(theme) }
                    ) {
                        Text(
                            text = theme.capitalize(),
                            modifier = Modifier.padding(12.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun String.capitalize(): String = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
