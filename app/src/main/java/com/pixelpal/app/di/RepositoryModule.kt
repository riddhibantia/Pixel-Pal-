package com.pixelpal.app.di

import com.pixelpal.app.data.remote.AgentConnector
import com.pixelpal.app.data.remote.GenericHttpAgentConnector
import com.pixelpal.app.data.repository.ActivityEventRepositoryImpl
import com.pixelpal.app.data.repository.AgentConfigRepositoryImpl
import com.pixelpal.app.data.repository.AgentStatusRepositoryImpl
import com.pixelpal.app.data.repository.BondRepositoryImpl
import com.pixelpal.app.data.repository.CompanionRepositoryImpl
import com.pixelpal.app.data.repository.PersonalityRepositoryImpl
import com.pixelpal.app.data.repository.ReminderRepositoryImpl
import com.pixelpal.app.data.repository.TaskRepositoryImpl
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.repository.AgentConfigRepository
import com.pixelpal.app.domain.repository.AgentStatusRepository
import com.pixelpal.app.domain.repository.BondRepository
import com.pixelpal.app.domain.repository.CompanionRepository
import com.pixelpal.app.domain.repository.PersonalityRepository
import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.domain.repository.TaskRepository
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

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindAgentConfigRepository(impl: AgentConfigRepositoryImpl): AgentConfigRepository

    @Binds
    @Singleton
    abstract fun bindAgentStatusRepository(impl: AgentStatusRepositoryImpl): AgentStatusRepository

    @Binds
    @Singleton
    abstract fun bindActivityEventRepository(impl: ActivityEventRepositoryImpl): ActivityEventRepository

    @Binds
    @Singleton
    abstract fun bindAgentConnector(impl: GenericHttpAgentConnector): AgentConnector
}