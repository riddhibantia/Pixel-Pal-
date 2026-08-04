package com.pixelpal.app.domain.engine

import com.pixelpal.app.animation.AnimationEngine
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.domain.model.Emotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmotionEngine @Inject constructor(
    private val animationEngine: AnimationEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _currentEmotion = MutableStateFlow(Emotion.CALM)
    val currentEmotion: StateFlow<Emotion> = _currentEmotion.asStateFlow()

    private var decayJob: Job? = null

    fun triggerEmotion(emotion: Emotion, durationMs: Long = 30_000L) {
        decayJob?.cancel()
        _currentEmotion.value = emotion

        val targetAnimation = when (emotion) {
            Emotion.HAPPY -> AnimationState.HAPPY
            Emotion.EXCITED -> AnimationState.EXCITED
            Emotion.CURIOUS -> AnimationState.WALK
            Emotion.SLEEPY -> AnimationState.SLEEP
            Emotion.HUNGRY -> AnimationState.EAT
            Emotion.LONELY -> AnimationState.SAD
            Emotion.CALM -> AnimationState.IDLE
            Emotion.THINKING -> AnimationState.THINKING
            Emotion.SAD -> AnimationState.SAD
        }
        animationEngine.trigger(targetAnimation)

        if (emotion != Emotion.CALM) {
            decayJob = scope.launch {
                delay(durationMs)
                _currentEmotion.value = Emotion.CALM
                animationEngine.trigger(AnimationState.IDLE)
            }
        }
    }
}
