package com.pixelpal.app.presentation.screens.companions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.data.local.datastore.CompanionBootstrapInitializer
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.usecase.companion.ArchiveCompanionUseCase
import com.pixelpal.app.domain.usecase.companion.GetActiveCompanionUseCase
import com.pixelpal.app.domain.usecase.companion.GetCompanionsUseCase
import com.pixelpal.app.domain.usecase.companion.RestoreCompanionUseCase
import com.pixelpal.app.domain.usecase.companion.SetActiveCompanionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import com.pixelpal.app.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompanionManagerUiState(
    val companions: List<Companion> = emptyList(),
    val archivedCompanions: List<Companion> = emptyList(),
    val activeCompanionId: Long? = null,
    val loading: Boolean = true
) {
    val archivedCount: Int get() = archivedCompanions.size
    val canCreate: Boolean get() = companions.size < Constants.MAX_ACTIVE_COMPANIONS
}

@HiltViewModel
class CompanionsViewModel @Inject constructor(
    getCompanionsUseCase: GetCompanionsUseCase,
    getActiveCompanionUseCase: GetActiveCompanionUseCase,
    private val setActiveCompanionUseCase: SetActiveCompanionUseCase,
    private val archiveCompanionUseCase: ArchiveCompanionUseCase,
    private val restoreCompanionUseCase: RestoreCompanionUseCase,
    private val bootstrapInitializer: CompanionBootstrapInitializer
) : ViewModel() {

    val uiState: StateFlow<CompanionManagerUiState> = combine(
        getCompanionsUseCase.getAllActive(),
        getCompanionsUseCase.getArchived(),
        getActiveCompanionUseCase.activeCompanion
    ) { activeList, archived, active ->
        CompanionManagerUiState(
            companions = activeList,
            archivedCompanions = archived,
            activeCompanionId = active?.id,
            loading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompanionManagerUiState())

    init {
        viewModelScope.launch {
            bootstrapInitializer.ensureInitialized()
        }
    }

    fun setActive(companionId: Long) {
        viewModelScope.launch {
            setActiveCompanionUseCase(companionId)
        }
    }

    fun archive(companion: Companion) {
        viewModelScope.launch {
            archiveCompanionUseCase(companion.id)
        }
    }

    fun restore(companionId: Long) {
        viewModelScope.launch {
            restoreCompanionUseCase(companionId)
        }
    }
}