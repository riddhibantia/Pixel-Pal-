package com.pixelpal.app.domain.usecase.companion

import com.pixelpal.app.domain.engine.CompanionEngine
import javax.inject.Inject

class FeedCompanionUseCase @Inject constructor(
    private val companionEngine: CompanionEngine
) {
    /** Feeds a specific companion (falls back to the active one when null). */
    suspend operator fun invoke(companionId: Long? = null) {
        val id = companionId
            ?: companionEngine.resolveActiveCompanionId()
            ?: return
        companionEngine.onFeed(id)
    }
}