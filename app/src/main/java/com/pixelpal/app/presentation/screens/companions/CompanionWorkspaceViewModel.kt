package com.pixelpal.app.presentation.screens.companions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.domain.model.AgentConnection
import com.pixelpal.app.domain.model.Bond
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.model.Task
import com.pixelpal.app.domain.model.AgentState
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.domain.repository.AgentConnectionRepository
import com.pixelpal.app.domain.repository.BondRepository
import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.map
import com.pixelpal.app.domain.usecase.agent.GetAgentConnectionUseCase
import com.pixelpal.app.domain.usecase.agent.SaveAgentConnectionUseCase
import com.pixelpal.app.domain.usecase.companion.GetActiveCompanionUseCase
import com.pixelpal.app.domain.usecase.companion.ToggleFavoriteUseCase
import com.pixelpal.app.domain.usecase.task.AddTaskUseCase
import com.pixelpal.app.domain.usecase.task.CompleteTaskUseCase
import com.pixelpal.app.presentation.components.SnackbarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompanionWorkspaceUiState(
    val companion: Companion? = null,
    val bond: Bond? = null,
    val tasks: List<Task> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val agentConnection: AgentConnection? = null,
    val tasksWidgetEnabled: Boolean = false,
    val checkingAgent: Boolean = false,
    val loading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CompanionWorkspaceViewModel @Inject constructor(
    getActiveCompanionUseCase: GetActiveCompanionUseCase,
    bondRepository: BondRepository,
    reminderRepository: ReminderRepository,
    private val taskRepository: TaskRepository,
    getAgentConnectionUseCase: GetAgentConnectionUseCase,
    private val preferencesManager: PreferencesManager,
    private val agentConnectionRepository: AgentConnectionRepository,
    private val saveAgentConnectionUseCase: SaveAgentConnectionUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val updateCompanionUseCase: com.pixelpal.app.domain.usecase.companion.UpdateCompanionUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase
) : ViewModel() {

    private val core = getActiveCompanionUseCase.activeCompanion.flatMapLatest { c ->
        if (c == null) flowOf<Pair<Companion?, Bond?>>(null to null)
        else bondRepository.getBond(c.id).map { bond -> c to bond }
    }

    private data class Extras(
        val tasks: List<Task>,
        val reminders: List<Reminder>,
        val agent: AgentConnection?
    )

    private val extras = getActiveCompanionUseCase.activeCompanion.flatMapLatest { c ->
        if (c == null) flowOf(Extras(emptyList(), emptyList(), null))
        else combine(
            taskRepository.getTasks(c.id),
            reminderRepository.getPendingForCompanion(c.id),
            getAgentConnectionUseCase.getConnection(c.id)
        ) { tasks, reminders, agent ->
            Extras(tasks, reminders, agent)
        }
    }

    private val tasksWidgetEnabled = preferencesManager.tasksWidgetEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val uiState: StateFlow<CompanionWorkspaceUiState> = combine(
        combine(core, extras) { corePair, extras ->
            val (companion, bond) = corePair
            CompanionWorkspaceUiState(
                companion = companion,
                bond = bond,
                tasks = extras.tasks,
                reminders = extras.reminders,
                agentConnection = extras.agent,
                loading = false
            )
        },
        tasksWidgetEnabled
    ) { state, widgetEnabled ->
        state.copy(tasksWidgetEnabled = widgetEnabled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompanionWorkspaceUiState())

    private val _checkingAgent = MutableStateFlow(false)
    val checkingAgent: StateFlow<Boolean> = _checkingAgent.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<SnackbarEvent>()
    val snackbarEvents: SharedFlow<SnackbarEvent> = _snackbarEvents.asSharedFlow()

    fun refreshAgentStatus() {
        val companionId = uiState.value.companion?.id ?: return
        if (_checkingAgent.value) return
        viewModelScope.launch {
            _checkingAgent.value = true
            try {
                agentConnectionRepository.checkNow(companionId)
            } finally {
                _checkingAgent.value = false
            }
        }
    }

    fun saveAgentConnection(connection: AgentConnection) {
        viewModelScope.launch { saveAgentConnectionUseCase(connection) }
    }

    fun disconnectAgent() {
        val connection = uiState.value.agentConnection ?: return
        viewModelScope.launch {
            saveAgentConnectionUseCase(
                connection.copy(
                    endpointUrl = "",
                    pollingEnabled = false,
                    connectionStatus = com.pixelpal.app.domain.model.ConnectionStatus.DISCONNECTED,
                    currentStatus = AgentState.DISCONNECTED,
                    currentTask = null,
                    progress = null,
                    lastMessage = null,
                    errorMessage = null
                )
            )
            _snackbarEvents.emit(SnackbarEvent.AgentDisconnected(connection))
        }
    }

    fun undoDisconnect(connection: AgentConnection) {
        viewModelScope.launch { saveAgentConnectionUseCase(connection) }
    }

    fun toggleFavorite() {
        val current = uiState.value.companion ?: return
        viewModelScope.launch { toggleFavoriteUseCase(current.id, !current.isFavorite) }
    }

    fun rename(name: String) {
        val current = uiState.value.companion ?: return
        if (name.isBlank()) return
        viewModelScope.launch { updateCompanionUseCase(current.copy(name = name.take(20))) }
    }

    fun addTask(title: String) {
        val companionId = uiState.value.companion?.id ?: return
        if (title.isBlank()) return
        viewModelScope.launch { addTaskUseCase(companionId, title.trim()) }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch { completeTaskUseCase(task) }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
            _snackbarEvents.emit(SnackbarEvent.TaskDeleted(task))
        }
    }

    fun undoDeleteTask(task: Task) {
        viewModelScope.launch { taskRepository.reinsertTask(task) }
    }

    fun setTasksWidgetEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setTasksWidgetEnabled(enabled) }
    }
}