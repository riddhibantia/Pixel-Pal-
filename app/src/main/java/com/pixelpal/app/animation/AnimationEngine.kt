package com.pixelpal.app.animation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class AnimationEngine @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _currentState = MutableStateFlow(AnimationState.IDLE)
    val currentState: StateFlow<AnimationState> = _currentState.asStateFlow()

    private var blinkJob: Job? = null
    private var sleepJob: Job? = null
    private var returnToIdleJob: Job? = null
    private var lastInteractionTime = System.currentTimeMillis()

    fun initialize() {
        startBlinkTimer()
        startSleepTimer()
    }

    fun trigger(state: AnimationState) {
        lastInteractionTime = System.currentTimeMillis()
        returnToIdleJob?.cancel()
        _currentState.value = state

        if (!state.loops || state.nextState != null) {
            scheduleReturn(state)
        }
    }

    private fun startBlinkTimer() {
        blinkJob?.cancel()
        blinkJob = scope.launch {
            while (isActive) {
                val delayMs = Random.nextLong(
                    AnimationConfig.BLINK_MIN_INTERVAL_MS,
                    AnimationConfig.BLINK_MAX_INTERVAL_MS
                )
                delay(delayMs)
                if (_currentState.value == AnimationState.IDLE) {
                    _currentState.value = AnimationState.BLINK
                    delay(AnimationState.BLINK.durationMs)
                    if (_currentState.value == AnimationState.BLINK) {
                        _currentState.value = AnimationState.IDLE
                    }
                }
            }
        }
    }

    private fun startSleepTimer() {
        sleepJob?.cancel()
        sleepJob = scope.launch {
            while (isActive) {
                delay(30_000L)
                val inactiveMs = System.currentTimeMillis() - lastInteractionTime
                if (inactiveMs >= AnimationConfig.SLEEP_TIMEOUT_MS && _currentState.value == AnimationState.IDLE) {
                    _currentState.value = AnimationState.SLEEP
                }
            }
        }
    }

    private fun scheduleReturn(state: AnimationState) {
        returnToIdleJob = scope.launch {
            delay(state.durationMs)
            _currentState.value = state.nextState ?: AnimationState.IDLE
        }
    }

    fun destroy() {
        blinkJob?.cancel()
        sleepJob?.cancel()
        returnToIdleJob?.cancel()
    }
}
