package com.pixelpal.app.presentation.screens.reminders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderScreen(
    navController: NavController,
    viewModel: ReminderViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var delayMinutes by remember { mutableStateOf("5") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Reminder", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = delayMinutes,
                onValueChange = { delayMinutes = it },
                label = { Text("Remind in (Minutes)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val minutes = delayMinutes.toLongOrNull() ?: 5L
                    val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
                    if (title.isNotBlank()) {
                        viewModel.createReminder(title, message.ifEmpty { null }, triggerTime)
                        navController.popBackStack()
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Reminder", fontSize = 16.sp)
            }
        }
    }
}
