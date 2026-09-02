package com.pixelpal.app.domain.repository

import com.pixelpal.app.domain.model.AgentCheckResult
import com.pixelpal.app.domain.model.AgentConnection
import kotlinx.coroutines.flow.Flow

/** The AI Agent integration of the single companion. */
interface AgentConnectionRepository {
    fun getConnection(companionId: Long): Flow<AgentConnection?>
    suspend fun getConnectionDirect(companionId: Long): AgentConnection?
    suspend fun getPollingEnabledDirect(): List<AgentConnection>

    /** Saves connection config; caller coordinates polling schedule. */
    suspend fun save(connection: AgentConnection)

    /** POSTs a command to the agent's command endpoint. */
    suspend fun sendCommand(companionId: Long, command: String): Result<Unit>

    /**
     * Polls the configured endpoint, persists the merged result, records
     * meaningful activity and returns what was observed.
     */
    suspend fun checkNow(companionId: Long): AgentCheckResult
}