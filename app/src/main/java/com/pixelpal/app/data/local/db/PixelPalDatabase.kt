package com.pixelpal.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pixelpal.app.data.local.db.dao.BondDao
import com.pixelpal.app.data.local.db.dao.CompanionDao
import com.pixelpal.app.data.local.db.dao.PersonalityDao
import com.pixelpal.app.data.local.db.dao.ReminderDao
import com.pixelpal.app.data.local.db.entity.BondEntity
import com.pixelpal.app.data.local.db.entity.CompanionEntity
import com.pixelpal.app.data.local.db.entity.PersonalityEntity
import com.pixelpal.app.data.local.db.entity.ReminderEntity

@Database(
    entities = [
        ReminderEntity::class,
        BondEntity::class,
        PersonalityEntity::class,
        CompanionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PixelPalDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun bondDao(): BondDao
    abstract fun personalityDao(): PersonalityDao
    abstract fun companionDao(): CompanionDao
}