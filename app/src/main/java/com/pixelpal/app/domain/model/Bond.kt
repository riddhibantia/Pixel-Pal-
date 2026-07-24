package com.pixelpal.app.domain.model

data class Bond(
    val id: Int = 1,
    val level: Int = 0,
    val totalInteractions: Int = 0,
    val tapsToday: Int = 0,
    val feedsToday: Int = 0,
    val lastInteractionTime: Long = 0L,
    val streakDays: Int = 0,
    val lastStreakDate: String = ""
)