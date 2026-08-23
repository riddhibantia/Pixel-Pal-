package com.pixelpal.app.data.remote

import com.pixelpal.app.domain.model.AgentCheckResult
import com.pixelpal.app.domain.model.AgentConfig

/**
 * Provider-independent contract for checking an AI agent's status.
 * Implementations are responsible for mapping external responses into the
 * stable domain [AgentCheckResult] model.
 */
interface AgentConnector {
    suspend fun checkStatus(config: AgentConfig): AgentCheckResult
}