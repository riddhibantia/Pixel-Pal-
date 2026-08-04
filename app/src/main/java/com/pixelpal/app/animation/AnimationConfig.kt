package com.pixelpal.app.animation

/**
 * Timing and behaviour constants for the companion animation system.
 *
 * Asset convention:  pet_{type}_{state}.xml   (vector drawable)
 */
object AnimationConfig {
    // Blink timing
    const val BLINK_MIN_INTERVAL_MS = 3000L
    const val BLINK_MAX_INTERVAL_MS = 8000L

    // Sleep after this much inactivity
    const val SLEEP_TIMEOUT_MS = 120_000L  // 2 minutes

    // Crossfade between drawable states
    const val CROSSFADE_DURATION_MS = 100

    // Idle micro-animation timing
    const val IDLE_ANIMATION_MIN_INTERVAL_MS = 10_000L
    const val IDLE_ANIMATION_MAX_INTERVAL_MS = 25_000L

    // Night time range (24-hour format)
    const val NIGHT_START_HOUR = 22 // 10 PM
    const val NIGHT_END_HOUR = 6    // 6 AM
}
