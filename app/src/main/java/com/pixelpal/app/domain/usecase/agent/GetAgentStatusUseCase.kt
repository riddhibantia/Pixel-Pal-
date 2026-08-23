package com.pixelpal.app.domain.usecase.agent

import com.pixelpal.app.domain.model.AgentStatus
import com.pixelpal.app.domain.repository.AgentStatusRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAgentStatusUseCase @Inject constructor(
    private val agentStatusRepository: AgentStatusRepository
) {
    fun getStatus(companionId: Long): Flow<AgentStatus?> = agentStatusRepository.getStatus(companionId)
}