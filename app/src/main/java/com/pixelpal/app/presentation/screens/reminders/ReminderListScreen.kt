package com.pixelpal.app.presentation.screens.reminders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.pixelpal.app.presentation.components.ReminderCard
import com.pixelpal.app.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    navController: NavController,
    viewModel: ReminderViewModel = hiltViewModel()
) {
    val pendingReminders by viewModel.pendingReminders.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reminders", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.CreateReminder.route) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Home.route) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
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
        if (pendingReminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No pending reminders!\nTap + to set one.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(pendingReminders, key = { it.id }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onComplete = { viewModel.completeReminder(reminder.id) },
                        onDelete = { viewModel.deleteReminder(reminder) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
