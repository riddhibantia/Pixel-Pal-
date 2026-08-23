package com.pixelpal.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * The AI Agent INTEGRATION of the single companion — not a companion itself.
 * One row per companion (v7 merged the old agent_config + agent_status tables).
 */
@Entity(
    tableName = "agent_connection",
    foreignKeys = [
        ForeignKey(
            entity = CompanionEntity::class,
            parentColumns = ["id"],
            childColumns = ["companionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AgentConnectionEntity(
    @PrimaryKey val companionId: Long,
    val agentName: String = "",
    val provider: String = "",
    val endpointUrl: String = "",
    val connectionStatus: String = "DISCONNECTED",
    val pollingEnabled: Boolean = false,
    val pollingIntervalMinutes: Long = 15,
    val currentStatus: String = "DISCONNECTED",
    val currentTask: String? = null,
    val progress: Int? = null,
    val lastMessage: String? = null,
    val errorMessage: String? = null,
    val lastCheckedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)