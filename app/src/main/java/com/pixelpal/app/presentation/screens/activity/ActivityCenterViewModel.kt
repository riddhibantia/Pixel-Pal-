package com.pixelpal.app.presentation.screens.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.domain.model.ActivityEvent
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.usecase.companion.GetActiveCompanionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Notification center. Source of truth is [ActivityEventRepository]
 * (meaningful events only — ordinary taps never reach this feed). Opening the
 * center marks everything as read so the Home bell badge clears.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActivityCenterViewModel @Inject constructor(
    private val activityEventRepository: ActivityEventRepository,
    getActiveCompanionUseCase: GetActiveCompanionUseCase
) : ViewModel() {

    /** null = show all activity (single-companion: equivalent, kept for filter UI). */
    private val selectedCompanionId = MutableStateFlow<Long?>(null)
    val selectedFilter: StateFlow<Long?> = selectedCompanionId.asStateFlow()

    val companion = getActiveCompanionUseCase.activeCompanion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val events: StateFlow<List<ActivityEvent>> =
        selectedCompanionId.flatMapLatest { id ->
            if (id == null) {
                activityEventRepository.getCenterEvents(limit = 100)
            } else {
                activityEventRepository.getCenterEventsForCompanion(id, limit = 100)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            activityEventRepository.markAllRead()
        }
    }

    fun filterBy(companionId: Long?) {
        selectedCompanionId.value = companionId
    }
}