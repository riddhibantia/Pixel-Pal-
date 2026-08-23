package com.pixelpal.app.domain.model

/**
 * Runtime operational state for an AI-agent companion. Belongs to the agent
 * domain only — general companion lifecycle uses [Companion.isArchived].
 */
enum class AgentState(
    val id: String,
    val displayName: String,
    val needsAttention: Boolean
) {
    IDLE("IDLE", "Idle", false),
    CONNECTING("CONNECTING", "Connecting", false),
    WORKING("WORKING", "Working", false),
    WAITING_FOR_INPUT("WAITING_FOR_INPUT", "Waiting for input", true),
    COMPLETED("COMPLETED", "Completed", false),
    FAILED("FAILED", "Failed", true),
    STOPPED("STOPPED", "Stopped", false),
    OFFLINE("OFFLINE", "Offline", true);

    companion object {
        fun fromId(id: String): AgentState =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: OFFLINE
    }
}