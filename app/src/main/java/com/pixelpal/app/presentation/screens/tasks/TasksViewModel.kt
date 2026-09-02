package com.pixelpal.app.presentation.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.domain.model.Subtask
import com.pixelpal.app.domain.model.Task
import com.pixelpal.app.domain.repository.SubtaskRepository
import com.pixelpal.app.domain.repository.TaskRepository
import com.pixelpal.app.domain.usecase.companion.GetActiveCompanionUseCase
import com.pixelpal.app.domain.usecase.task.CompleteTaskUseCase
import com.pixelpal.app.data.local.datastore.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUiState(
    val items: List<TaskWithSubtasks> = emptyList(),
    val loading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TasksViewModel @Inject constructor(
    getActiveCompanionUseCase: GetActiveCompanionUseCase,
    private val taskRepository: TaskRepository,
    private val subtaskRepository: SubtaskRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val activeCompanion = getActiveCompanionUseCase.activeCompanion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val tasks = activeCompanion.flatMapLatest { companion ->
        if (companion == null) flowOf(emptyList()) else taskRepository.getTasks(companion.id)
    }

    private val tasksWithSubtasks = tasks.flatMapLatest { list ->
        if (list.isEmpty()) flowOf(emptyList())
        else combine(
            list.map { task ->
                subtaskRepository.getByTask(task.id).map { subtasks ->
                    TaskWithSubtasks(task, subtasks)
                }
            }
        ) { pairs -> pairs.toList() }
    }

    val uiState: StateFlow<TasksUiState> = tasksWithSubtasks
        .map { items -> TasksUiState(items = items, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    /** Home-screen widget opt-in (moved from the workspace). */
    val tasksWidgetEnabled: StateFlow<Boolean> = preferencesManager.tasksWidgetEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setTasksWidgetEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setTasksWidgetEnabled(enabled) }
    }

    /**
     * Creates the task from the New Task screen, then its subtasks in order.
     */
    fun createTask(title: String, description: String?, subtaskTitles: List<String>) {
        val companionId = activeCompanion.value?.id ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            val id = taskRepository.addTask(
                Task(
                    companionId = companionId,
                    title = title.trim(),
                    description = description?.trim()?.takeIf { it.isNotEmpty() }
                )
            )
            subtaskTitles.filter { it.isNotBlank() }.forEach { subtaskRepository.add(id, it) }
        }
    }

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

    fun deleteTask(task: Task) {
        viewModelScope.launch { taskRepository.deleteTask(task) }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch { taskRepository.updateTask(task) }
    }

    fun addSubtask(task: Task, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { subtaskRepository.add(task.id, title) }
    }

    fun toggleSubtask(subtask: Subtask) {
        viewModelScope.launch { subtaskRepository.toggle(subtask) }
    }

    fun deleteSubtask(subtask: Subtask) {
        viewModelScope.launch { subtaskRepository.delete(subtask) }
    }
}
