package com.pixelpal.app.domain.repository

import com.pixelpal.app.domain.model.Companion
import kotlinx.coroutines.flow.Flow

interface CompanionRepository {
    fun getAllActive(): Flow<List<Companion>>
    fun getAll(): Flow<List<Companion>>
    fun getArchived(): Flow<List<Companion>>
    fun getById(id: Long): Flow<Companion?>

    suspend fun getAllActiveDirect(): List<Companion>
    suspend fun getAllDirect(): List<Companion>
    suspend fun getByIdDirect(id: Long): Companion?

    /** Creates a companion, enforcing MAX_ACTIVE_COMPANIONS when it would be active. */
    suspend fun create(companion: Companion): CompanionActionResult

    suspend fun update(companion: Companion)

    suspend fun archive(id: Long)

    /** Restores an archived companion; returns [CompanionActionResult.LimitReached] when at the cap. */
    suspend fun restore(id: Long): CompanionActionResult

    suspend fun setFavorite(id: Long, favorite: Boolean)
    suspend fun setLastUsed(id: Long)
    suspend fun countActive(): Int
    suspend fun canCreateActive(): Boolean
    suspend fun firstActiveDirect(): Companion?
    suspend fun firstAnyDirect(): Companion?
}