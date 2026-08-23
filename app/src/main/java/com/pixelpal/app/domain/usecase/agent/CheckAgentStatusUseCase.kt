package com.pixelpal.app.domain.usecase.agent

import com.pixelpal.app.domain.engine.AgentMonitorEngine
import com.pixelpal.app.domain.model.AgentCheckResult
import javax.inject.Inject

class CheckAgentStatusUseCase @Inject constructor(
    private val agentMonitorEngine: AgentMonitorEngine
) {
    suspend operator fun invoke(companionId: Long): AgentCheckResult =
        agentMonitorEngine.checkNow(companionId)
}