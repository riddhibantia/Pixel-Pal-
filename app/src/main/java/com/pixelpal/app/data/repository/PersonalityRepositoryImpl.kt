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

    override fun getPersonality(): Flow<Personality> {
        return dao.getPersonality().map { entity ->
            entity?.toDomain() ?: Personality()
        }
    }

    override suspend fun getPersonalityDirect(): Personality {
        return dao.getPersonalityDirect()?.toDomain() ?: Personality()
    }

    override suspend fun updatePersonality(personality: Personality) {
        dao.insertOrUpdate(personality.toEntity())
    }

    private fun PersonalityEntity.toDomain() = Personality(
        id = id,
        friendliness = friendliness,
        curiosity = curiosity,
        playfulness = playfulness,
        sleepiness = sleepiness,
        confidence = confidence,
        independence = independence,
        lastUpdated = lastUpdated
    )

    private fun Personality.toEntity() = PersonalityEntity(
        id = id,
        friendliness = friendliness,
        curiosity = curiosity,
        playfulness = playfulness,
        sleepiness = sleepiness,
        confidence = confidence,
        independence = independence,
        lastUpdated = lastUpdated
    )
}
