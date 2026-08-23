package com.pixelpal.app.data.repository

import com.pixelpal.app.data.local.db.dao.CompanionDao
import com.pixelpal.app.data.local.db.entity.CompanionEntity
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.CompanionRole
import com.pixelpal.app.domain.repository.CompanionActionResult
import com.pixelpal.app.domain.repository.CompanionRepository
import com.pixelpal.app.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanionRepositoryImpl @Inject constructor(
    private val dao: CompanionDao
) : CompanionRepository {

    override fun getAllActive(): Flow<List<Companion>> =
        dao.getAllActive().map { list -> list.map { it.toDomain() } }

    override fun getAll(): Flow<List<Companion>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getArchived(): Flow<List<Companion>> =
        dao.getArchived().map { list -> list.map { it.toDomain() } }

    override fun getById(id: Long): Flow<Companion?> =
        dao.getById(id).map { it?.toDomain() }

    override suspend fun getAllActiveDirect(): List<Companion> =
        dao.getAllActiveDirect().map { it.toDomain() }

    override suspend fun getAllDirect(): List<Companion> =
        dao.getAllDirect().map { it.toDomain() }

    override suspend fun getByIdDirect(id: Long): Companion? =
        dao.getByIdDirect(id)?.toDomain()

    override suspend fun create(companion: Companion): CompanionActionResult {
        val willBeActive = !companion.isArchived
        if (willBeActive && dao.countActive() >= Constants.MAX_ACTIVE_COMPANIONS) {
            return CompanionActionResult.LimitReached
        }
        return try {
            val id = dao.insert(companion.toEntity())
            CompanionActionResult.Success(companion.copy(id = id))
        } catch (e: Exception) {
            CompanionActionResult.Error(e.message)
        }
    }

    override suspend fun update(companion: Companion) {
        dao.update(companion.toEntity())
    }

    override suspend fun archive(id: Long) {
        dao.setArchived(id, true)
    }

    override suspend fun restore(id: Long): CompanionActionResult {
        if (dao.countActive() >= Constants.MAX_ACTIVE_COMPANIONS) {
            return CompanionActionResult.LimitReached
        }
        val entity = dao.getByIdDirect(id) ?: return CompanionActionResult.Error("Companion not found")
        dao.setArchived(id, false)
        return CompanionActionResult.Success(entity.toDomain().copy(isArchived = false))
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        dao.setFavorite(id, favorite)
    }

    override suspend fun setLastUsed(id: Long) {
        dao.setLastUsed(id, System.currentTimeMillis())
    }

    override suspend fun countActive(): Int = dao.countActive()

    override suspend fun canCreateActive(): Boolean = dao.countActive() < Constants.MAX_ACTIVE_COMPANIONS

    override suspend fun firstActiveDirect(): Companion? = dao.firstActiveDirect()?.toDomain()

    override suspend fun firstAnyDirect(): Companion? = dao.firstAnyDirect()?.toDomain()

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
        accessoryId = accessoryId
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
        accessoryId = accessoryId
    )
}