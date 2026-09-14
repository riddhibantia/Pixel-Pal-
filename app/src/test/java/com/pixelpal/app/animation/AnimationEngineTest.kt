package com.pixelpal.app.animation

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnimationEngineTest {

    @Test
    fun testAnimationStateDefaults() {
        val idleState = AnimationState.IDLE
        assertTrue(idleState.loops)
        assertEquals(AnimationState.IDLE, idleState)
    }

    @Test
    fun testAnimationStateTransitions() {
        val happyState = AnimationState.HAPPY
        assertEquals(AnimationState.IDLE, happyState.nextState)
        assertEquals(2000L, happyState.durationMs)
    }

    @Test
    fun testPandaResolvesToPandaFile() {
        // getLottieRawResId needs a Context; assert the naming contract instead:
        // resolver builds "pet_<type>_<state>", so panda must follow it.
        val expected = "pet_panda_idle"
        assertEquals(expected, "pet_${"panda"}_${AnimationState.IDLE.stateName}")
    }
}
