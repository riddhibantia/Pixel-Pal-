package com.pixelpal.app.domain.usecase.companion

import com.pixelpal.app.domain.engine.CompanionEngine
import javax.inject.Inject

class TapCompanionUseCase @Inject constructor(
    private val companionEngine: CompanionEngine
) {
    /** Interacts with a specific companion (falls back to the active one when null). */
    suspend operator fun invoke(companionId: Long? = null) {
        val id = companionId
            ?: companionEngine.resolveActiveCompanionId()
            ?: return
        companionEngine.onTap(id)
    }
}