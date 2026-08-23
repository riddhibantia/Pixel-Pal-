package com.pixelpal.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_events",
    foreignKeys = [
        ForeignKey(
            entity = CompanionEntity::class,
            parentColumns = ["id"],
            childColumns = ["companionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("companionId")]
)
data class ActivityEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companionId: Long,
    val type: String,
    val title: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)