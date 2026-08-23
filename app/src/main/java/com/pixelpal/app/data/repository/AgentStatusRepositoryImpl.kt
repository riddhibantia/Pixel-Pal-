package com.pixelpal.app.data.repository

import com.pixelpal.app.data.local.db.dao.AgentStatusDao
import com.pixelpal.app.data.local.db.entity.AgentStatusEntity
import com.pixelpal.app.domain.model.AgentState
import com.pixelpal.app.domain.model.AgentStatus
import com.pixelpal.app.domain.repository.AgentStatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentStatusRepositoryImpl @Inject constructor(
    private val dao: AgentStatusDao
) : AgentStatusRepository {

    override fun getStatus(companionId: Long): Flow<AgentStatus?> {
        return dao.getStatus(companionId).map { it?.toDomain() }
    }

    override suspend fun getStatusDirect(companionId: Long): AgentStatus? {
        return dao.getStatusDirect(companionId)?.toDomain()
    }

    override suspend fun updateStatus(status: AgentStatus) {
        dao.upsert(status.toEntity())
    }

    private fun AgentStatusEntity.toDomain() = AgentStatus(
        companionId = companionId,
        state = AgentState.fromId(state),
        message = message,
        lastCheckedAt = lastCheckedAt,
        lastSuccessfulCheckAt = lastSuccessfulCheckAt,
        consecutiveFailureCount = consecutiveFailureCount,
        updatedAt = updatedAt
    )

    private fun AgentStatus.toEntity() = AgentStatusEntity(
        companionId = companionId,
        state = state.id,
        message = message,
        lastCheckedAt = lastCheckedAt,
        lastSuccessfulCheckAt = lastSuccessfulCheckAt,
        consecutiveFailureCount = consecutiveFailureCount,
        updatedAt = updatedAt
    )
}