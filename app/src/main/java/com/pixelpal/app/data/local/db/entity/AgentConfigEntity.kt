package com.pixelpal.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "agent_config",
    foreignKeys = [
        ForeignKey(
            entity = CompanionEntity::class,
            parentColumns = ["id"],
            childColumns = ["companionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AgentConfigEntity(
    @PrimaryKey val companionId: Long,
    val endpointUrl: String = "",
    val enabled: Boolean = false,
    val pollIntervalMinutes: Long = 15,
    val updatedAt: Long = System.currentTimeMillis()
)