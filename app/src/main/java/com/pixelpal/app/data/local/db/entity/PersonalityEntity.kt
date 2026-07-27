package com.pixelpal.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personality")
data class PersonalityEntity(
    @PrimaryKey val id: Int = 1,
    val friendliness: Float = 0.5f,
    val curiosity: Float = 0.5f,
    val playfulness: Float = 0.5f,
    val sleepiness: Float = 0.5f,
    val confidence: Float = 0.5f,
    val independence: Float = 0.5f,
    val lastUpdated: Long = System.currentTimeMillis()
)
