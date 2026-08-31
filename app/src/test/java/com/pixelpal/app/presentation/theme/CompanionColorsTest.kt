package com.pixelpal.app.presentation.theme

import org.junit.Test
import kotlin.test.assertEquals

class CompanionColorsTest {

    @Test
    fun testColorLookup() {
        assertEquals(CompanionColors.Blue, CompanionColors.forName("blue"))
        assertEquals(CompanionColors.Purple, CompanionColors.forName("purple"))
        assertEquals(CompanionColors.Pink, CompanionColors.forName("pink"))
        assertEquals(CompanionColors.Green, CompanionColors.forName("green"))
        assertEquals(CompanionColors.Orange, CompanionColors.forName("orange"))
    }

    @Test
    fun testFallbackColor() {
        assertEquals(CompanionColors.Orange, CompanionColors.forName("unknown"))
        assertEquals(CompanionColors.Orange, CompanionColors.forName(""))
    }
}
