package com.pixelpal.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
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
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companionId: Long,
    val title: String,
    val isDone: Boolean = false,
    val dueAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)