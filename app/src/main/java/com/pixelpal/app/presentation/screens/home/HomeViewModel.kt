package com.pixelpal.app.presentation.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.animation.AnimationEngine
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.domain.model.AgentState
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.repository.AgentStatusRepository
import com.pixelpal.app.domain.repository.BondRepository
import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.domain.repository.TaskRepository
import com.pixelpal.app.domain.usecase.companion.FeedCompanionUseCase
import com.pixelpal.app.domain.usecase.companion.GetCompanionsUseCase
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

/**
 * One vertically-stacked dashboard card on Home. Every field is sourced from
 * flows scoped to THIS companion only — nothing here leaks across companions.
 */
data class CompanionCardUi(
    val companion: Companion,
    val bondLevel: Int,
    val totalInteractions: Int,
    val streakDays: Int,
    val lastInteractionTime: Long,
    val pendingTasks: Int = 0,
    val completedTodayTasks: Int = 0,
    val nextReminderTime: Long? = null,
    val agentState: AgentState? = null,
    val agentLastCheckedAt: Long? = null
) {
    /** Per-companion state label derived from its own interaction recency (not the global emotion engine). */
    fun stateLabel(now: Long = System.currentTimeMillis()): String {
        val recentInteraction = now - lastInteractionTime < RECENT_INTERACTION_WINDOW_MS
        return if (recentInteraction) "Happy" else "Calm"
    }

    val isMaxBond: Boolean get() = bondLevel >= Constants.MAX_BOND_LEVEL
}

private const val RECENT_INTERACTION_WINDOW_MS = 2 * 60 * 1000L

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val cards: List<CompanionCardUi> = emptyList(),
    val canCreateCompanion: Boolean = true,
    val unreadActivityCount: Int = 0,
    val overlayEnabled: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    animationEngine: AnimationEngine, // retained: drives in-app sprite states elsewhere
    getCompanionsUseCase: GetCompanionsUseCase,
    private val bondRepository: BondRepository,
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val agentStatusRepository: AgentStatusRepository,
    private val activityEventRepository: ActivityEventRepository,
    private val tapCompanionUseCase: TapCompanionUseCase,
    private val feedCompanionUseCase: FeedCompanionUseCase
) : ViewModel() {

    private val userName = preferencesManager.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val overlayEnabled = preferencesManager.overlayEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val unreadActivityCount = activityEventRepository.unreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** One scoped flow per companion; combine over a dynamic list. */
    private fun cardFlow(companion: Companion) = combine(
        bondRepository.getBond(companion.id),
        taskRepository.getTasks(companion.id),
        reminderRepository.getPendingForCompanion(companion.id),
        agentStatusRepository.getStatus(companion.id)
    ) { bond, tasks, reminders, status ->
        CompanionCardUi(
            companion = companion,
            bondLevel = bond.level,
            totalInteractions = bond.totalInteractions,
            streakDays = bond.streakDays,
            lastInteractionTime = bond.lastInteractionTime,
            pendingTasks = tasks.count { !it.isDone },
            completedTodayTasks = tasks.count { it.isDone && (it.completedAt ?: 0L) >= startOfToday() },
            nextReminderTime = reminders.minOfOrNull { it.triggerTime },
            agentState = status?.state,
            agentLastCheckedAt = status?.lastCheckedAt
        )
    }

    private val cards = getCompanionsUseCase.getAllActive().flatMapLatest { companions ->
        if (companions.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(companions.map { cardFlow(it) }) { values ->
                values.toList()
                    .sortedWith(
                        compareByDescending<CompanionCardUi> { it.companion.isFavorite }
                            .thenByDescending { it.companion.lastUsedAt ?: 0L }
                    )
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        cards,
        userName,
        unreadActivityCount,
        overlayEnabled
    ) { cards, user, unread, overlay ->
        HomeUiState(
            isLoading = false,
            userName = user,
            cards = cards,
            canCreateCompanion = cards.size < Constants.MAX_ACTIVE_COMPANIONS,
            unreadActivityCount = unread,
            overlayEnabled = overlay
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Interact (tap/play) with one specific companion from its card. */
    fun interactWith(companionId: Long) {
        viewModelScope.launch { tapCompanionUseCase(companionId) }
    }

    /** Feed one specific companion from its card. */
    fun feedCompanion(companionId: Long) {
        viewModelScope.launch { feedCompanionUseCase(companionId) }
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

    private fun startOfToday(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}