package com.pixelpal.app.domain.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AvatarLayerTest {

    @Test
    fun registry_has17OptionsAcross5Slots() {
        assertEquals(17, AvatarOptions.all.size)
        assertEquals(4, AvatarOptions.forSlot(AvatarSlot.EYES).size)
        assertEquals(3, AvatarOptions.forSlot(AvatarSlot.EARS).size)
        assertEquals(4, AvatarOptions.forSlot(AvatarSlot.HEADWEAR).size)
        assertEquals(3, AvatarOptions.forSlot(AvatarSlot.SCARF).size)
        assertEquals(3, AvatarOptions.forSlot(AvatarSlot.AURA).size)
    }

    @Test
    fun unlockLevels_matchSpec() {
        assertTrue(AvatarOptions.forSlot(AvatarSlot.EYES).all { it.unlockBondLevel == 0 })
        assertTrue(AvatarOptions.forSlot(AvatarSlot.EARS).all { it.unlockBondLevel == 0 })
        assertTrue(AvatarOptions.forSlot(AvatarSlot.HEADWEAR).filter { !it.id.startsWith("none") }.all { it.unlockBondLevel == 5 })
        assertTrue(AvatarOptions.forSlot(AvatarSlot.SCARF).filter { !it.id.startsWith("none") }.all { it.unlockBondLevel == 10 })
        assertTrue(AvatarOptions.forSlot(AvatarSlot.AURA).filter { !it.id.startsWith("none") }.all { it.unlockBondLevel == 15 })
    }

    @Test
    fun unknownId_fallsBackToBase() {
        assertNull(AvatarOptions.fromId("dragon-wings"))
        assertNull(AvatarOptions.fromId(null))
        assertEquals("classic", AvatarOptions.fallbackFor(AvatarSlot.EYES).id)
        assertEquals("pointy-cat", AvatarOptions.fallbackFor(AvatarSlot.EARS).id)
        assertEquals("none-headwear", AvatarOptions.fallbackFor(AvatarSlot.HEADWEAR).id)
    }

    @Test
    fun gating_respectsBondLevel() {
        val bow = AvatarOptions.fromId("bow")!!
        assertEquals(false, AvatarOptions.isUnlocked(bow, 4))
        assertEquals(true, AvatarOptions.isUnlocked(bow, 5))
    }
}
