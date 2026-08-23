package com.pixelpal.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pixelpal.app.data.local.db.dao.ActivityEventDao
import com.pixelpal.app.data.local.db.dao.AgentConfigDao
import com.pixelpal.app.data.local.db.dao.AgentStatusDao
import com.pixelpal.app.data.local.db.dao.BondDao
import com.pixelpal.app.data.local.db.dao.CompanionDao
import com.pixelpal.app.data.local.db.dao.PersonalityDao
import com.pixelpal.app.data.local.db.dao.ReminderDao
import com.pixelpal.app.data.local.db.dao.TaskDao
import com.pixelpal.app.data.local.db.entity.ActivityEventEntity
import com.pixelpal.app.data.local.db.entity.AgentConfigEntity
import com.pixelpal.app.data.local.db.entity.AgentStatusEntity
import com.pixelpal.app.data.local.db.entity.BondEntity
import com.pixelpal.app.data.local.db.entity.CompanionEntity
import com.pixelpal.app.data.local.db.entity.PersonalityEntity
import com.pixelpal.app.data.local.db.entity.ReminderEntity
import com.pixelpal.app.data.local.db.entity.TaskEntity

@Database(
    entities = [
        ReminderEntity::class,
        BondEntity::class,
        PersonalityEntity::class,
        CompanionEntity::class,
        TaskEntity::class,
        AgentConfigEntity::class,
        AgentStatusEntity::class,
        ActivityEventEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class PixelPalDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun bondDao(): BondDao
    abstract fun personalityDao(): PersonalityDao
    abstract fun companionDao(): CompanionDao
    abstract fun taskDao(): TaskDao
    abstract fun agentConfigDao(): AgentConfigDao
    abstract fun agentStatusDao(): AgentStatusDao
    abstract fun activityEventDao(): ActivityEventDao
}