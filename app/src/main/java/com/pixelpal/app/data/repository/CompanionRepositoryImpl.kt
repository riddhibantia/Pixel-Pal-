package com.pixelpal.app.data.repository

import com.pixelpal.app.data.local.db.dao.CompanionDao
import com.pixelpal.app.data.local.db.entity.CompanionEntity
import com.pixelpal.app.data.remote.firebase.FirestoreSyncEngine
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.CompanionRole
import com.pixelpal.app.domain.model.SpeciesStyle
import com.pixelpal.app.domain.repository.CompanionActionResult
import com.pixelpal.app.domain.repository.CompanionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanionRepositoryImpl @Inject constructor(
    private val dao: CompanionDao,
    private val syncEngine: FirestoreSyncEngine
) : CompanionRepository {

    override fun getPrimary(): Flow<Companion?> =
        dao.getPrimary().map { it?.toDomain() }

    override suspend fun getPrimaryDirect(): Companion? =
        dao.getPrimaryDirect()?.toDomain()

    override fun getById(id: Long): Flow<Companion?> =
        dao.getById(id).map { it?.toDomain() }

    override suspend fun getByIdDirect(id: Long): Companion? =
        dao.getByIdDirect(id)?.toDomain()

    override suspend fun getAllDirect(): List<Companion> =
        dao.getAllDirect().map { it.toDomain() }

    override suspend fun create(companion: Companion): CompanionActionResult {
        return try {
            val entity = companion.toEntity().copy(updatedAt = System.currentTimeMillis())
            val id = dao.insert(entity)
            val created = entity.copy(id = id).toDomain()
            syncEngine.pushCompanionAsync(created)
            CompanionActionResult.Success(created)
        } catch (e: Exception) {
            CompanionActionResult.Error(e.message)
        }
    }

    override suspend fun update(companion: Companion) {
        val entity = companion.toEntity().copy(updatedAt = System.currentTimeMillis())
        dao.update(entity)
        syncEngine.pushCompanionAsync(entity.toDomain())
    }

    /** Appearance-only update: never touches identity or feature data. */
    override suspend fun transformAppearance(style: SpeciesStyle) {
        val current = dao.getPrimaryDirect() ?: return
        val updated = current.copy(
            species = style.species,
            color = style.color,
            pattern = style.pattern
        )
        val entity = updated.copy(updatedAt = System.currentTimeMillis())
        dao.update(entity)
        syncEngine.pushCompanionAsync(entity.toDomain())
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        dao.setFavorite(id, favorite)
    }

    override suspend fun setLastUsed(id: Long) {
        dao.setLastUsed(id, System.currentTimeMillis())
    }

    override suspend fun firstAnyDirect(): Companion? = dao.getPrimaryDirect()?.toDomain()

    private fun CompanionEntity.toDomain() = Companion(
        id = id,
        name = name,
        petType = petType,
        role = CompanionRole.fromId(role),
        description = description,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        isFavorite = isFavorite,
        isArchived = isArchived,
        hatId = hatId,
        outfitId = outfitId,
        accessoryId = accessoryId,
        species = species,
        color = color,
        pattern = pattern
    )

    private fun Companion.toEntity() = CompanionEntity(
        id = id,
        name = name,
        petType = petType,
        role = role.id,
        description = description,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        isFavorite = isFavorite,
        isArchived = isArchived,
        hatId = hatId,
        outfitId = outfitId,
        accessoryId = accessoryId,
        species = species,
        color = color,
        pattern = pattern
    )
}