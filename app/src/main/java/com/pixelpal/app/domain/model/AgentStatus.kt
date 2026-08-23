package com.pixelpal.app.domain.model

data class AgentStatus(
    val companionId: Long,
    val state: AgentState = AgentState.IDLE,
    val message: String? = null,
    val lastCheckedAt: Long? = null,
    val lastSuccessfulCheckAt: Long? = null,
    val consecutiveFailureCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)