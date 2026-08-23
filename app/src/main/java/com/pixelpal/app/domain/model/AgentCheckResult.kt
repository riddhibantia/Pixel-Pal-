package com.pixelpal.app.domain.model

/**
 * Stable domain-level result of an agent connectivity check. External API
 * responses are mapped into this model by the connector, never surfaced raw.
 */
data class AgentCheckResult(
    val state: AgentState,
    val message: String? = null,
    val checkedAt: Long = System.currentTimeMillis()
)