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
        val saved = connection.copy(updatedAt = System.currentTimeMillis())
        agentConnectionRepository.save(saved)
        if (saved.pollingEnabled && saved.endpointUrl.isNotBlank()) {
            workerScheduler.scheduleAgentPolling(saved.companionId, saved.pollingIntervalMinutes)
        } else {
            workerScheduler.cancelAgentPolling(saved.companionId)
        }
    }
}