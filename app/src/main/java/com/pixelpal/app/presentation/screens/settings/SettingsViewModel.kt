package com.pixelpal.app.presentation.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.usecase.companion.GetCompanionsUseCase
import com.pixelpal.app.overlay.OverlayService
import com.pixelpal.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    getCompanionsUseCase: GetCompanionsUseCase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: Context
) : ViewModel() {

    val overlayEnabled: StateFlow<Boolean> = preferencesManager.overlayEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    /** Companion ids the user explicitly picked for overlays (empty = default to active). */
    val overlayCompanionIds: StateFlow<Set<Long>> = preferencesManager.overlayCompanionIds
        .map { ids -> ids.mapNotNull(String::toLongOrNull).toSet() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val companions: StateFlow<List<Companion>> = getCompanionsUseCase.getAllActive()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val userName: StateFlow<String> = preferencesManager.userName
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val avatarSeed: StateFlow<String> = preferencesManager.avatarSeed
        .stateIn(viewModelScope, SharingStarted.Lazily, "pixelpal")

    val currentTheme: StateFlow<String> = preferencesManager.currentTheme
        .stateIn(viewModelScope, SharingStarted.Lazily, "dark")

    fun toggleOverlay(context: Context) {
        viewModelScope.launch {
            val next = !overlayEnabled.value
            preferencesManager.setOverlayEnabled(next)
            if (next) {
                OverlayService.start(appContext)
            } else {
                OverlayService.stop(appContext)
            }
        }
    }

    /**
     * Toggles one companion's overlay selection. Enforces
     * [Constants.MAX_SIMULTANEOUS_OVERLAYS]; checking beyond the cap is ignored.
     * Keeps the foreground service in sync so changes apply immediately.
     */
    fun toggleOverlayCompanion(companionId: Long) {
        viewModelScope.launch {
            val current = overlayCompanionIds.value
            val next = when (companionId in current) {
                true -> current - companionId
                false ->
                    if (current.size >= Constants.MAX_SIMULTANEOUS_OVERLAYS) current
                    else current + companionId
            }
            preferencesManager.setOverlayCompanionIds(next)
            syncOverlayService()
        }
    }

    /** Starts the overlay service when the master toggle is on; stops it when off. */
    private suspend fun syncOverlayService() {
        if (overlayEnabled.value) {
            OverlayService.start(appContext)
        } else {
            OverlayService.stop(appContext)
        }
    }

    fun resetOverlayPosition() {
        viewModelScope.launch {
            preferencesManager.updateOverlayPosition(
                Constants.OVERLAY_OFFSET_X_DP,
                Constants.OVERLAY_OFFSET_Y_DP
            )
            preferencesManager.clearOverlayPositions()
        }
    }

    fun selectTheme(theme: String) {
        viewModelScope.launch {
            preferencesManager.setCurrentTheme(theme)
        }
    }
}
