package com.pixelpal.app.domain.engine

import com.pixelpal.app.domain.model.Bond
import org.junit.Test
import kotlin.test.assertEquals

class BondEngineTest {

    @Test
    fun testBondDefaults() {
        val bond = Bond()
        assertEquals(0, bond.level)
        assertEquals(0, bond.tapsToday)
        assertEquals(0, bond.feedsToday)
    }

    @Test
    fun testBondLimits() {
        val bond = Bond(level = 99)
        val nextLevel = (bond.level + 3).coerceAtMost(100)
        assertEquals(100, nextLevel)
    }
}
