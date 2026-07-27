package com.pixelpal.app.domain.usecase.companion

import com.pixelpal.app.domain.engine.BondEngine
import com.pixelpal.app.domain.engine.EmotionEngine
import com.pixelpal.app.domain.model.Bond
import com.pixelpal.app.domain.model.Emotion
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCompanionStateUseCase @Inject constructor(
    private val emotionEngine: EmotionEngine,
    private val bondEngine: BondEngine
) {
    val currentEmotion: Flow<Emotion> = emotionEngine.currentEmotion
    val bond: Flow<Bond> = bondEngine.bond
}
