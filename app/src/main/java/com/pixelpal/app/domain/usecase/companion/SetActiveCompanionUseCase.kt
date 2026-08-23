package com.pixelpal.app.domain.usecase.companion

import com.pixelpal.app.domain.engine.ActiveCompanionManager
import javax.inject.Inject

class SetActiveCompanionUseCase @Inject constructor(
    private val activeCompanionManager: ActiveCompanionManager
) {
    suspend operator fun invoke(companionId: Long) {
        activeCompanionManager.setActiveCompanion(companionId)
    }
}