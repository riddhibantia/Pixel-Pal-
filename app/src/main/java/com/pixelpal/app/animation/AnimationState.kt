package com.pixelpal.app.animation

import android.content.Context

enum class AnimationState(
    val drawableResName: String,
    val durationMs: Long,
    val loops: Boolean,
    val nextState: AnimationState?
) {
    IDLE("idle", Long.MAX_VALUE, true, null),
    BLINK("blink", 400L, false, IDLE),
    WALK("walk", 2000L, true, IDLE),
    WAVE("wave", 1500L, false, IDLE),
    JUMP("jump", 800L, false, IDLE),
    SLEEP("sleep", Long.MAX_VALUE, true, null),
    EAT("eat", 2000L, false, HAPPY),
    HAPPY("happy", 2000L, false, IDLE),
    THINKING("thinking", Long.MAX_VALUE, true, null),
    SAD("sad", 3000L, true, IDLE),
    EXCITED("excited", 2500L, false, HAPPY),
    CURIOUS("curious", 2000L, false, IDLE);

    fun getDrawableResId(petType: String, context: Context): Int {
        val name = "pet_${petType.lowercase()}_${drawableResName}"
        return context.resources.getIdentifier(name, "drawable", context.packageName)
    }
}
