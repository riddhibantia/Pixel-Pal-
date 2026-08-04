package com.pixelpal.app.di

import android.content.Context
import androidx.room.Room
import com.pixelpal.app.data.local.db.PixelPalDatabase
import com.pixelpal.app.data.local.db.dao.BondDao
import com.pixelpal.app.data.local.db.dao.CompanionDao
import com.pixelpal.app.data.local.db.dao.PersonalityDao
import com.pixelpal.app.data.local.db.dao.ReminderDao
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
        .fallbackToDestructiveMigration()
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
}