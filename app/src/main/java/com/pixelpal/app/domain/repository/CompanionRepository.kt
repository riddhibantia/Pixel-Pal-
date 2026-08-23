package com.pixelpal.app.domain.repository

import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.SpeciesStyle
import kotlinx.coroutines.flow.Flow

/** Single-companion repository: exactly one primary companion is expected. */
interface CompanionRepository {
    fun getPrimary(): Flow<Companion?>
    suspend fun getPrimaryDirect(): Companion?
    fun getById(id: Long): Flow<Companion?>
    suspend fun getByIdDirect(id: Long): Companion?

    /** Maintenance/fold access — should return one row in normal operation. */
    suspend fun getAllDirect(): List<Companion>

    /** Creates the single companion (fresh installs only). */
    suspend fun create(companion: Companion): CompanionActionResult

    /** Appearance transformation (species/color/pattern) + profile edits. */
    suspend fun update(companion: Companion)
    suspend fun transformAppearance(style: SpeciesStyle)

    suspend fun setFavorite(id: Long, favorite: Boolean)
    suspend fun setLastUsed(id: Long)

    suspend fun firstAnyDirect(): Companion?
}