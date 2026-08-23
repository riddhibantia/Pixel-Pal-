package com.pixelpal.app.domain.model

data class Companion(
    val id: Long = 0,
    val name: String = "Pixel",
    val petType: String = "cat",
    val role: CompanionRole = CompanionRole.GENERAL,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val hatId: String? = null,
    val outfitId: String? = null,
    val accessoryId: String? = null
)