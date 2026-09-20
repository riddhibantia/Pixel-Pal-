package com.pixelpal.app.data.local.db.entity

import androidx.room.ColumnInfo
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
    indices = [Index("companionId"), Index("cloudId"), Index(value = ["companionId", "updatedAt"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companionId: Long,
    val title: String,
    val description: String? = null,
    val isDone: Boolean = false,
    val dueAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    // Stable cross-device identity for Cloud Firestore documents (Room row ids
    // are device-local autoincrement values and must never key cloud docs).
    @ColumnInfo(defaultValue = "") val cloudId: String = java.util.UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L
)