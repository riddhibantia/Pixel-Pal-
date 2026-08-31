package com.pixelpal.app.animation

import android.content.Context

/**
 * Every visual state a companion can be in.
 *
 * The drawable lookup is fully data-driven:
 *   PetType + AnimationState  →  R.drawable.pet_{type}_{state}
 *
 * Adding a new state only requires:
 *   1. Add the enum entry here
 *   2. Drop a vector drawable named pet_{type}_{state}.xml into res/drawable
 */
enum class AnimationState(
    val stateName: String,
    val durationMs: Long,
    val loops: Boolean,
    val nextState: AnimationState?
) {
    IDLE("idle", Long.MAX_VALUE, true, null),
    BLINK("blink", 400L, false, IDLE),
    HAPPY("happy", 2000L, false, IDLE),
    SAD("sad", 3000L, true, IDLE),
    SLEEP("sleep", Long.MAX_VALUE, true, null),
    THINKING("thinking", Long.MAX_VALUE, true, null),
    WAVE("wave", 1500L, false, IDLE),
    JUMP("jump", 800L, false, IDLE),
    EXCITED("excited", 2500L, false, HAPPY),
    CELEBRATE("celebrate", 3000L, false, HAPPY),
    EAT("eat", 2000L, false, HAPPY),
    WALK("walk", 2000L, true, IDLE);

    /**
     * Resolves the drawable resource for this state and the given pet type.
     * Returns 0 if no matching drawable exists (caller should fall back to IDLE).
     */
    fun getDrawableResId(petType: String, context: Context): Int {
        val name = "pet_${petType.lowercase()}_$stateName"
        return context.resources.getIdentifier(name, "drawable", context.packageName)
    }

    /**
     * Resolves the Lottie raw JSON resource for this state and pet type.
     * Returns 0 if no matching Lottie animation exists.
     */
    fun getLottieRawResId(petType: String, context: Context): Int {
        val specificName = "pet_${petType.lowercase()}_$stateName"
        val specificId = context.resources.getIdentifier(specificName, "raw", context.packageName)
        if (specificId != 0) return specificId

        val genericName = "lottie_pet_$stateName"
        return context.resources.getIdentifier(genericName, "raw", context.packageName)
    }
}
