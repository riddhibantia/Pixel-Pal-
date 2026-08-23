package com.pixelpal.app.presentation.screens.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.usecase.companion.GetCompanionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * In-app notification center. Source of truth is [ActivityEventRepository]
 * (meaningful events only — ordinary taps never reach this feed). Opening the
 * center marks everything as read so the Home bell badge clears.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActivityCenterViewModel @Inject constructor(
    private val activityEventRepository: ActivityEventRepository,
    getCompanionsUseCase: GetCompanionsUseCase
) : ViewModel() {

    /** null = show all companions' activity. */
    private val selectedCompanionId = MutableStateFlow<Long?>(null)
    val selectedFilter: StateFlow<Long?> = selectedCompanionId.asStateFlow()

    val companions: StateFlow<List<Companion>> = getCompanionsUseCase.getAllActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val events: StateFlow<List<com.pixelpal.app.domain.model.ActivityEvent>> =
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