package com.pixelpal.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "agent_status",
    foreignKeys = [
        ForeignKey(
            entity = CompanionEntity::class,
            parentColumns = ["id"],
            childColumns = ["companionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AgentStatusEntity(
    @PrimaryKey val companionId: Long,
    val state: String = "IDLE",
    val message: String? = null,
    val lastCheckedAt: Long? = null,
    val lastSuccessfulCheckAt: Long? = null,
    val consecutiveFailureCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)