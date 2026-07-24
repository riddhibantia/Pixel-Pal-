package com.pixelpal.app.domain.model

data class Reminder(
    val id: Long = 0,
    val title: String,
    val message: String? = null,
    val triggerTime: Long = 0L,
    val recurrence: String = "ONCE",
    val recurrenceInterval: Long? = null,
    val category: String = "CUSTOM",
    val status: String = "PENDING",
    val snoozeCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)