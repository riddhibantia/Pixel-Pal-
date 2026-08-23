package com.pixelpal.app.presentation.screens.customize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.animation.SpriteAnimator
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.domain.engine.BondEngine
import com.pixelpal.app.domain.model.Bond
import com.pixelpal.app.domain.model.PetType
import com.pixelpal.app.domain.usecase.companion.GetActiveCompanionUseCase
import com.pixelpal.app.domain.usecase.companion.UpdateCompanionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomizeViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val spriteAnimator: SpriteAnimator,
    getActiveCompanionUseCase: GetActiveCompanionUseCase,
    private val updateCompanionUseCase: UpdateCompanionUseCase,
    bondEngine: BondEngine
) : ViewModel() {

    private val activeCompanion = getActiveCompanionUseCase.activeCompanion
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val selectedPetType: StateFlow<String> = activeCompanion
        .map { it?.petType ?: "cat" }
        .stateIn(viewModelScope, SharingStarted.Lazily, "cat")

    val petName: StateFlow<String> = activeCompanion
        .map { it?.name ?: "Pixel" }
        .stateIn(viewModelScope, SharingStarted.Lazily, "Pixel")

    val currentTheme: StateFlow<String> = preferencesManager.currentTheme
        .stateIn(viewModelScope, SharingStarted.Lazily, "dark")

    val bond: StateFlow<Bond> = bondEngine.bond
        .stateIn(viewModelScope, SharingStarted.Lazily, Bond(companionId = -1))

    fun selectPet(petType: PetType) {
        viewModelScope.launch {
            val companion = activeCompanion.value ?: return@launch
            updateCompanionUseCase(companion.copy(petType = petType.id))
            spriteAnimator.setPetType(petType.id)
        }
    }

    fun updatePetName(name: String) {
        viewModelScope.launch {
            val companion = activeCompanion.value ?: return@launch
            updateCompanionUseCase(companion.copy(name = name))
        }
    }

    fun selectTheme(theme: String) {
        viewModelScope.launch {
            preferencesManager.setCurrentTheme(theme)
        }
    }
}