package com.pixelpal.app.domain.model

/**
 * Stable domain-level result of an agent connectivity check. External API
 * responses are mapped into this model by the connector, never surfaced raw.
 */
data class AgentCheckResult(
    val state: AgentState,
    val message: String? = null,
    /** Structured update fields (OpenCode-style payloads); null when absent. */
    val currentTask: String? = null,
    val progress: Int? = null,
    val checkedAt: Long = System.currentTimeMillis()
)