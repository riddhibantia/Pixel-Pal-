package com.pixelpal.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The SINGLE user companion. Exactly one row is expected; v7 folds any legacy
 * extras into the primary and deletes them. Species/color/pattern are pure
 * appearance — transformation never touches bond/tasks/reminders/agent.
 */
@Entity(tableName = "companions")
data class CompanionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val petType: String,
    val role: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val hatId: String? = null,
    val outfitId: String? = null,
    val accessoryId: String? = null,
    val species: String = "cat",
    val color: String = "orange",
    val pattern: String = "plain"
)