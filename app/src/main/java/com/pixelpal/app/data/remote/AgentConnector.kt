package com.pixelpal.app.data.remote

import com.pixelpal.app.domain.model.AgentCheckResult
import com.pixelpal.app.domain.model.AgentState

/**
 * Provider-independent contract for checking an AI agent's status.
 * Implementations map external responses into the stable [AgentCheckResult].
 */
interface AgentConnector {
    suspend fun checkNow(endpointUrl: String): AgentCheckResult
}