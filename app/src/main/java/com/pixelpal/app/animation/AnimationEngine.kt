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
    private var idleBehaviorJob: Job? = null
    private var returnToIdleJob: Job? = null
    private var lastInteractionTime = System.currentTimeMillis()
    private var isScreenOn = true

    fun setScreenOn(isOn: Boolean) {
        isScreenOn = isOn
        if (isOn && _currentState.value == AnimationState.SLEEP) {
            trigger(AnimationState.IDLE)
        }
    }

    fun initialize() {
        startBlinkTimer()
        startIdleBehaviorTimer()
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

    private fun startIdleBehaviorTimer() {
        idleBehaviorJob?.cancel()
        idleBehaviorJob = scope.launch {
            while (isActive) {
                val delayMs = Random.nextLong(
                    AnimationConfig.IDLE_ANIMATION_MIN_INTERVAL_MS,
                    AnimationConfig.IDLE_ANIMATION_MAX_INTERVAL_MS
                )
                delay(delayMs)
                
                val inactiveMs = System.currentTimeMillis() - lastInteractionTime
                
                // Sleep only when the screen is off (do NOT auto-sleep at night while idle on-screen).
                if (inactiveMs >= AnimationConfig.SLEEP_TIMEOUT_MS && !isScreenOn) {
                    if (_currentState.value == AnimationState.IDLE) {
                        _currentState.value = AnimationState.SLEEP
                    }
                    continue
                }
                
                // If we get here, the screen is ON. Do micro-animations if idle
                if (_currentState.value == AnimationState.IDLE) {
                    val randomAction = listOf(
                        AnimationState.WAVE,
                        AnimationState.WALK,
                        AnimationState.JUMP,
                        AnimationState.EXCITED
                    ).random()
                    trigger(randomAction)
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
        idleBehaviorJob?.cancel()
        returnToIdleJob?.cancel()
    }
}
