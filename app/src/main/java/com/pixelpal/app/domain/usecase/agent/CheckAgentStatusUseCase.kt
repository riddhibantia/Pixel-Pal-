package com.pixelpal.app.domain.usecase.agent

import com.pixelpal.app.domain.model.AgentCheckResult
import com.pixelpal.app.domain.repository.AgentConnectionRepository
import javax.inject.Inject

/** Manual "Check now" for the companion's agent connection. */
class CheckAgentStatusUseCase @Inject constructor(
    private val agentConnectionRepository: AgentConnectionRepository
) {
    suspend operator fun invoke(companionId: Long): AgentCheckResult =
        agentConnectionRepository.checkNow(companionId)
}