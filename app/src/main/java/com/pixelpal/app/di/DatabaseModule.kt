package com.pixelpal.app.di

import android.content.Context
import androidx.room.Room
import com.pixelpal.app.data.local.db.PixelPalDatabase
import com.pixelpal.app.data.local.db.DatabaseMigrations
import com.pixelpal.app.data.local.db.dao.ActivityEventDao
import com.pixelpal.app.data.local.db.dao.AgentConnectionDao
import com.pixelpal.app.data.local.db.dao.BondDao
import com.pixelpal.app.data.local.db.dao.CompanionDao
import com.pixelpal.app.data.local.db.dao.PersonalityDao
import com.pixelpal.app.data.local.db.dao.ReminderDao
import com.pixelpal.app.data.local.db.dao.SubtaskDao
import com.pixelpal.app.data.local.db.dao.TaskDao
import com.pixelpal.app.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): PixelPalDatabase {
        return Room.databaseBuilder(
            context,
            PixelPalDatabase::class.java,
            Constants.DATABASE_NAME
        )
        .addMigrations(
            DatabaseMigrations.MIGRATION_1_3,
            DatabaseMigrations.MIGRATION_2_3,
            DatabaseMigrations.MIGRATION_3_4,
            DatabaseMigrations.MIGRATION_4_5,
            DatabaseMigrations.MIGRATION_5_6,
            DatabaseMigrations.MIGRATION_6_7,
            DatabaseMigrations.MIGRATION_7_8,
            DatabaseMigrations.MIGRATION_8_9,
            DatabaseMigrations.MIGRATION_9_10,
            DatabaseMigrations.MIGRATION_10_11
        )
        // Dev builds may move between versions freely; wiping on downgrade beats crashing.
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()
    }

    @Provides
    fun provideReminderDao(db: PixelPalDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun provideBondDao(db: PixelPalDatabase): BondDao = db.bondDao()

    @Provides
    fun providePersonalityDao(db: PixelPalDatabase): PersonalityDao = db.personalityDao()

    @Provides
    fun provideCompanionDao(db: PixelPalDatabase): CompanionDao = db.companionDao()

    @Provides
    fun provideTaskDao(db: PixelPalDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideSubtaskDao(db: PixelPalDatabase): SubtaskDao = db.subtaskDao()

    @Provides
    fun provideAgentConnectionDao(db: PixelPalDatabase): AgentConnectionDao = db.agentConnectionDao()

    @Provides
    fun provideActivityEventDao(db: PixelPalDatabase): ActivityEventDao = db.activityEventDao()
}