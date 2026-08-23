package com.pixelpal.app.domain.engine

import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.repository.CompanionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single authority for "which companion is active". Every companion-aware
 * component (ViewModels, engines, services) reads the active selection from
 * here instead of reaching into DataStore directly.
 */
@Singleton
class ActiveCompanionManager @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val companionRepository: CompanionRepository
) {
    val activeCompanionId: Flow<Long?> = preferencesManager.activeCompanionId

    /** The active companion, or null when none is selected or it is archived. */
    val activeCompanion: Flow<Companion?> = activeCompanionId.flatMapLatest { id ->
        if (id == null) {
            flowOf(null)
        } else {
            companionRepository.getById(id).map { companion ->
                companion?.takeIf { !it.isArchived }
            }
        }
    }

    suspend fun setActiveCompanion(companionId: Long) {
        preferencesManager.setActiveCompanionId(companionId)
        companionRepository.setLastUsed(companionId)
    }

    suspend fun getActiveCompanionIdDirect(): Long? = preferencesManager.getActiveCompanionId()

    /** Direct (non-reactive) lookup of any companion by id, regardless of active state. */
    suspend fun companionById(companionId: Long): Companion? =
        companionRepository.getByIdDirect(companionId)?.takeIf { !it.isArchived }

    /**
     * Repairs the active selection when it points at a missing or archived
     * companion: falls back to the first active companion, then any companion.
     */
    suspend fun ensureValidActiveCompanion() {
        val current = preferencesManager.getActiveCompanionId()
        val valid = current?.let { id ->
            companionRepository.getByIdDirect(id)?.takeIf { !it.isArchived }
        }
        if (valid == null) {
            val fallback = companionRepository.firstActiveDirect()
                ?: companionRepository.firstAnyDirect()
            if (fallback != null) {
                preferencesManager.setActiveCompanionId(fallback.id)
            }
        }
    }

    /** Call when a companion is archived — repairs the selection if it was active. */
    suspend fun onCompanionArchived(id: Long) {
        if (preferencesManager.getActiveCompanionId() == id) {
            ensureValidActiveCompanion()
        }
    }
}