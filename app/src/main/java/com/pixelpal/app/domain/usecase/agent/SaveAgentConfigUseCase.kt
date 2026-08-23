package com.pixelpal.app.domain.usecase.agent

import com.pixelpal.app.domain.model.AgentConfig
import com.pixelpal.app.domain.repository.AgentConfigRepository
import com.pixelpal.app.worker.WorkerScheduler
import javax.inject.Inject

class SaveAgentConfigUseCase @Inject constructor(
    private val agentConfigRepository: AgentConfigRepository,
    private val workerScheduler: WorkerScheduler
) {
    suspend operator fun invoke(config: AgentConfig) {
        val saved = config.copy(updatedAt = System.currentTimeMillis())
        agentConfigRepository.saveConfig(saved)
        if (saved.enabled && saved.endpointUrl.isNotBlank()) {
            workerScheduler.scheduleAgentPolling(saved.companionId, saved.pollIntervalMinutes)
        } else {
            workerScheduler.cancelAgentPolling(saved.companionId)
        }
    }
}