package com.pixelpal.app.presentation.screens.customize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.animation.SpriteAnimator
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.domain.engine.BondEngine
import com.pixelpal.app.domain.model.Bond
import com.pixelpal.app.domain.model.PetType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomizeViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val spriteAnimator: SpriteAnimator,
    bondEngine: BondEngine
) : ViewModel() {

    val selectedPetType: StateFlow<String> = preferencesManager.selectedPetType
        .stateIn(viewModelScope, SharingStarted.Lazily, "cat")

    val petName: StateFlow<String> = preferencesManager.petName
        .stateIn(viewModelScope, SharingStarted.Lazily, "Pixel")

    val currentTheme: StateFlow<String> = preferencesManager.currentTheme
        .stateIn(viewModelScope, SharingStarted.Lazily, "dark")

    val bond: StateFlow<Bond> = bondEngine.bond
        .stateIn(viewModelScope, SharingStarted.Lazily, Bond())

    fun selectPet(petType: PetType) {
        viewModelScope.launch {
            preferencesManager.setSelectedPetType(petType.id)
            spriteAnimator.setPetType(petType.id)
        }
    }

    fun updatePetName(name: String) {
        viewModelScope.launch {
            preferencesManager.setPetName(name)
        }
    }

    fun selectTheme(theme: String) {
        viewModelScope.launch {
            preferencesManager.setCurrentTheme(theme)
        }
    }
}
