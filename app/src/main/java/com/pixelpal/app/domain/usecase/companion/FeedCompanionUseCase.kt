package com.pixelpal.app.domain.usecase.companion

import com.pixelpal.app.domain.engine.CompanionEngine
import javax.inject.Inject

class FeedCompanionUseCase @Inject constructor(
    private val companionEngine: CompanionEngine
) {
    operator fun invoke() {
        companionEngine.onFeed()
    }
}
