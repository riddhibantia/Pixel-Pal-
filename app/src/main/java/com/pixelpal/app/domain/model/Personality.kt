package com.pixelpal.app.domain.model

data class Personality(
    val id: Int = 1,
    val friendliness: Float = 0.5f,
    val curiosity: Float = 0.5f,
    val playfulness: Float = 0.5f,
    val sleepiness: Float = 0.5f,
    val confidence: Float = 0.5f,
    val independence: Float = 0.5f,
    val lastUpdated: Long = System.currentTimeMillis()
)