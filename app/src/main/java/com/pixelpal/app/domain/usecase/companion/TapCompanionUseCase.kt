package com.pixelpal.app.domain.usecase.companion

import com.pixelpal.app.domain.engine.CompanionEngine
import javax.inject.Inject

class TapCompanionUseCase @Inject constructor(
    private val companionEngine: CompanionEngine
) {
    operator fun invoke() {
        companionEngine.onTap()
    }
}
