package com.pixelpal.app.presentation.screens.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelpal.app.domain.model.Subtask
import com.pixelpal.app.presentation.components.AppTopBar
import com.pixelpal.app.presentation.components.AppTextField
import com.pixelpal.app.presentation.components.ConfirmationDialog
import com.pixelpal.app.presentation.components.DestructiveButton
import com.pixelpal.app.presentation.components.LoadingState
import com.pixelpal.app.presentation.components.PrimaryButton
import com.pixelpal.app.presentation.components.SectionHeader
import com.pixelpal.app.presentation.theme.Radius
import com.pixelpal.app.presentation.theme.Spacing

/**
 * Task Detail: view/edit the task, tick it complete, manage its subtasks, or
 * delete it. Everything persists through the existing Room + Firestore sync.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    navController: NavController,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val state = uiState

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (state.loading) {
        Scaffold { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                AppTopBar(title = "Task", onBack = { navController.popBackStack() })
                LoadingState()
            }
        }
        return
    }

    val task = state.task
    if (task == null) {
        // Deleted from elsewhere (e.g. cloud sync) — leave the screen.
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    var editedTitle by remember(task.id) { mutableStateOf(task.title) }
    var editedDescription by remember(task.id) { mutableStateOf(task.description ?: "") }
    var newSubtask by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TopAppBar(
                title = { Text("Task", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal)
                    .padding(top = Spacing.xs, bottom = Spacing.md)
            ) {
                // ── Editable task heading ──
                AppTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = "Task title",
                    isError = editedTitle.isBlank()
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                AppTextField(
                    value = editedDescription,
                    onValueChange = { editedDescription = it },
                    label = "Description (optional)",
                    placeholder = "Add a description…",
                    singleLine = false,
                    modifier = Modifier.height(110.dp)
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // ── Main task completion ──
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.large),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = task.isDone, onCheckedChange = { viewModel.toggleTask(task) })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (task.isDone) "Completed" else "Mark task complete",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                            )
                            if (state.subtasks.isNotEmpty()) {
                                Text(
                                    text = "${state.doneCount}/${state.subtasks.size} subtasks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (state.subtasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                // ── Subtasks ──
                SectionHeader(title = "Subtasks")

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.large),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(vertical = Spacing.xs)) {
                        if (state.subtasks.isEmpty()) {
                            Text(
                                text = "No subtasks yet — add the smaller steps below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                            )
                        }

                        state.subtasks.forEachIndexed { index, subtask ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            }
                            SubtaskRow(
                                subtask = subtask,
                                onToggle = { viewModel.toggleSubtask(subtask) },
                                onRename = { viewModel.renameSubtask(subtask.id, it) },
                                onDelete = { viewModel.deleteSubtask(subtask) }
                            )
                        }

                        // Inline add-subtask row (task already exists here).
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.addSubtask(newSubtask)
                                    newSubtask = ""
                                },
                                enabled = newSubtask.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add subtask",
                                    tint = if (newSubtask.isNotBlank()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            OutlinedTextField(
                                value = newSubtask,
                                onValueChange = { newSubtask = it },
                                placeholder = { Text("Add a subtask…") },
                                singleLine = true,
                                shape = RoundedCornerShape(Radius.medium),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                // ── Edit & delete actions ──
                PrimaryButton(
                    text = "Save Changes",
                    enabled = editedTitle.isNotBlank() &&
                        (editedTitle.trim() != task.title || editedDescription.trim() != (task.description ?: "")),
                    onClick = {
                        viewModel.saveTask(editedTitle, editedDescription)
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                DestructiveButton(
                    text = "Delete Task",
                    onClick = { showDeleteDialog = true }
                )
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "Delete task?",
            message = "\"${task.title}\" and its subtasks will be removed from this device and the cloud.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            destructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteTask()
                navController.popBackStack()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

/**
 * A saved subtask row: checkbox, editable title (committed on focus loss),
 * delete action.
 */
@Composable
private fun SubtaskRow(
    subtask: Subtask,
    onToggle: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var text by remember(subtask.id, subtask.title) { mutableStateOf(subtask.title) }
    var focused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = subtask.isDone, onCheckedChange = { onToggle() })
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                textDecoration = if (subtask.isDone) TextDecoration.LineThrough else null
            ),
            shape = RoundedCornerShape(Radius.medium),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    if (focused && !focusState.isFocused && text.trim() != subtask.title) {
                        onRename(text)
                    }
                    focused = focusState.isFocused
                }
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete subtask",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
