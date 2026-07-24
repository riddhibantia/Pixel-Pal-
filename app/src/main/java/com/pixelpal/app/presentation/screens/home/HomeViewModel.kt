package com.pixelpal.app.presentation.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.animation.AnimationEngine
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.overlay.OverlayService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val animationEngine: AnimationEngine
) : ViewModel() {

    val petName: StateFlow<String> = preferencesManager.petName
        .stateIn(viewModelScope, SharingStarted.Lazily, "Pixel")

    val selectedPetType: StateFlow<String> = preferencesManager.selectedPetType
        .stateIn(viewModelScope, SharingStarted.Lazily, "cat")

    val overlayEnabled: StateFlow<Boolean> = preferencesManager.overlayEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val currentAnimation: StateFlow<AnimationState> = animationEngine.currentState

    fun toggleOverlay(context: Context) {
        viewModelScope.launch {
            val current = overlayEnabled.value
            val next = !current
            preferencesManager.setOverlayEnabled(next)
            if (next) {
                OverlayService.start(context)
            } else {
                OverlayService.stop(context)
            }
        }
    }

    fun tapPet() {
        animationEngine.trigger(AnimationState.HAPPY)
    }
}
