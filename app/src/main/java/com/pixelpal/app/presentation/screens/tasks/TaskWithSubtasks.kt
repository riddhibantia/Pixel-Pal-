package com.pixelpal.app.presentation.screens.tasks

import com.pixelpal.app.domain.model.Subtask
import com.pixelpal.app.domain.model.Task

/** One task heading plus its subtask checklist. */
data class TaskWithSubtasks(
    val task: Task,
    val subtasks: List<Subtask> = emptyList()
)
