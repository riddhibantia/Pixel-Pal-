package com.pixelpal.app.presentation.screens.companions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.BuildConfig
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.data.remote.GeminiAgentConnector
import com.pixelpal.app.domain.model.AgentConnection
import com.pixelpal.app.domain.model.Bond
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.AgentState
import com.pixelpal.app.domain.repository.AgentConnectionRepository
import com.pixelpal.app.domain.repository.BondRepository
import kotlinx.coroutines.flow.map
import com.pixelpal.app.domain.usecase.agent.GetAgentConnectionUseCase
import com.pixelpal.app.domain.usecase.agent.SaveAgentConnectionUseCase
import com.pixelpal.app.domain.usecase.companion.GetActiveCompanionUseCase
import com.pixelpal.app.domain.usecase.companion.ToggleFavoriteUseCase
import com.pixelpal.app.presentation.components.SnackbarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompanionWorkspaceUiState(
    val companion: Companion? = null,
    val bond: Bond? = null,
    val agentConnection: AgentConnection? = null,
    val loading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CompanionWorkspaceViewModel @Inject constructor(
    getActiveCompanionUseCase: GetActiveCompanionUseCase,
    bondRepository: BondRepository,
    getAgentConnectionUseCase: GetAgentConnectionUseCase,
    private val agentConnectionRepository: AgentConnectionRepository,
    private val saveAgentConnectionUseCase: SaveAgentConnectionUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val updateCompanionUseCase: com.pixelpal.app.domain.usecase.companion.UpdateCompanionUseCase,
    private val preferencesManager: PreferencesManager,
    private val geminiAgentConnector: GeminiAgentConnector
) : ViewModel() {

    private val core = getActiveCompanionUseCase.activeCompanion.flatMapLatest { c ->
        if (c == null) flowOf<Pair<Companion?, Bond?>>(null to null)
        else bondRepository.getBond(c.id).map { bond -> c to bond }
    }

    private val extras = getActiveCompanionUseCase.activeCompanion.flatMapLatest { c ->
        if (c == null) flowOf(null) else getAgentConnectionUseCase.getConnection(c.id)
    }

    val uiState: StateFlow<CompanionWorkspaceUiState> = combine(
        core, extras
    ) { corePair, agent ->
        val (companion, bond) = corePair
        CompanionWorkspaceUiState(
            companion = companion,
            bond = bond,
            agentConnection = agent,
            loading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompanionWorkspaceUiState())

    private val _checkingAgent = MutableStateFlow(false)
    val checkingAgent: StateFlow<Boolean> = _checkingAgent.asStateFlow()

    private val _commandFeedback = MutableStateFlow<String?>(null)
    val commandFeedback: StateFlow<String?> = _commandFeedback.asStateFlow()

    /** Raw override text from DataStore; blank = using BuildConfig key. */
    val geminiKeyOverride: StateFlow<String> = preferencesManager.geminiApiKeyOverride
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _snackbarEvents = MutableSharedFlow<SnackbarEvent>()
    val snackbarEvents: SharedFlow<SnackbarEvent> = _snackbarEvents.asSharedFlow()

    fun sendAgentCommand(command: String) {
        val companionId = uiState.value.companion?.id ?: return
        if (command.isBlank()) return
        viewModelScope.launch {
            val wasGemini = uiState.value.agentConnection?.isGemini == true
            val result = agentConnectionRepository.sendCommand(companionId, command.trim())
            _commandFeedback.value = if (result.isSuccess && wasGemini) {
                // Surface Gemini's actual reply instead of a generic "Sent".
                val updated = agentConnectionRepository.getConnectionDirect(companionId)
                updated?.lastMessage?.takeIf { it.isNotBlank() }?.let { "Agent: $it" }
                    ?: "Sent \"${command.trim()}\""
            } else result.fold(
                onSuccess = { "Sent \"${command.trim()}\"" },
                onFailure = { "Couldn't send: ${it.message ?: "unknown error"}" }
            )
            // The agent updated its own status in response — surface it now.
            if (result.isSuccess) {
                refreshAgentStatus()
            }
        }
    }

    fun reportVoiceUnavailable() {
        _commandFeedback.value = "Voice input isn't available on this device — type instead."
    }

    fun clearCommandFeedback() {
        _commandFeedback.value = null
    }

    fun refreshAgentStatus() {
        val companionId = uiState.value.companion?.id ?: return
        if (_checkingAgent.value) return
        viewModelScope.launch {
            _checkingAgent.value = true
            try {
                agentConnectionRepository.checkNow(companionId)
            } finally {
                _checkingAgent.value = false
            }
        }
    }

    fun saveAgentConnection(connection: AgentConnection) {
        viewModelScope.launch {
            saveAgentConnectionUseCase(connection)
            // Connect should feel like connecting — run the first check now.
            // Gemini needs no endpoint; everything else needs its URL.
            if (connection.endpointUrl.isNotBlank() || connection.isGemini) {
                agentConnectionRepository.checkNow(connection.companionId)
            }
        }
    }

    /** Persists a user-supplied Gemini key and activates it immediately. */
    fun saveGeminiKey(key: String) {
        viewModelScope.launch {
            val trimmed = key.trim()
            preferencesManager.setGeminiApiKeyOverride(trimmed)
            when {
                GeminiAgentConnector.isUsableKey(trimmed) -> geminiAgentConnector.initialize(trimmed)
                trimmed.isBlank() -> {
                    // Fall back to the bundled BuildConfig key, if any.
                    val bundled = BuildConfig.GEMINI_API_KEY
                    if (GeminiAgentConnector.isUsableKey(bundled)) geminiAgentConnector.initialize(bundled)
                    else geminiAgentConnector.clear()
                }
                else -> geminiAgentConnector.clear()
            }
        }
    }

    fun disconnectAgent() {
        val connection = uiState.value.agentConnection ?: return
        viewModelScope.launch {
            saveAgentConnectionUseCase(
                connection.copy(
                    endpointUrl = "",
                    commandUrl = null,
                    pollingEnabled = false,
                    connectionStatus = com.pixelpal.app.domain.model.ConnectionStatus.DISCONNECTED,
                    currentStatus = AgentState.DISCONNECTED,
                    currentTask = null,
                    progress = null,
                    lastMessage = null,
                    errorMessage = null
                )
            )
            _snackbarEvents.emit(SnackbarEvent.AgentDisconnected(connection))
        }
    }

    fun undoDisconnect(connection: AgentConnection) {
        viewModelScope.launch { saveAgentConnectionUseCase(connection) }
    }

    fun toggleFavorite() {
        val current = uiState.value.companion ?: return
        viewModelScope.launch { toggleFavoriteUseCase(current.id, !current.isFavorite) }
    }

    fun rename(name: String) {
        val current = uiState.value.companion ?: return
        if (name.isBlank()) return
        viewModelScope.launch { updateCompanionUseCase(current.copy(name = name.take(20))) }
    }

}
