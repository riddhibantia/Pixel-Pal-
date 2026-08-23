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
        fun fromId(id: String): AgentState = when {
            id.equals("FAILED", ignoreCase = true) -> ERROR
            id.equals("STOPPED", ignoreCase = true) -> IDLE
            else -> entries.find { it.id.equals(id, ignoreCase = true) } ?: DISCONNECTED
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
 * The AI Agent INTEGRATION of the single companion. One row; created lazily.
 */
data class AgentConnection(
    val companionId: Long,
    val agentName: String = "",
    val provider: String = "",
    val endpointUrl: String = "",
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
}