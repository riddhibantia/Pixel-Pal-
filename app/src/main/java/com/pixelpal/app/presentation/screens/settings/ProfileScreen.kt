package com.pixelpal.app.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.presentation.components.AppTextField
import com.pixelpal.app.presentation.components.AppTopBar
import com.pixelpal.app.presentation.components.ConfirmationDialog
import com.pixelpal.app.presentation.components.PixelAvatar
import com.pixelpal.app.presentation.components.PrimaryButton
import com.pixelpal.app.presentation.components.SectionHeader
import com.pixelpal.app.presentation.theme.Radius
import com.pixelpal.app.presentation.theme.Spacing

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

/**
 * Edit Profile — warm, personal space. Local profile only (no auth system):
 * name, email and a cozy deterministic avatar.
 */
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var avatarSeed by remember { mutableStateOf("pixelpal") }
    var originalName by remember { mutableStateOf("") }
    var originalEmail by remember { mutableStateOf("") }
    var originalSeed by remember { mutableStateOf("pixelpal") }
    var loaded by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var attemptedSave by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Load stored profile once, directly from DataStore (avoids StateFlow's
    // initial-value race where the form would start blank).
    LaunchedEffect(Unit) {
        if (!loaded) {
            originalName = viewModel.currentUserName()
            originalEmail = viewModel.currentUserEmail()
            originalSeed = viewModel.currentAvatarSeed()
            name = originalName
            email = originalEmail
            avatarSeed = originalSeed
            loaded = true
        }
    }

    val currentName = name
    val currentEmail = email
    val currentSeed = avatarSeed

    val nameError = attemptedSave && currentName.isBlank()
    val emailError = attemptedSave && currentEmail.isNotEmpty() && !EMAIL_REGEX.matches(currentEmail)
    val hasChanges = loaded && (
        currentName != originalName ||
            currentEmail != originalEmail ||
            currentSeed != originalSeed
        )

    fun attemptBack() {
        if (hasChanges) showDiscardDialog = true else navController.popBackStack()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Profile", onBack = { attemptBack() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(bottom = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Warm avatar header
            PixelAvatar(seed = currentSeed, size = 96.dp)
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = currentName.ifBlank { "Your name" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "This is your personal space in PixelPal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            IconButton(onClick = {
                avatarSeed = viewModel.nextAvatarSeed(currentSeed)
            }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Change avatar style",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionHeader(title = "Personal Information")

            Surface(
                shape = RoundedCornerShape(Radius.large),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    AppTextField(
                        value = currentName,
                        onValueChange = { name = it },
                        label = "Name",
                        placeholder = "Your name",
                        isError = nameError,
                        supportingText = if (nameError) "Name is required" else null
                    )
                    AppTextField(
                        value = currentEmail,
                        onValueChange = { email = it },
                        label = "Email (optional)",
                        placeholder = "you@example.com",
                        keyboardType = KeyboardType.Email,
                        isError = emailError,
                        supportingText = when {
                            emailError -> "That email doesn't look right"
                            else -> null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            SectionHeader(title = "Cloud Sync & Firestore")

            Surface(
                shape = RoundedCornerShape(Radius.large),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    val isSyncing by viewModel.isSyncing.collectAsState()
                    val accountText = when {
                        !viewModel.isUserLoggedIn -> "Not Connected to Cloud"
                        viewModel.isAnonymousUser -> "Guest Mode (Local & Cloud Sync Active)"
                        else -> "Connected: ${viewModel.currentUserEmail ?: "Authenticated"}"
                    }

                    Text(
                        text = accountText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Firestore automatically backs up your pet, tasks, reminders, and streaks in real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(Spacing.xs))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        com.pixelpal.app.presentation.components.SecondaryButton(
                            text = if (isSyncing) "Syncing..." else "Sync Now",
                            enabled = !isSyncing && viewModel.isUserLoggedIn,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.triggerCloudSync() }
                        )

                        com.pixelpal.app.presentation.components.SecondaryButton(
                            text = if (viewModel.isUserLoggedIn && !viewModel.isAnonymousUser) "Account" else "Log In",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                navController.navigate(com.pixelpal.app.presentation.navigation.Screen.Auth.route)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            PrimaryButton(
                text = "Save Changes",
                enabled = !nameError && !emailError,
                onClick = {
                    attemptedSave = true
                    if (currentName.isNotBlank() &&
                        (currentEmail.isEmpty() || EMAIL_REGEX.matches(currentEmail))
                    ) {
                        focusManager.clearFocus()
                        viewModel.saveProfile(currentName, currentEmail, currentSeed)
                        navController.popBackStack()
                    }
                }
            )
        }
    }

    if (showDiscardDialog) {
        ConfirmationDialog(
            title = "Discard changes?",
            message = "You have unsaved changes to your profile.",
            confirmLabel = "Discard",
            destructive = true,
            onConfirm = {
                showDiscardDialog = false
                navController.popBackStack()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }
}
