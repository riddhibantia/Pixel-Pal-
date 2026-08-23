package com.pixelpal.app.domain.repository

import com.pixelpal.app.domain.model.AgentConfig
import kotlinx.coroutines.flow.Flow

interface AgentConfigRepository {
    fun getConfig(companionId: Long): Flow<AgentConfig?>
    suspend fun getConfigDirect(companionId: Long): AgentConfig?
    suspend fun getEnabledDirect(): List<AgentConfig>
    suspend fun saveConfig(config: AgentConfig)
}