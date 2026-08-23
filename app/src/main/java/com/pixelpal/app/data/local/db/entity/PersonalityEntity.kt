package com.pixelpal.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "personality",
    foreignKeys = [
        ForeignKey(
            entity = CompanionEntity::class,
            parentColumns = ["id"],
            childColumns = ["companionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PersonalityEntity(
    @PrimaryKey val companionId: Long,
    val friendliness: Float = 0.5f,
    val curiosity: Float = 0.5f,
    val playfulness: Float = 0.5f,
    val sleepiness: Float = 0.5f,
    val confidence: Float = 0.5f,
    val independence: Float = 0.5f,
    val lastUpdated: Long = System.currentTimeMillis()
)