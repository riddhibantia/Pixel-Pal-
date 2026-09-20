package com.pixelpal.app.data.repository

import com.pixelpal.app.data.local.db.dao.AgentConnectionDao
import com.pixelpal.app.data.local.db.entity.AgentConnectionEntity
import com.pixelpal.app.data.remote.AgentConnector
import com.pixelpal.app.data.remote.GeminiAgentConnector
import com.pixelpal.app.data.remote.GenericHttpAgentConnector
import com.pixelpal.app.data.remote.WebSocketAgentConnector
import com.pixelpal.app.domain.model.ActivityType
import com.pixelpal.app.domain.model.AgentCheckResult
import com.pixelpal.app.domain.model.AgentConnection
import com.pixelpal.app.domain.model.AgentProviders
import com.pixelpal.app.domain.model.AgentState
import com.pixelpal.app.domain.model.ConnectionStatus
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.repository.AgentConnectionRepository
import com.pixelpal.app.util.AgentNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class AgentConnectionRepositoryImpl @Inject constructor(
    private val dao: AgentConnectionDao,
    private val agentConnector: AgentConnector,
    private val geminiConnector: GeminiAgentConnector,
    private val wsConnector: WebSocketAgentConnector,
    private val client: OkHttpClient,
    private val activityEventRepository: ActivityEventRepository,
    private val agentNotificationHelper: AgentNotificationHelper
) : AgentConnectionRepository {

    override fun getConnection(companionId: Long): Flow<AgentConnection?> =
        dao.getConnection(companionId).map {
            // A default (disconnected) connection instead of null, so the UI's
            // Connect/Save flow always has a companionId to write against.
            it?.toDomain() ?: AgentConnection(companionId = companionId)
        }

    override suspend fun getConnectionDirect(companionId: Long): AgentConnection? =
        dao.getConnectionDirect(companionId)?.toDomain()

    override suspend fun getPollingEnabledDirect(): List<AgentConnection> =
        dao.getPollingEnabledDirect().map { it.toDomain() }

    override suspend fun save(connection: AgentConnection) {
        // Normalize provider so legacy "" / mixed-case values can't fork routing.
        dao.upsert(connection.copy(provider = AgentProviders.normalize(connection.provider)).toEntity())
        // Leaving a live socket behind would leak it — only one provider is active.
        if (connection.normalizedProvider != AgentProviders.WEBSOCKET) {
            wsConnector.disconnect()
        }
    }

    /**
     * Two-way agent communication.
     * - gemini provider: chats with Gemini directly, records reply preview.
     * - websocket provider with ws:// command URL: sends over the live socket.
     * - otherwise: POSTs {"command": ...} to the command endpoint (falls back
     *   to the status endpoint when no dedicated command URL is set).
     */
    override suspend fun sendCommand(companionId: Long, command: String): Result<Unit> {
        if (command.isBlank()) return Result.failure(IllegalArgumentException("Empty command"))
        return withContext(Dispatchers.IO) {
            val current = dao.getConnectionDirect(companionId)?.toDomain()
                ?: return@withContext Result.failure(IllegalStateException("No agent configured"))

            if (current.isGemini) {
                val reply = geminiConnector.generateReply(
                    prompt = command.trim(),
                    companionName = current.agentName.takeIf { it.isNotBlank() } ?: "PixelPal"
                ) ?: return@withContext Result.failure(
                    IllegalStateException("Gemini not configured or request failed — check your API key")
                )
                activityEventRepository.record(
                    companionId,
                    ActivityType.AGENT_COMMAND_SENT,
                    "You: \"${command.trim().take(60)}\" — Agent: \"${reply.take(100)}\""
                )
                dao.upsert(
                    dao.getConnectionDirect(companionId)?.copy(
                        lastMessage = reply.take(200),
                        updatedAt = System.currentTimeMillis()
                    ) ?: AgentConnectionEntity(companionId = companionId, lastMessage = reply.take(200))
                )
                return@withContext Result.success(Unit)
            }

            val url = current.commandUrl?.takeIf { it.isNotBlank() }
                ?: current.endpointUrl.takeIf { it.isNotBlank() }
            if (url == null) {
                return@withContext Result.failure(IllegalStateException("No agent endpoint configured"))
            }

            // Live-socket send when the command target itself is ws://.
            if (GenericHttpAgentConnector.isWebSocketUrl(url)) {
                val sent = try {
                    wsConnector.sendMessage(JSONObject().put("command", command.trim()).toString())
                } catch (e: Exception) {
                    Timber.d(e, "WebSocket send failed")
                    false
                }
                return@withContext if (sent) {
                    activityEventRepository.record(
                        companionId,
                        ActivityType.AGENT_COMMAND_SENT,
                        "Command sent: \"${command.trim().take(40)}\""
                    )
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("WebSocket not connected — press Check Now first"))
                }
            }

            if (!GenericHttpAgentConnector.isAllowedEndpoint(url)) {
                return@withContext Result.failure(
                    IllegalStateException("HTTPS required (cleartext only for localhost)")
                )
            }
            try {
                val body = JSONObject().put("command", command.trim()).toString()
                    .toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).post(body).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        activityEventRepository.record(
                            companionId,
                            ActivityType.AGENT_COMMAND_SENT,
                            "Command sent: \"${command.trim().take(40)}\""
                        )
                        Result.success(Unit)
                    } else {
                        Result.failure(IllegalStateException("HTTP ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * poll → persist → record meaningful activity → notify on attention-worthy
     * changes. Both the periodic worker and manual "Check now" funnel here.
     * Routes by provider: gemini needs no endpoint, ws:// uses a one-shot
     * socket check, everything else uses the generic HTTP poll.
     */
    override suspend fun checkNow(companionId: Long): AgentCheckResult {
        val current = dao.getConnectionDirect(companionId)
            ?: AgentConnectionEntity(companionId = companionId)

        val routing = current.toDomain()
        val endpoint = current.endpointUrl.trim()
        val result = when {
            routing.isGemini -> geminiConnector.checkNow("")
            endpoint.isBlank() -> AgentCheckResult(AgentState.DISCONNECTED, "No endpoint configured")
            routing.isWebSocket || GenericHttpAgentConnector.isWebSocketUrl(endpoint) ->
                wsConnector.checkOnce(endpoint)
            else -> agentConnector.checkNow(endpoint)
        }

        val missingConfig = if (routing.isGemini) {
            result.state == AgentState.DISCONNECTED
        } else {
            endpoint.isBlank()
        }
        val connectionProblem =
            result.state == AgentState.OFFLINE || result.state == AgentState.ERROR

        val updated = current.copy(
            connectionStatus = when {
                missingConfig -> ConnectionStatus.DISCONNECTED.name
                connectionProblem -> ConnectionStatus.ERROR.name
                else -> ConnectionStatus.CONNECTED.name
            },
            currentStatus = result.state.id,
            lastMessage = result.message,
            errorMessage = if (connectionProblem) result.message else null,
            currentTask = result.currentTask ?: if (result.state == AgentState.DISCONNECTED) null else current.currentTask,
            progress = result.progress ?: if (result.state == AgentState.DISCONNECTED) null else current.progress,
            lastCheckedAt = result.checkedAt,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsert(updated)

        // Meaningful events only: state transitions and task/progress changes.
        val wasRunState = AgentState.fromId(current.currentStatus)
        val runStateChanged = wasRunState != result.state
        val taskChanged = result.currentTask != null && result.currentTask != previousTaskOf(current)
        if (runStateChanged) {
            activityEventRepository.record(
                companionId = companionId,
                type = ActivityType.AGENT_STATUS_CHANGED,
                title = "AI Agent is now ${result.state.displayName}",
                description = result.message ?: result.currentTask
            )
        } else if (taskChanged) {
            activityEventRepository.record(
                companionId = companionId,
                type = ActivityType.AGENT_STATUS_CHANGED,
                title = "Agent started \"${result.currentTask}\""
            )
        }

        if (runStateChanged && result.state.needsAttention) {
            agentNotificationHelper.notify(companionId, "AI Agent", result)
        }

        return result
    }

    override fun streamAgentUpdates(companionId: Long): Flow<AgentCheckResult> =
        dao.getConnection(companionId).flatMapLatest { entity ->
            val domain = entity?.toDomain()
            val endpoint = entity?.endpointUrl?.trim().orEmpty()
            if (domain == null || endpoint.isBlank() ||
                (!domain.isWebSocket && !GenericHttpAgentConnector.isWebSocketUrl(endpoint))
            ) {
                emptyFlow()
            } else {
                wsConnector.connectAndStream(endpoint)
            }
        }

    override suspend fun chatWithGemini(companionId: Long, prompt: String): String? {
        if (prompt.isBlank()) return null
        val current = dao.getConnectionDirect(companionId)?.toDomain() ?: return null
        return geminiConnector.generateReply(
            prompt = prompt.trim(),
            companionName = current.agentName.takeIf { it.isNotBlank() } ?: "PixelPal"
        )
    }

    private fun previousTaskOf(entity: AgentConnectionEntity): String? = entity.currentTask

    private fun AgentConnectionEntity.toDomain() = AgentConnection(
        companionId = companionId,
        agentName = agentName,
        provider = provider,
        endpointUrl = endpointUrl,
        commandUrl = commandUrl,
        connectionStatus = ConnectionStatus.fromId(connectionStatus),
        pollingEnabled = pollingEnabled,
        pollingIntervalMinutes = pollingIntervalMinutes,
        currentStatus = AgentState.fromId(currentStatus),
        currentTask = currentTask,
        progress = progress,
        lastMessage = lastMessage,
        errorMessage = errorMessage,
        lastCheckedAt = lastCheckedAt,
        updatedAt = updatedAt
    )

    private fun AgentConnection.toEntity() = AgentConnectionEntity(
        companionId = companionId,
        agentName = agentName,
        provider = provider,
        endpointUrl = endpointUrl,
        commandUrl = commandUrl,
        connectionStatus = connectionStatus.name,
        pollingEnabled = pollingEnabled,
        pollingIntervalMinutes = pollingIntervalMinutes,
        currentStatus = currentStatus.id,
        currentTask = currentTask,
        progress = progress,
        lastMessage = lastMessage,
        errorMessage = errorMessage,
        lastCheckedAt = lastCheckedAt,
        updatedAt = System.currentTimeMillis()
    )
}
