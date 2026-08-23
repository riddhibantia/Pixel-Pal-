package com.pixelpal.app.overlay

import android.content.Context
import com.pixelpal.app.animation.AnimationState

/**
 * Per-overlay-session sprite source. Deliberately NOT a shared singleton:
 * each session renders its own companion's pet type independently, so two
 * overlays never share frames or animation state.
 */
class SessionSpriteRenderer(
    private val context: Context,
    petType: String
) {
    @Volatile var petType: String = petType
        private set

    fun updatePetType(petType: String) {
        this.petType = petType
    }

    fun drawableFor(state: AnimationState): Int {
        val resId = state.getDrawableResId(petType, context)
        if (resId != 0) return resId
        if (state != AnimationState.IDLE) {
            return AnimationState.IDLE.getDrawableResId(petType, context)
        }
        return 0
    }
}