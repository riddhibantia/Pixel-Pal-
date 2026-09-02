package com.pixelpal.app.data.repository

import com.pixelpal.app.data.local.db.dao.AgentConnectionDao
import com.pixelpal.app.data.local.db.entity.AgentConnectionEntity
import com.pixelpal.app.data.remote.AgentConnector
import com.pixelpal.app.domain.model.ActivityType
import com.pixelpal.app.domain.model.AgentCheckResult
import com.pixelpal.app.domain.model.AgentConnection
import com.pixelpal.app.domain.model.AgentState
import com.pixelpal.app.domain.model.ConnectionStatus
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.repository.AgentConnectionRepository
import com.pixelpal.app.util.AgentNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentConnectionRepositoryImpl @Inject constructor(
    private val dao: AgentConnectionDao,
    private val agentConnector: AgentConnector,
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
        dao.upsert(connection.toEntity())
    }

    /**
     * Two-way agent communication: POSTs {"command": ...} to the command
     * endpoint (falls back to the status endpoint). Success is recorded as an
     * activity event so it shows up in the home/bell feed.
     */
    override suspend fun sendCommand(companionId: Long, command: String): Result<Unit> {
        if (command.isBlank()) return Result.failure(IllegalArgumentException("Empty command"))
        return withContext(Dispatchers.IO) {
            val current = dao.getConnectionDirect(companionId)
            val url = current?.commandUrl?.takeIf { it.isNotBlank() }
                ?: current?.endpointUrl?.takeIf { it.isNotBlank() }
            if (url == null) {
                return@withContext Result.failure(IllegalStateException("No agent endpoint configured"))
            }
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()
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
     */
    override suspend fun checkNow(companionId: Long): AgentCheckResult {
        val current = dao.getConnectionDirect(companionId)
            ?: AgentConnectionEntity(companionId = companionId)

        val wasRunState = AgentState.fromId(current.currentStatus)
        val result = if (current.endpointUrl.isBlank()) {
            AgentCheckResult(AgentState.DISCONNECTED, "No endpoint configured")
        } else {
            agentConnector.checkNow(current.endpointUrl)
        }

        val connectionProblem =
            result.state == AgentState.OFFLINE || result.state == AgentState.ERROR

        val updated = current.copy(
            connectionStatus = when {
                current.endpointUrl.isBlank() -> ConnectionStatus.DISCONNECTED.name
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