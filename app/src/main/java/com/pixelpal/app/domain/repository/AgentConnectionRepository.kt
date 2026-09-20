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

    /** POSTs a command to the agent (or chats with Gemini when provider is gemini). */
    suspend fun sendCommand(companionId: Long, command: String): Result<Unit>

    /**
     * Answers an agent approval request: POSTs
     * `{approvalId, decision: "approve"|"deny"}` to the command endpoint.
     */
    suspend fun respondToApproval(companionId: Long, approvalId: String, approved: Boolean): Result<Unit>

    /**
     * Polls the configured provider, persists the merged result, records
     * meaningful activity and returns what was observed.
     */
    suspend fun checkNow(companionId: Long): AgentCheckResult

    /**
     * Live WebSocket feed for ws:// providers. Empty flow for other providers.
     * Caller collects while the workspace is visible.
     */
    fun streamAgentUpdates(companionId: Long): Flow<AgentCheckResult>

    /**
     * One-shot Gemini reply in the companion's voice. Null when Gemini is
     * unconfigured/failed — caller falls back to local dialogue.
     */
    suspend fun chatWithGemini(companionId: Long, prompt: String): String?
}