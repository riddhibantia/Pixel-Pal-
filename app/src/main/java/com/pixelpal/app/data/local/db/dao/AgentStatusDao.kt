package com.pixelpal.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixelpal.app.data.local.db.entity.AgentStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentStatusDao {
    @Query("SELECT * FROM agent_status WHERE companionId = :companionId")
    fun getStatus(companionId: Long): Flow<AgentStatusEntity?>

    @Query("SELECT * FROM agent_status WHERE companionId = :companionId")
    suspend fun getStatusDirect(companionId: Long): AgentStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(status: AgentStatusEntity)
}