package com.pixelpal.app.presentation.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.domain.model.AgentConnection
import com.pixelpal.app.domain.model.Bond
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.model.Task
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.repository.BondRepository
import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.domain.repository.TaskRepository
import com.pixelpal.app.domain.usecase.agent.GetAgentConnectionUseCase
import com.pixelpal.app.domain.usecase.companion.FeedCompanionUseCase
import com.pixelpal.app.domain.usecase.companion.GetActiveCompanionUseCase
import com.pixelpal.app.domain.usecase.companion.TapCompanionUseCase
import com.pixelpal.app.overlay.OverlayService
import com.pixelpal.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val companion: Companion? = null,
    val bond: Bond? = null,
    val pendingTasks: List<Task> = emptyList(),
    val completedTodayTasks: Int = 0,
    val nextReminder: Reminder? = null,
    val agentConnection: AgentConnection? = null,
    val unreadActivityCount: Int = 0,
    val overlayEnabled: Boolean = true
) {
    val isMaxBond: Boolean get() = (bond?.level ?: 0) >= Constants.MAX_BOND_LEVEL

    fun stateLabel(now: Long = System.currentTimeMillis()): String {
        val last = bond?.lastInteractionTime ?: 0L
        return if (now - last < RECENT_INTERACTION_WINDOW_MS) "Happy" else "Calm"
    }
}

private const val RECENT_INTERACTION_WINDOW_MS = 2 * 60 * 1000L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    getActiveCompanionUseCase: GetActiveCompanionUseCase,
    getAgentConnectionUseCase: GetAgentConnectionUseCase,
    private val activityEventRepository: ActivityEventRepository,
    private val taskRepository: TaskRepository,    private val reminderRepository: ReminderRepository,
    bondRepository: BondRepository,
    private val tapCompanionUseCase: TapCompanionUseCase,
    private val feedCompanionUseCase: FeedCompanionUseCase
) : ViewModel() {

    private val userName = preferencesManager.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val overlayEnabled = preferencesManager.overlayEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val unreadActivityCount = activityEventRepository.unreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val companion = getActiveCompanionUseCase.activeCompanion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private data class Features(
        val pendingTasks: List<Task> = emptyList(),
        val completedToday: Int = 0,
        val nextReminder: Reminder? = null
    )

    /** Feature queries re-keyed to THE companion's id. */
    private val features = companion.flatMapLatest { c ->
        if (c == null) flowOf(Features())
        else combine(
            taskRepository.getTasks(c.id),
            reminderRepository.getPendingForCompanion(c.id)
        ) { tasks, reminders ->
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            Features(
                pendingTasks = tasks.filter { !it.isDone },
                completedToday = tasks.count { it.isDone && (it.completedAt ?: 0L) >= startOfDay },
                nextReminder = reminders.minByOrNull { it.triggerTime }
            )
        }
    }

    private val bondFlow = companion.flatMapLatest { c ->
        if (c == null) flowOf(null) else bondRepository.getBond(c.id)
    }

    private val agentFlow = companion.flatMapLatest { c ->
        if (c == null) flowOf(null) else getAgentConnectionUseCase.getConnection(c.id)
    }


    val uiState: StateFlow<HomeUiState> = combine(
        companion, bondFlow, features, agentFlow,
        combine(userName, unreadActivityCount, overlayEnabled) { user, unread, overlay ->
            Triple(user, unread, overlay)
        }
    ) { c, bond, features, agent, meta ->
        HomeUiState(
            isLoading = false,
            userName = meta.first,
            companion = c,
            bond = bond,
            pendingTasks = features.pendingTasks,
            completedTodayTasks = features.completedToday,
            nextReminder = features.nextReminder,
            agentConnection = agent,
            unreadActivityCount = meta.second,
            overlayEnabled = meta.third
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Casual interaction with THE companion. */
    fun interactWithCompanion() {
        viewModelScope.launch { tapCompanionUseCase() }
    }

    /** Cosmetic-only interaction (no bond inflation by design). */
    fun feedCompanion() {
        viewModelScope.launch { feedCompanionUseCase() }
    }

    fun markActivitiesSeen() {
        viewModelScope.launch { activityEventRepository.markAllRead() }
    }

    fun toggleOverlay(context: Context) {
        viewModelScope.launch {
            val next = !overlayEnabled.value
            preferencesManager.setOverlayEnabled(next)
            if (next) {
                OverlayService.start(context)
            } else {
                OverlayService.stop(context)
            }
        }
    }
}