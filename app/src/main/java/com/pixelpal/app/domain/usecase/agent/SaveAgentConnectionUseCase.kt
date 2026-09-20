package com.pixelpal.app.domain.usecase.agent

import com.pixelpal.app.domain.model.AgentConnection
import com.pixelpal.app.domain.repository.AgentConnectionRepository
import com.pixelpal.app.worker.WorkerScheduler
import javax.inject.Inject

/** Saves the agent connection config and syncs its polling schedule. */
class SaveAgentConnectionUseCase @Inject constructor(
    private val agentConnectionRepository: AgentConnectionRepository,
    private val workerScheduler: WorkerScheduler
) {
    suspend operator fun invoke(connection: AgentConnection) {
        val normalized = connection.copy(
            provider = com.pixelpal.app.domain.model.AgentProviders.normalize(connection.provider),
            pollingIntervalMinutes = connection.pollingIntervalMinutes.coerceAtLeast(
                com.pixelpal.app.util.Constants.DEFAULT_AGENT_POLL_INTERVAL_MIN
            ),
            updatedAt = System.currentTimeMillis()
        )
        agentConnectionRepository.save(normalized)
        // Gemini needs no endpoint — polling is valid with pollingEnabled alone.
        val pollable = normalized.pollingEnabled &&
            (normalized.endpointUrl.isNotBlank() || normalized.isGemini)
        if (pollable) {
            workerScheduler.scheduleAgentPolling(normalized.companionId, normalized.pollingIntervalMinutes)
        } else {
            workerScheduler.cancelAgentPolling(normalized.companionId)
        }
    }
}