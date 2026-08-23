package com.pixelpal.app.domain.repository

import com.pixelpal.app.domain.model.Personality
import kotlinx.coroutines.flow.Flow

interface PersonalityRepository {
    fun getPersonality(companionId: Long): Flow<Personality>
    suspend fun getPersonalityDirect(companionId: Long): Personality
    suspend fun updatePersonality(personality: Personality)
    /** Creates a default personality row for the companion if one does not exist. */
    suspend fun ensureExists(companionId: Long)
}