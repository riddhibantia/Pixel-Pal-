package com.pixelpal.app.presentation.components

import com.pixelpal.app.domain.model.AgentConnection
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.model.Task

/**
 * Events that trigger an undo-capable snackbar across the app.
 */
sealed class SnackbarEvent(val message: String) {
    data class TaskDeleted(val task: Task) : SnackbarEvent("Task deleted")
    data class ReminderDeleted(val reminder: Reminder) : SnackbarEvent("Reminder deleted")
    data class AgentDisconnected(val connection: AgentConnection) : SnackbarEvent("Agent disconnected")
    data class Generic(val text: String) : SnackbarEvent(text)
}
