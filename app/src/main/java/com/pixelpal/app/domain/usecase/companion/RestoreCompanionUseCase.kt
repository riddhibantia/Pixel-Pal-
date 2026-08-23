package com.pixelpal.app.domain.usecase.companion

import com.pixelpal.app.domain.model.ActivityType
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.repository.CompanionActionResult
import com.pixelpal.app.domain.repository.CompanionRepository
import javax.inject.Inject

class RestoreCompanionUseCase @Inject constructor(
    private val companionRepository: CompanionRepository,
    private val activityEventRepository: ActivityEventRepository
) {
    suspend operator fun invoke(companionId: Long): CompanionActionResult {
        val result = companionRepository.restore(companionId)
        if (result is CompanionActionResult.Success) {
            result.companion?.let {
                activityEventRepository.record(
                    companionId = it.id,
                    type = ActivityType.COMPANION_RESTORED,
                    title = "${it.name} was restored"
                )
            }
        }
        return result
    }
}