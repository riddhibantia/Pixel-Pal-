package com.pixelpal.app.presentation.screens.companions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.domain.engine.AgentMonitorEngine
import com.pixelpal.app.domain.engine.PersonalityEngine
import com.pixelpal.app.domain.model.ActivityEvent
import com.pixelpal.app.domain.model.AgentConfig
import com.pixelpal.app.domain.model.AgentStatus
import com.pixelpal.app.domain.model.Bond
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.Personality
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.model.Task
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.repository.BondRepository
import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.domain.repository.TaskRepository
import com.pixelpal.app.domain.usecase.agent.GetAgentConfigUseCase
import com.pixelpal.app.domain.usecase.agent.GetAgentStatusUseCase
import com.pixelpal.app.domain.usecase.agent.SaveAgentConfigUseCase
import com.pixelpal.app.domain.usecase.companion.ArchiveCompanionUseCase
import com.pixelpal.app.domain.usecase.companion.GetActiveCompanionUseCase
import com.pixelpal.app.domain.usecase.companion.GetCompanionsUseCase
import com.pixelpal.app.domain.usecase.companion.SetActiveCompanionUseCase
import com.pixelpal.app.domain.usecase.companion.ToggleFavoriteUseCase
import com.pixelpal.app.domain.usecase.companion.UpdateCompanionUseCase
import com.pixelpal.app.domain.usecase.task.AddTaskUseCase
import com.pixelpal.app.domain.usecase.task.CompleteTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompanionWorkspaceUiState(
    val companion: Companion? = null,
    val bond: Bond? = null,
    val personality: Personality? = null,
    val isActive: Boolean = false,
    val agentConfig: AgentConfig? = null,
    val agentStatus: AgentStatus? = null,
    val recentActivity: List<ActivityEvent> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val loading: Boolean = true
)

@HiltViewModel
class CompanionWorkspaceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getCompanionsUseCase: GetCompanionsUseCase,
    getActiveCompanionUseCase: GetActiveCompanionUseCase,
    getAgentConfigUseCase: GetAgentConfigUseCase,
    getAgentStatusUseCase: GetAgentStatusUseCase,
    bondRepository: BondRepository,
    personalityEngine: PersonalityEngine,
    activityEventRepository: ActivityEventRepository,
    reminderRepository: ReminderRepository,
    taskRepository: TaskRepository,
    private val agentMonitorEngine: AgentMonitorEngine,
    private val saveAgentConfigUseCase: SaveAgentConfigUseCase,
    private val setActiveCompanionUseCase: SetActiveCompanionUseCase,
    private val archiveCompanionUseCase: ArchiveCompanionUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val updateCompanionUseCase: UpdateCompanionUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase
) : ViewModel() {

    private val companionId: Long = savedStateHandle["companionId"] ?: -1L

    private data class Core(
        val companion: Companion?,
        val bond: Bond?,
        val personality: Personality?,
        val isActive: Boolean
    )

    private data class Extras(
        val agentConfig: AgentConfig?,
        val agentStatus: AgentStatus?,
        val activity: List<ActivityEvent>,
        val reminders: List<Reminder>,
        val tasks: List<Task>
    )

    private val core: kotlinx.coroutines.flow.Flow<Core> = combine(
        getCompanionsUseCase.getById(companionId),
        bondRepository.getBond(companionId),
        personalityEngine.getPersonality(companionId),
        getActiveCompanionUseCase.activeCompanion
    ) { c, bond, personality, active ->
        Core(c, bond, personality, active?.id == c?.id)
    }

    private val extras: kotlinx.coroutines.flow.Flow<Extras> = combine(
        getAgentConfigUseCase.getConfig(companionId),
        getAgentStatusUseCase.getStatus(companionId),
        activityEventRepository.getCenterEventsForCompanion(companionId, limit = 20),
        reminderRepository.getPendingForCompanion(companionId),
        taskRepository.getTasks(companionId)
    ) { config, status, activity, reminders, tasks ->
        Extras(config, status, activity, reminders, tasks)
    }

    val uiState: StateFlow<CompanionWorkspaceUiState> = combine(core, extras) { c, e ->
        CompanionWorkspaceUiState(
            companion = c.companion,
            bond = c.bond,
            personality = c.personality,
            isActive = c.isActive,
            agentConfig = e.agentConfig,
            agentStatus = e.agentStatus,
            recentActivity = e.activity,
            reminders = e.reminders,
            tasks = e.tasks,
            loading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompanionWorkspaceUiState())

    private val _checkingAgent = MutableStateFlow(false)
    val checkingAgent: StateFlow<Boolean> = _checkingAgent.asStateFlow()

    fun refreshAgentStatus() {
        if (companionId <= 0 || _checkingAgent.value) return
        viewModelScope.launch {
            _checkingAgent.value = true
            agentMonitorEngine.checkNow(companionId)
            _checkingAgent.value = false
        }
    }

    fun saveAgentConfig(enabled: Boolean, endpointUrl: String, pollIntervalMinutes: Long) {
        if (companionId <= 0) return
        viewModelScope.launch {
            saveAgentConfigUseCase(
                AgentConfig(
                    companionId = companionId,
                    endpointUrl = endpointUrl.trim(),
                    enabled = enabled,
                    pollIntervalMinutes = pollIntervalMinutes
                )
            )
        }
    }

    fun setActive() {
        if (companionId <= 0) return
        viewModelScope.launch { setActiveCompanionUseCase(companionId) }
    }

    fun archive() {
        if (companionId <= 0) return
        viewModelScope.launch { archiveCompanionUseCase(companionId) }
    }

    fun toggleFavorite() {
        val current = uiState.value.companion ?: return
        viewModelScope.launch { toggleFavoriteUseCase(current.id, !current.isFavorite) }
    }

    fun rename(name: String) {
        val current = uiState.value.companion ?: return
        viewModelScope.launch { updateCompanionUseCase(current.copy(name = name.take(20))) }
    }

    fun setDescription(description: String) {
        val current = uiState.value.companion ?: return
        viewModelScope.launch {
            updateCompanionUseCase(current.copy(description = description.trim().ifBlank { null }))
        }
    }

    fun addTask(title: String) {
        if (companionId <= 0 || title.isBlank()) return
        viewModelScope.launch { addTaskUseCase(companionId, title.trim()) }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch { completeTaskUseCase(task) }
    }
}