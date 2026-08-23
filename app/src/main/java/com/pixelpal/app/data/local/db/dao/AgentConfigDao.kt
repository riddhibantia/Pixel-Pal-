package com.pixelpal.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixelpal.app.data.local.db.entity.AgentConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentConfigDao {
    @Query("SELECT * FROM agent_config WHERE companionId = :companionId")
    fun getConfig(companionId: Long): Flow<AgentConfigEntity?>

    @Query("SELECT * FROM agent_config WHERE companionId = :companionId")
    suspend fun getConfigDirect(companionId: Long): AgentConfigEntity?

    @Query("SELECT * FROM agent_config WHERE enabled = 1")
    suspend fun getEnabledDirect(): List<AgentConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: AgentConfigEntity)
}