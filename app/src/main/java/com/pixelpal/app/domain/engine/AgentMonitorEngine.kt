package com.pixelpal.app.domain.engine

import com.pixelpal.app.domain.model.ActivityType
import com.pixelpal.app.domain.model.AgentCheckResult
import com.pixelpal.app.domain.model.AgentConfig
import com.pixelpal.app.domain.model.AgentState
import com.pixelpal.app.domain.model.AgentStatus
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.repository.AgentConfigRepository
import com.pixelpal.app.domain.repository.AgentStatusRepository
import com.pixelpal.app.domain.repository.CompanionRepository
import com.pixelpal.app.data.remote.AgentConnector
import com.pixelpal.app.util.AgentNotificationHelper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared core for agent monitoring. Both the periodic WorkManager worker and
 * the manual "Check now" action call [checkNow], so their behavior stays
 * consistent: poll -> persist status -> record activity -> notify on changes
 * that need attention.
 */
@Singleton
class AgentMonitorEngine @Inject constructor(
    private val agentConfigRepository: AgentConfigRepository,
    private val agentStatusRepository: AgentStatusRepository,
    private val activityEventRepository: ActivityEventRepository,
    private val companionRepository: CompanionRepository,
    private val agentConnector: AgentConnector,
    private val agentNotificationHelper: AgentNotificationHelper
) {

    suspend fun checkNow(companionId: Long): AgentCheckResult {
        val config = agentConfigRepository.getConfigDirect(companionId)
            ?: AgentConfig(companionId = companionId)

        val result = agentConnector.checkStatus(config)

        val previous = agentStatusRepository.getStatusDirect(companionId)
        val isFailure = result.state == AgentState.FAILED || result.state == AgentState.OFFLINE
        val status = AgentStatus(
            companionId = companionId,
            state = result.state,
            message = result.message,
            lastCheckedAt = result.checkedAt,
            lastSuccessfulCheckAt = if (isFailure) previous?.lastSuccessfulCheckAt else result.checkedAt,
            consecutiveFailureCount = if (isFailure) (previous?.consecutiveFailureCount ?: 0) + 1 else 0,
            updatedAt = System.currentTimeMillis()
        )
        agentStatusRepository.updateStatus(status)

        activityEventRepository.record(
            companionId = companionId,
            type = ActivityType.AGENT_STATUS_CHANGED,
            title = "Agent status: ${result.state.displayName}",
            description = result.message
        )

        val stateChanged = previous?.state != result.state
        if (stateChanged && result.state.needsAttention) {
            val companionName = companionRepository.getByIdDirect(companionId)?.name ?: "Agent"
            agentNotificationHelper.notify(companionId, companionName, result)
        }

        return result
    }
}