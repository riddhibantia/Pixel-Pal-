package com.pixelpal.app.presentation.screens.customize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.SpeciesStyle
import com.pixelpal.app.domain.repository.BondRepository
import com.pixelpal.app.domain.repository.CompanionRepository
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    bondRepository: BondRepository,
    private val spriteAnimator: com.pixelpal.app.animation.SpriteAnimator
) : ViewModel() {

    val companion: StateFlow<Companion?> = companionRepository.getPrimary()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val bondLevel: StateFlow<Int> = companion.flatMapLatest { c ->
        if (c == null) flowOf(0) else bondRepository.getBond(c.id).map { it.level }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val currentTheme: StateFlow<String> = preferencesManager.currentTheme
        .stateIn(viewModelScope, SharingStarted.Lazily, "dark")

    fun transformAppearance(style: SpeciesStyle) {
        viewModelScope.launch {
            companionRepository.transformAppearance(style)
            spriteAnimator.setPetType(style.species)
        }
    }
    fun selectLayer(slot: com.pixelpal.app.domain.model.AvatarSlot, optionId: String) {
        viewModelScope.launch {
            val current = companion.value ?: return@launch
            val known = com.pixelpal.app.domain.model.AvatarOptions.fromId(optionId) ?: return@launch
            if (known.slot != slot) return@launch
            val updated = when (slot) {
                com.pixelpal.app.domain.model.AvatarSlot.EYES -> current.copy(eyeStyle = optionId)
                com.pixelpal.app.domain.model.AvatarSlot.EARS -> current.copy(earStyle = optionId)
                com.pixelpal.app.domain.model.AvatarSlot.HEADWEAR -> current.copy(hatId = optionId)
                com.pixelpal.app.domain.model.AvatarSlot.SCARF -> current.copy(outfitId = optionId)
                com.pixelpal.app.domain.model.AvatarSlot.AURA -> current.copy(accessoryId = optionId)
            }
            companionRepository.update(updated)
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