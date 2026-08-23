package com.pixelpal.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "bond",
    foreignKeys = [
        ForeignKey(
            entity = CompanionEntity::class,
            parentColumns = ["id"],
            childColumns = ["companionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BondEntity(
    @PrimaryKey val companionId: Long,
    val level: Int = 0,
    val totalInteractions: Int = 0,
    val tapsToday: Int = 0,
    val feedsToday: Int = 0,
    val lastInteractionTime: Long = 0L,
    val streakDays: Int = 0,
    val lastStreakDate: String = ""
)