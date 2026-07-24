package com.pixelpal.app.animation

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpriteAnimator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val animationEngine: AnimationEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentPetType: String = "cat"

    private val _currentDrawableRes = MutableStateFlow<Int>(0)
    val currentDrawableRes: StateFlow<Int> = _currentDrawableRes.asStateFlow()

    init {
        updateDrawable(animationEngine.currentState.value)
        scope.launch {
            animationEngine.currentState.collect { state ->
                updateDrawable(state)
            }
        }
    }

    fun setPetType(petType: String) {
        currentPetType = petType
        updateDrawable(animationEngine.currentState.value)
    }

    private fun updateDrawable(state: AnimationState) {
        val resId = state.getDrawableResId(currentPetType, context)
        if (resId != 0) {
            _currentDrawableRes.value = resId
        }
    }
}
