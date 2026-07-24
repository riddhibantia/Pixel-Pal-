package com.pixelpal.app.di

import android.content.Context
import androidx.room.Room
import com.pixelpal.app.data.local.db.PixelPalDatabase
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
        ).build()
    }
}