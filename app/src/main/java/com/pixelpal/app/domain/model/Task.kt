package com.pixelpal.app.domain.model

data class Task(
    val id: Long = 0,
    val companionId: Long,
    val title: String,
    val isDone: Boolean = false,
    val dueAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)