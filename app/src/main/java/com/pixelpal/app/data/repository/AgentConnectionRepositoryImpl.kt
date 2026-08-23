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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        dao.getConnection(companionId).map { it?.toDomain() }

    override suspend fun getConnectionDirect(companionId: Long): AgentConnection? =
        dao.getConnectionDirect(companionId)?.toDomain()

    override suspend fun getPollingEnabledDirect(): List<AgentConnection> =
        dao.getPollingEnabledDirect().map { it.toDomain() }

    override suspend fun save(connection: AgentConnection) {
        dao.upsert(connection.toEntity())
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