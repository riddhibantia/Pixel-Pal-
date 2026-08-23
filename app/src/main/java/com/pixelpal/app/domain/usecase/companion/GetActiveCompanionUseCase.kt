package com.pixelpal.app.domain.usecase.companion

import com.pixelpal.app.domain.engine.ActiveCompanionManager
import com.pixelpal.app.domain.model.Companion
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActiveCompanionUseCase @Inject constructor(
    private val activeCompanionManager: ActiveCompanionManager
) {
    val activeCompanion: Flow<Companion?> = activeCompanionManager.activeCompanion
}