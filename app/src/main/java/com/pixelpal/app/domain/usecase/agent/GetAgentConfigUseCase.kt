package com.pixelpal.app.domain.usecase.agent

import com.pixelpal.app.domain.model.AgentConfig
import com.pixelpal.app.domain.repository.AgentConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAgentConfigUseCase @Inject constructor(
    private val agentConfigRepository: AgentConfigRepository
) {
    fun getConfig(companionId: Long): Flow<AgentConfig?> = agentConfigRepository.getConfig(companionId)
}