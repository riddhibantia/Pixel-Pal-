package com.pixelpal.app.domain.model

/**
 * Run state of the connected external agent (OpenCode etc.). Stored in
 * [AgentConnection.currentStatus]; legacy values map in [fromId].
 */
enum class AgentState(
    val id: String,
    val displayName: String,
    val needsAttention: Boolean
) {
    DISCONNECTED("DISCONNECTED", "Disconnected", false),
    CONNECTING("CONNECTING", "Connecting", false),
    ONLINE("ONLINE", "Online", false),
    IDLE("IDLE", "Idle", false),
    WORKING("WORKING", "Working", false),
    WAITING_FOR_INPUT("WAITING_FOR_INPUT", "Waiting for input", true),
    COMPLETED("COMPLETED", "Completed", false),
    ERROR("ERROR", "Error", true),
    OFFLINE("OFFLINE", "Offline", true);

    companion object {
        fun fromId(id: String): AgentState {
            if (id.isBlank()) return DISCONNECTED
            if (id.equals("FAILED", ignoreCase = true)) return ERROR
            if (id.equals("STOPPED", ignoreCase = true)) return IDLE
            // Unknown non-blank values are config/server bugs — surface as ERROR
            // (needsAttention) instead of silently looking "not connected".
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: ERROR
        }
    }
}

/** Health of the connection itself, separate from what the agent is doing. */
enum class ConnectionStatus { DISCONNECTED, CONNECTED, ERROR;

    companion object {
        fun fromId(id: String): ConnectionStatus =
            entries.find { it.name.equals(id, ignoreCase = true) } ?: DISCONNECTED
    }
}

/**
 * Supported agent providers. Stored in [AgentConnection.provider] (lowercase).
 * - generic: user HTTP(S) status endpoint polled as JSON
 * - gemini: direct Google Gemini AI (no endpoint needed)
 * - websocket: live ws:// / wss:// feed (polled via one-shot, streamed live)
 */
object AgentProviders {
    const val GENERIC = "generic"
    const val GEMINI = "gemini"
    const val WEBSOCKET = "websocket"

    val ALL = listOf(GENERIC, GEMINI, WEBSOCKET)

    fun normalize(raw: String): String = when (raw.trim().lowercase()) {
        GEMINI -> GEMINI
        WEBSOCKET, "ws", "socket" -> WEBSOCKET
        else -> GENERIC
    }

    fun displayName(provider: String): String = when (normalize(provider)) {
        GEMINI -> "Gemini AI"
        WEBSOCKET -> "WebSocket"
        else -> "Generic HTTP"
    }
}
/**
 * The AI Agent INTEGRATION of the single companion. One row; created lazily.
 */
data class AgentConnection(
    val companionId: Long,
    val agentName: String = "",
    val provider: String = "",
    val endpointUrl: String = "",
    val commandUrl: String? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val pollingEnabled: Boolean = false,
    val pollingIntervalMinutes: Long = 15,
    val currentStatus: AgentState = AgentState.DISCONNECTED,
    val currentTask: String? = null,
    val progress: Int? = null,
    val lastMessage: String? = null,
    val errorMessage: String? = null,
    val lastCheckedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isConnected: Boolean get() = connectionStatus == ConnectionStatus.CONNECTED

    /** Normalized provider id — always one of [AgentProviders.ALL]. */
    val normalizedProvider: String get() = AgentProviders.normalize(provider)

    val isGemini: Boolean get() = normalizedProvider == AgentProviders.GEMINI
    val isWebSocket: Boolean get() = normalizedProvider == AgentProviders.WEBSOCKET

    /** Gemini needs no endpoint; others require a status/WebSocket URL. */
    val requiresEndpoint: Boolean get() = !isGemini
}