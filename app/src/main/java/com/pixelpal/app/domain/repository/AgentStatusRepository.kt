package com.pixelpal.app.domain.repository

import com.pixelpal.app.domain.model.AgentStatus
import kotlinx.coroutines.flow.Flow

interface AgentStatusRepository {
    fun getStatus(companionId: Long): Flow<AgentStatus?>
    suspend fun getStatusDirect(companionId: Long): AgentStatus?
    suspend fun updateStatus(status: AgentStatus)
}