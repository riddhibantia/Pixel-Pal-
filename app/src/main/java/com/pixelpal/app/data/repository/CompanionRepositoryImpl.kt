package com.pixelpal.app.data.repository

import com.pixelpal.app.data.local.db.dao.CompanionDao
import com.pixelpal.app.data.local.db.entity.CompanionEntity
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.repository.CompanionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanionRepositoryImpl @Inject constructor(
    private val dao: CompanionDao
) : CompanionRepository {

    override fun getCompanion(): Flow<Companion> {
        return dao.getCompanion().map { entity ->
            entity?.toDomain() ?: Companion()
        }
    }

    override suspend fun getCompanionDirect(): Companion {
        return dao.getCompanionDirect()?.toDomain() ?: Companion()
    }

    override suspend fun updateCompanion(companion: Companion) {
        dao.insertOrUpdate(companion.toEntity())
    }

    private fun CompanionEntity.toDomain() = Companion(
        id = id,
        petType = petType,
        hatId = hatId,
        outfitId = outfitId,
        accessoryId = accessoryId
    )

    private fun Companion.toEntity() = CompanionEntity(
        id = id,
        petType = petType,
        hatId = hatId,
        outfitId = outfitId,
        accessoryId = accessoryId
    )
}
