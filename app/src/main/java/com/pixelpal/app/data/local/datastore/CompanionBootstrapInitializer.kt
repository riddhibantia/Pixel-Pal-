package com.pixelpal.app.data.local.datastore

import androidx.room.withTransaction
import com.pixelpal.app.data.local.db.PixelPalDatabase
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.CompanionRole
import com.pixelpal.app.domain.repository.BondRepository
import com.pixelpal.app.domain.repository.CompanionRepository
import com.pixelpal.app.domain.repository.CompanionActionResult
import com.pixelpal.app.domain.repository.PersonalityRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Post-v7 startup reconciliation:
 *
 *  1. [runSingleCompanionFoldIfNeeded] — ONE-TIME pivot to the single-companion
 *     architecture: picks the primary companion (stored active id → favorite →
 *     most recently used → first), re-keys useful data from legacy extra
 *     companions (pending tasks, pending reminders, activity history, agent
 *     connection when the primary lacks one), then deletes the extra rows.
 *  2. [ensureInitialized] — creates the companion on fresh installs and
 *     ensures bond/personality rows exist.
 *
 * Both steps are idempotent and guarded by DataStore flags.
 */
@Singleton
class CompanionBootstrapInitializer @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val companionRepository: CompanionRepository,
    private val bondRepository: BondRepository,
    private val personalityRepository: PersonalityRepository,
    private val database: PixelPalDatabase
) {

    suspend fun runStartupReconciliation() {
        runSingleCompanionFoldIfNeeded()
        ensureInitialized()
    }

    /**
     * Folds every legacy extra companion into the primary and deletes them.
     * SQL-level because it is a one-time bulk move; guarded by its own flag so
     * it runs exactly once even across migration retries.
     */
    private suspend fun runSingleCompanionFoldIfNeeded() {
        if (preferencesManager.isSingleCompanionFoldDone()) return

        database.withTransaction {
            val all = companionRepository.getAllDirect()
            if (all.size > 1) {
                val storedActiveId = preferencesManager.getActiveCompanionId()
                val primary = all.firstOrNull { it.id == storedActiveId && !it.isArchived }
                    ?: all.firstOrNull { it.isFavorite }
                    ?: all.sortedByDescending { it.lastUsedAt ?: 0L }.first()
                val extras = all.filter { it.id != primary.id }
                val primaryId = primary.id
                val db = database.openHelper.writableDatabase

                extras.forEach { extra ->
                    val id = extra.id
                    // Pending tasks move (completed ones are history the primary didn't earn).
                    db.execSQL(
                        "UPDATE tasks SET companionId = $primaryId " +
                            "WHERE companionId = $id AND isDone = 0"
                    )
                    // Pending reminders move; completed ones stay with their owner's fate.
                    db.execSQL(
                        "UPDATE reminders SET companionId = $primaryId " +
                            "WHERE companionId = $id AND status = 'PENDING'"
                    )
                    // Activity history merges into the primary timeline.
                    db.execSQL(
                        "UPDATE activity_events SET companionId = $primaryId WHERE companionId = $id"
                    )
                    // The AI Agent feature moves when the primary lacks one of its own.
                    db.execSQL(
                        "UPDATE agent_connection SET companionId = $primaryId " +
                            "WHERE companionId = $id AND NOT EXISTS (" +
                            "SELECT 1 FROM agent_connection ac WHERE ac.companionId = $primaryId)"
                    )
                }

                // Remaining child rows cascade with the companion delete.
                db.execSQL("DELETE FROM companions WHERE id != $primaryId")
                preferencesManager.setActiveCompanionId(primaryId)
            } else if (all.size == 1) {
                preferencesManager.setActiveCompanionId(all[0].id)
            }
        }

        preferencesManager.setSingleCompanionFoldDone(true)
    }

    /** Fresh-install creation + bond/personality row guarantees. Idempotent. */
    suspend fun ensureInitialized() {
        database.withTransaction {
            val all = companionRepository.getAllDirect()

            if (all.isEmpty()) {
                val name = preferencesManager.getPetName().ifBlank { "Pixel" }
                val petType = preferencesManager.getSelectedPetType().ifBlank { "cat" }
                when (val result = companionRepository.create(
                    Companion(
                        name = name,
                        petType = petType,
                        species = petType,
                        role = CompanionRole.GENERAL
                    )
                )) {
                    is CompanionActionResult.Success -> Unit
                    else -> return@withTransaction
                }
            }

            // Ensure per-companion bond/personality rows exist.
            companionRepository.getAllDirect().forEach { companion ->
                bondRepository.ensureExists(companion.id)
                personalityRepository.ensureExists(companion.id)
            }

            // Keep the pref pointing at THE companion.
            companionRepository.getPrimaryDirect()?.let {
                if (preferencesManager.getActiveCompanionId() != it.id) {
                    preferencesManager.setActiveCompanionId(it.id)
                }
            }

            preferencesManager.setCompanionBootstrapDone(true)
        }
    }
}