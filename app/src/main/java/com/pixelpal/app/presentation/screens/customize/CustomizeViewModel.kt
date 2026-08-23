package com.pixelpal.app.presentation.screens.customize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.SpeciesStyle
import com.pixelpal.app.domain.repository.CompanionRepository
import com.pixelpal.app.data.local.datastore.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Companion customization = transformation of THE companion's appearance.
 * Species/color/pattern changes never touch bond/tasks/reminders/agent data.
 */
@HiltViewModel
class CustomizeViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val companionRepository: CompanionRepository,
    private val spriteAnimator: com.pixelpal.app.animation.SpriteAnimator
) : ViewModel() {

    val companion: StateFlow<Companion?> = companionRepository.getPrimary()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val currentTheme: StateFlow<String> = preferencesManager.currentTheme
        .stateIn(viewModelScope, SharingStarted.Lazily, "dark")

    fun transformAppearance(style: SpeciesStyle) {
        viewModelScope.launch {
            companionRepository.transformAppearance(style)
            spriteAnimator.setPetType(style.species)
        }
    }

    fun updatePetName(name: String) {
        viewModelScope.launch {
            val current = companion.value ?: return@launch
            if (name.isNotBlank()) {
                companionRepository.update(current.copy(name = name.take(20)))
            }
        }
    }

    fun selectTheme(theme: String) {
        viewModelScope.launch { preferencesManager.setCurrentTheme(theme) }
    }
}