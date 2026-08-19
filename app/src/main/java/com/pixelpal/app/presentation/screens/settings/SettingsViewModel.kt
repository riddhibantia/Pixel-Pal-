package com.pixelpal.app.presentation.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.overlay.OverlayService
import com.pixelpal.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val overlayEnabled: StateFlow<Boolean> = preferencesManager.overlayEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

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
                OverlayService.start(context)
            } else {
                OverlayService.stop(context)
            }
        }
    }

    fun resetOverlayPosition() {
        viewModelScope.launch {
            preferencesManager.updateOverlayPosition(Constants.OVERLAY_OFFSET_X_DP, Constants.OVERLAY_OFFSET_Y_DP)
        }
    }

    fun selectTheme(theme: String) {
        viewModelScope.launch {
            preferencesManager.setCurrentTheme(theme)
        }
    }
}
