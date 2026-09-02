package com.pixelpal.app.presentation.screens.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.domain.model.Subtask
import com.pixelpal.app.domain.model.Task
import com.pixelpal.app.domain.repository.SubtaskRepository
import com.pixelpal.app.domain.repository.TaskRepository
import com.pixelpal.app.domain.usecase.task.CompleteTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val task: Task? = null,
    val subtasks: List<Subtask> = emptyList(),
    val loading: Boolean = true
) {
    val doneCount: Int get() = subtasks.count { it.isDone }
    val progress: Float
        get() = if (subtasks.isEmpty()) 0f else doneCount / subtasks.size.toFloat()
}

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val subtaskRepository: SubtaskRepository,
    private val completeTaskUseCase: CompleteTaskUseCase
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: -1L

    val uiState: StateFlow<TaskDetailUiState> = combine(
        taskRepository.getTask(taskId),
        subtaskRepository.getByTask(taskId)
    ) { task, subtasks ->
        TaskDetailUiState(task = task, subtasks = subtasks, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskDetailUiState())

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            if (task.isDone) {
                // Untick: flip local+cloud state without a second bond reward.
                taskRepository.toggleTask(task)
            } else {
                completeTaskUseCase(task)
            }
        }
    }

    fun saveTask(title: String, description: String?) {
        val current = uiState.value.task ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            taskRepository.updateTask(
                current.copy(
                    title = title.trim(),
                    description = description?.trim()?.takeIf { it.isNotEmpty() }
                )
            )
        }
    }

    fun deleteTask() {
        val current = uiState.value.task ?: return
        viewModelScope.launch { taskRepository.deleteTask(current) }
    }

    fun addSubtask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { subtaskRepository.add(taskId, title) }
    }

    fun toggleSubtask(subtask: Subtask) {
        viewModelScope.launch { subtaskRepository.toggle(subtask) }
    }

    fun renameSubtask(subtaskId: Long, title: String) {
        viewModelScope.launch { subtaskRepository.rename(subtaskId, title) }
    }

    fun deleteSubtask(subtask: Subtask) {
        viewModelScope.launch { subtaskRepository.delete(subtask) }
    }
}
