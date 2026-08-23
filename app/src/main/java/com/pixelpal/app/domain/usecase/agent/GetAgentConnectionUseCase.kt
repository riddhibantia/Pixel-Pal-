package com.pixelpal.app.domain.usecase.agent

import com.pixelpal.app.domain.model.AgentConnection
import com.pixelpal.app.domain.repository.AgentConnectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAgentConnectionUseCase @Inject constructor(
    private val agentConnectionRepository: AgentConnectionRepository
) {
    fun getConnection(companionId: Long): Flow<AgentConnection?> =
        agentConnectionRepository.getConnection(companionId)

    suspend fun getConnectionDirect(companionId: Long): AgentConnection? =
        agentConnectionRepository.getConnectionDirect(companionId)
}