package com.pixelpal.app.di

import com.pixelpal.app.data.repository.BondRepositoryImpl
import com.pixelpal.app.data.repository.CompanionRepositoryImpl
import com.pixelpal.app.data.repository.PersonalityRepositoryImpl
import com.pixelpal.app.data.repository.ReminderRepositoryImpl
import com.pixelpal.app.domain.repository.BondRepository
import com.pixelpal.app.domain.repository.CompanionRepository
import com.pixelpal.app.domain.repository.PersonalityRepository
import com.pixelpal.app.domain.repository.ReminderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds
    @Singleton
    abstract fun bindBondRepository(impl: BondRepositoryImpl): BondRepository

    @Binds
    @Singleton
    abstract fun bindPersonalityRepository(impl: PersonalityRepositoryImpl): PersonalityRepository

    @Binds
    @Singleton
    abstract fun bindCompanionRepository(impl: CompanionRepositoryImpl): CompanionRepository
}
