package com.pixelpal.app.domain.usecase.personality

import com.pixelpal.app.domain.engine.DailyInteractionStats
import com.pixelpal.app.domain.engine.PersonalityEngine
import javax.inject.Inject

class RecalculatePersonalityUseCase @Inject constructor(
    private val personalityEngine: PersonalityEngine
) {
    suspend operator fun invoke(stats: DailyInteractionStats) {
        personalityEngine.recalculateDaily(stats)
    }
}
