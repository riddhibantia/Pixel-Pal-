package com.pixelpal.app.domain.repository

import com.pixelpal.app.domain.model.Personality
import kotlinx.coroutines.flow.Flow

interface PersonalityRepository {
    fun getPersonality(): Flow<Personality>
    suspend fun getPersonalityDirect(): Personality
    suspend fun updatePersonality(personality: Personality)
}
