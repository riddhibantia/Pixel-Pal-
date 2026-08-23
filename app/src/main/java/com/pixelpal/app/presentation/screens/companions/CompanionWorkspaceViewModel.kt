package com.pixelpal.app.presentation.screens.companions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.domain.model.ActivityEvent
import com.pixelpal.app.domain.model.AgentConnection
import com.pixelpal.app.domain.model.AgentState
import com.pixelpal.app.domain.model.Bond
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.Personality
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.model.Task
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.repository.AgentConnectionRepository
import com.pixelpal.app.domain.repository.BondRepository
import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.domain.repository.TaskRepository
import com.pixelpal.app.domain.usecase.agent.GetAgentConnectionUseCase
import com.pixelpal.app.domain.usecase.agent.SaveAgentConnectionUseCase
import com.pixelpal.app.domain.usecase.companion.GetActiveCompanionUseCase
import com.pixelpal.app.domain.usecase.companion.ToggleFavoriteUseCase
import com.pixelpal.app.domain.usecase.task.AddTaskUseCase
import com.pixelpal.app.domain.usecase.task.CompleteTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    val personality: Personality? = null,
    val tasks: List<Task> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val agentConnection: AgentConnection? = null,
    val recentActivity: List<ActivityEvent> = emptyList(),
    val checkingAgent: Boolean = false,
    val loading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CompanionWorkspaceViewModel @Inject constructor(
    getActiveCompanionUseCase: GetActiveCompanionUseCase,
    bondRepository: BondRepository,
    activityEventRepository: ActivityEventRepository,
    reminderRepository: ReminderRepository,
    taskRepository: TaskRepository,
    private val personalityEngine: com.pixelpal.app.domain.engine.PersonalityEngine,
    getAgentConnectionUseCase: GetAgentConnectionUseCase,
    private val agentConnectionRepository: AgentConnectionRepository,
    private val saveAgentConnectionUseCase: SaveAgentConnectionUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val updateCompanionUseCase: com.pixelpal.app.domain.usecase.companion.UpdateCompanionUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase
) : ViewModel() {

    private data class Core(
        val companion: Companion?,
        val bond: Bond?,
        val personality: Personality?
    )

    private data class Extras(
        val tasks: List<Task>,
        val reminders: List<Reminder>,
        val agent: AgentConnection?,
        val activity: List<ActivityEvent>
    )

    private val core = getActiveCompanionUseCase.activeCompanion.flatMapLatest { c ->
        if (c == null) flowOf(Core(null, null, null))
        else combine(
            bondRepository.getBond(c.id),
            personalityEngine.getPersonality(c.id)
        ) { bond, personality -> Core(c, bond, personality) }
    }

    private val extras = getActiveCompanionUseCase.activeCompanion.flatMapLatest { c ->
        if (c == null) flowOf(Extras(emptyList(), emptyList(), null, emptyList()))
        else combine(
            taskRepository.getTasks(c.id),
            reminderRepository.getPendingForCompanion(c.id),
            getAgentConnectionUseCase.getConnection(c.id),
            activityEventRepository.getCenterEventsForCompanion(c.id, limit = 20)
        ) { tasks, reminders, agent, activity ->
            Extras(tasks, reminders, agent, activity)
        }
    }

    val uiState: StateFlow<CompanionWorkspaceUiState> = combine(core, extras) { core, extras ->
        CompanionWorkspaceUiState(
            companion = core.companion,
            bond = core.bond,
            personality = core.personality,
            tasks = extras.tasks,
            reminders = extras.reminders,
            agentConnection = extras.agent,
            recentActivity = extras.activity,
            loading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompanionWorkspaceUiState())

    private val _checkingAgent = MutableStateFlow(false)
    val checkingAgent: StateFlow<Boolean> = _checkingAgent.asStateFlow()

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
        }
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
}