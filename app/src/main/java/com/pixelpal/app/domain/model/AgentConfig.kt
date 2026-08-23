package com.pixelpal.app.domain.model

/**
 * User-configured connection settings for an AI-agent companion.
 * No secrets or API keys are ever stored here.
 */
data class AgentConfig(
    val companionId: Long,
    val endpointUrl: String = "",
    val enabled: Boolean = false,
    val pollIntervalMinutes: Long = 15,
    val updatedAt: Long = System.currentTimeMillis()
)