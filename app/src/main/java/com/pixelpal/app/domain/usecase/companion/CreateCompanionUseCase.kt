package com.pixelpal.app.domain.usecase.companion

import com.pixelpal.app.domain.engine.ActiveCompanionManager
import com.pixelpal.app.domain.model.ActivityType
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.repository.CompanionActionResult
import com.pixelpal.app.domain.repository.CompanionRepository
import javax.inject.Inject

class CreateCompanionUseCase @Inject constructor(
    private val companionRepository: CompanionRepository,
    private val activeCompanionManager: ActiveCompanionManager,
    private val activityEventRepository: ActivityEventRepository
) {
    suspend operator fun invoke(companion: Companion): CompanionActionResult {
        val result = companionRepository.create(companion)
        if (result is CompanionActionResult.Success) {
            val created = result.companion
            if (created != null) {
                activeCompanionManager.setActiveCompanion(created.id)
                activityEventRepository.record(
                    companionId = created.id,
                    type = ActivityType.COMPANION_CREATED,
                    title = "${created.name} joined you"
                )
            }
        }
        return result
    }
}