package com.pixelpal.app.data.repository

import com.pixelpal.app.data.local.db.dao.AgentConfigDao
import com.pixelpal.app.data.local.db.entity.AgentConfigEntity
import com.pixelpal.app.domain.model.AgentConfig
import com.pixelpal.app.domain.repository.AgentConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentConfigRepositoryImpl @Inject constructor(
    private val dao: AgentConfigDao
) : AgentConfigRepository {

    override fun getConfig(companionId: Long): Flow<AgentConfig?> {
        return dao.getConfig(companionId).map { it?.toDomain() }
    }

    override suspend fun getConfigDirect(companionId: Long): AgentConfig? {
        return dao.getConfigDirect(companionId)?.toDomain()
    }

    override suspend fun getEnabledDirect(): List<AgentConfig> {
        return dao.getEnabledDirect().map { it.toDomain() }
    }

    override suspend fun saveConfig(config: AgentConfig) {
        dao.upsert(config.toEntity())
    }

    private fun AgentConfigEntity.toDomain() = AgentConfig(
        companionId = companionId,
        endpointUrl = endpointUrl,
        enabled = enabled,
        pollIntervalMinutes = pollIntervalMinutes,
        updatedAt = updatedAt
    )

    private fun AgentConfig.toEntity() = AgentConfigEntity(
        companionId = companionId,
        endpointUrl = endpointUrl,
        enabled = enabled,
        pollIntervalMinutes = pollIntervalMinutes,
        updatedAt = updatedAt
    )
}