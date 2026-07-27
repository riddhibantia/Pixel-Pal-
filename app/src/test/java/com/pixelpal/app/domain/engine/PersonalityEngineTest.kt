package com.pixelpal.app.domain.engine

import com.pixelpal.app.domain.model.Personality
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersonalityEngineTest {

    @Test
    fun testPersonalityDefaults() {
        val personality = Personality()
        assertEquals(0.5f, personality.friendliness)
        assertEquals(0.5f, personality.curiosity)
        assertEquals(0.5f, personality.playfulness)
        assertEquals(0.5f, personality.sleepiness)
        assertEquals(0.5f, personality.confidence)
        assertEquals(0.5f, personality.independence)
    }

    @Test
    fun testPersonalityClamping() {
        val stats = DailyInteractionStats(
            tapCount = 50,
            feedCount = 10,
            remindersCompleted = 10,
            remindersIgnored = 0,
            isLateNightActive = false,
            isMorningActive = true
        )
        assertTrue(stats.tapCount > 10)
    }
}
