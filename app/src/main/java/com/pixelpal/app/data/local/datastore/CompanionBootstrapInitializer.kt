package com.pixelpal.app.data.local.datastore

import androidx.room.withTransaction
import com.pixelpal.app.data.local.db.PixelPalDatabase
import com.pixelpal.app.domain.engine.ActiveCompanionManager
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.CompanionRole
import com.pixelpal.app.domain.repository.BondRepository
import com.pixelpal.app.domain.repository.CompanionRepository
import com.pixelpal.app.domain.repository.CompanionActionResult
import com.pixelpal.app.domain.repository.PersonalityRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Idempotent post-migration bootstrap. Runs once (guarded by a DataStore flag)
 * and reconciles Room companion data with the legacy DataStore pet identity:
 *
 *  - creates a default companion from legacy name/type when none exists,
 *  - adopts the legacy name/type when the migrated companion is still the
 *    placeholder 'Pixel' row (created by MIGRATION_4_5),
 *  - ensures bond + personality rows exist for every companion,
 *  - selects a valid active companion,
 *  - marks bootstrap complete.
 *
 * Re-running never duplicates companions.
 */
@Singleton
class CompanionBootstrapInitializer @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val companionRepository: CompanionRepository,
    private val activeCompanionManager: ActiveCompanionManager,
    private val bondRepository: BondRepository,
    private val personalityRepository: PersonalityRepository,
    private val database: PixelPalDatabase
) {

    suspend fun ensureInitialized() {
        if (preferencesManager.isCompanionBootstrapDone()) return

        database.withTransaction {
            val all = companionRepository.getAllDirect()

            when {
                all.isEmpty() -> {
                    val name = preferencesManager.getPetName().ifBlank { "Pixel" }
                    val petType = preferencesManager.getSelectedPetType().ifBlank { "cat" }
                    companionRepository.create(
                        Companion(
                            name = name,
                            petType = petType,
                            role = CompanionRole.GENERAL
                        )
                    )
                }

                // Single migrated placeholder row (MIGRATION_4_5 inserts name 'Pixel').
                all.size == 1 && all[0].name == "Pixel" -> {
                    val legacy = all[0]
                    val legacyName = preferencesManager.getPetName().ifBlank { "Pixel" }
                    val legacyType = preferencesManager.getSelectedPetType().ifBlank { "cat" }
                    if (legacyName != "Pixel" || legacyType != legacy.petType) {
                        companionRepository.update(
                            legacy.copy(name = legacyName, petType = legacyType)
                        )
                    }
                }
            }

            // Ensure per-companion bond/personality rows exist.
            companionRepository.getAllDirect().forEach { companion ->
                bondRepository.ensureExists(companion.id)
                personalityRepository.ensureExists(companion.id)
            }

            activeCompanionManager.ensureValidActiveCompanion()

            preferencesManager.setCompanionBootstrapDone(true)
        }
    }
}