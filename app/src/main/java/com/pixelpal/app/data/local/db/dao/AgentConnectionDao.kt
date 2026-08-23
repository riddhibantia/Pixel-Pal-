package com.pixelpal.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixelpal.app.data.local.db.entity.AgentConnectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentConnectionDao {
    @Query("SELECT * FROM agent_connection WHERE companionId = :companionId")
    fun getConnection(companionId: Long): Flow<AgentConnectionEntity?>

    @Query("SELECT * FROM agent_connection WHERE companionId = :companionId")
    suspend fun getConnectionDirect(companionId: Long): AgentConnectionEntity?

    @Query("SELECT * FROM agent_connection WHERE pollingEnabled = 1")
    suspend fun getPollingEnabledDirect(): List<AgentConnectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(connection: AgentConnectionEntity)
}