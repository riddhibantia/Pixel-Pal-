package com.pixelpal.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "companion")
data class CompanionEntity(
    @PrimaryKey val id: Int = 1,
    val petType: String = "CAT",
    val hatId: String? = null,
    val outfitId: String? = null,
    val accessoryId: String? = null
)
