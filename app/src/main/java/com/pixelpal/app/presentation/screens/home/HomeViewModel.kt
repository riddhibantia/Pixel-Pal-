package com.pixelpal.app.presentation.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.animation.AnimationEngine
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.domain.engine.CompanionEngine
import com.pixelpal.app.domain.model.Bond
import com.pixelpal.app.domain.model.Emotion
import com.pixelpal.app.domain.usecase.companion.GetCompanionStateUseCase
import android.provider.Settings
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
    private val animationEngine: AnimationEngine,
    private val companionEngine: CompanionEngine,
    getCompanionStateUseCase: GetCompanionStateUseCase
) : ViewModel() {

    val petName: StateFlow<String> = preferencesManager.petName
        .stateIn(viewModelScope, SharingStarted.Lazily, "Pixel")

    val selectedPetType: StateFlow<String> = preferencesManager.selectedPetType
        .stateIn(viewModelScope, SharingStarted.Lazily, "cat")

    val overlayEnabled: StateFlow<Boolean> = preferencesManager.overlayEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val currentAnimation: StateFlow<AnimationState> = animationEngine.currentState

    val currentEmotion: StateFlow<Emotion> = getCompanionStateUseCase.currentEmotion
        .stateIn(viewModelScope, SharingStarted.Lazily, Emotion.CALM)

    val bond: StateFlow<Bond> = getCompanionStateUseCase.bond
        .stateIn(viewModelScope, SharingStarted.Lazily, Bond())

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
        companionEngine.onTap()
    }

    fun feedPet() {
        companionEngine.onFeed()
    }
}
