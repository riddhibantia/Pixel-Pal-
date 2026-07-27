package com.pixelpal.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String? = null,
    val triggerTime: Long,
    val recurrence: String = "ONCE",       // ONCE, DAILY, WEEKLY, MONTHLY
    val recurrenceInterval: Long? = null,
    val category: String = "CUSTOM",       // MEETING, MEDICINE, BIRTHDAY, SHOPPING, ASSIGNMENT, CUSTOM
    val status: String = "PENDING",        // PENDING, TRIGGERED, COMPLETED, SNOOZED, DISMISSED
    val snoozeCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
