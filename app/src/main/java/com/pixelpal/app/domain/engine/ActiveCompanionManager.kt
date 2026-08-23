package com.pixelpal.app.domain.engine

import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.repository.CompanionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single-companion authority: "the active companion" IS the primary row.
 * The legacy DataStore `active_companion_id` pref is still read because the
 * v7 fold uses it to choose which legacy companion becomes the primary.
 */
@Singleton
class ActiveCompanionManager @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val companionRepository: CompanionRepository
) {
    val activeCompanionId: Flow<Long?> =
        preferencesManager.activeCompanionId

    /** THE companion, or null on a fresh install before creation. */
    val activeCompanion: Flow<Companion?> = companionRepository.getPrimary()

    suspend fun getActiveCompanionIdDirect(): Long? {
        // Prefer the actual primary row; fall back to the stored pref (fold input).
        companionRepository.getPrimaryDirect()?.let { return it.id }
        return preferencesManager.getActiveCompanionId()
    }

    suspend fun getActiveCompanionDirect(): Companion? =
        companionRepository.getPrimaryDirect()

    /** Direct (non-reactive) lookup by id — used by overlay sessions. */
    suspend fun companionById(companionId: Long): Companion? =
        companionRepository.getByIdDirect(companionId)

    /** Marks the primary as recently used (kept for recency signals). */
    suspend fun touchPrimary() {
        companionRepository.getPrimaryDirect()?.let {
            companionRepository.setLastUsed(it.id)
        }
    }

    /** Points the stored pref at a companion (creation/bootstrap paths). */
    suspend fun setActiveCompanion(companionId: Long) {
        preferencesManager.setActiveCompanionId(companionId)
        companionRepository.setLastUsed(companionId)
    }

    /** Legacy repair path; a no-op when exactly one row exists. */
    suspend fun ensureValidActiveCompanion() {
        val primary = companionRepository.getPrimaryDirect()
        if (primary != null && preferencesManager.getActiveCompanionId() != primary.id) {
            preferencesManager.setActiveCompanionId(primary.id)
        }
    }
}