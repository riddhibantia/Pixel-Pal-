package com.pixelpal.app.presentation.screens.companions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.CompanionRole
import com.pixelpal.app.domain.repository.CompanionActionResult
import com.pixelpal.app.domain.usecase.companion.CreateCompanionUseCase
import com.pixelpal.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateCompanionUiState(
    val step: Int = 0,
    val role: CompanionRole = CompanionRole.GENERAL,
    val name: String = "",
    val petType: String = "cat",
    val isCreating: Boolean = false,
    val createdCompanion: Companion? = null,
    val error: String? = null
) {
    val isStepValid: Boolean
        get() = when (step) {
            0 -> true
            1 -> name.isNotBlank() || true // falls back to a role-based default name
            else -> true
        }

    val effectiveName: String
        get() = name.ifBlank { defaultNameFor(role) }
}

private fun defaultNameFor(role: CompanionRole): String = when (role) {
    CompanionRole.GENERAL -> "Pixel"
    CompanionRole.REMINDER -> "Remi"
    CompanionRole.TASK -> "Todo"
    CompanionRole.AI_AGENT -> "Agent"
    CompanionRole.CUSTOM -> "Companion"
}

@HiltViewModel
class CreateCompanionViewModel @Inject constructor(
    private val createCompanionUseCase: CreateCompanionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateCompanionUiState())
    val uiState: StateFlow<CreateCompanionUiState> = _uiState.asStateFlow()

    fun selectRole(role: CompanionRole) {
        _uiState.update { it.copy(role = role) }
    }

    fun setName(name: String) {
        _uiState.update { it.copy(name = name.take(20)) }
    }

    fun selectPetType(petType: String) {
        _uiState.update { it.copy(petType = petType) }
    }

    fun nextStep() {
        _uiState.update { it.copy(step = it.step + 1, error = null) }
    }

    fun previousStep() {
        _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }
    }

    fun create() {
        if (_uiState.value.isCreating) return
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isCreating = true, error = null) }
            val result = createCompanionUseCase(
                Companion(
                    name = state.effectiveName,
                    petType = state.petType,
                    role = state.role
                )
            )
            when (result) {
                is CompanionActionResult.Success -> {
                    _uiState.update {
                        it.copy(isCreating = false, createdCompanion = result.companion, step = 3)
                    }
                }
                CompanionActionResult.LimitReached -> {
                    _uiState.update {
                        it.copy(isCreating = false, error = "Active companion limit reached (${Constants.MAX_ACTIVE_COMPANIONS})")
                    }
                }
                is CompanionActionResult.Error -> {
                    _uiState.update {
                        it.copy(isCreating = false, error = result.message ?: "Could not create companion")
                    }
                }
            }
        }
    }
}