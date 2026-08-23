package com.pixelpal.app.data.repository

import com.pixelpal.app.data.local.db.dao.PersonalityDao
import com.pixelpal.app.data.local.db.entity.PersonalityEntity
import com.pixelpal.app.domain.model.Personality
import com.pixelpal.app.domain.repository.PersonalityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonalityRepositoryImpl @Inject constructor(
    private val dao: PersonalityDao
) : PersonalityRepository {

    override fun getPersonality(companionId: Long): Flow<Personality> {
        return dao.getPersonality(companionId).map { entity ->
            entity?.toDomain() ?: Personality(companionId = companionId)
        }
    }

    override suspend fun getPersonalityDirect(companionId: Long): Personality {
        return dao.getPersonalityDirect(companionId)?.toDomain() ?: Personality(companionId = companionId)
    }

    override suspend fun updatePersonality(personality: Personality) {
        dao.insertOrUpdate(personality.toEntity())
    }

    override suspend fun ensureExists(companionId: Long) {
        if (dao.getPersonalityDirect(companionId) == null) {
            dao.insertOrUpdate(PersonalityEntity(companionId = companionId))
        }
    }

    private fun PersonalityEntity.toDomain() = Personality(
        companionId = companionId,
        friendliness = friendliness,
        curiosity = curiosity,
        playfulness = playfulness,
        sleepiness = sleepiness,
        confidence = confidence,
        independence = independence,
        lastUpdated = lastUpdated
    )

    private fun Personality.toEntity() = PersonalityEntity(
        companionId = companionId,
        friendliness = friendliness,
        curiosity = curiosity,
        playfulness = playfulness,
        sleepiness = sleepiness,
        confidence = confidence,
        independence = independence,
        lastUpdated = lastUpdated
    )
}