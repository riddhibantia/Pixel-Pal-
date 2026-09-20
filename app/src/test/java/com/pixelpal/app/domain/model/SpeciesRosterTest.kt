package com.pixelpal.app.domain.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeciesRosterTest {

    @Test
    fun roster_hasPanda_noRabbit() {
        val ids = PetType.entries.map { it.id }
        assertTrue("panda" in ids, "roster missing panda: $ids")
        assertTrue("rabbit" !in ids, "roster still contains rabbit: $ids")
        assertTrue("fox" !in ids, "roster still contains fox: $ids")
        assertEquals(7, ids.size)
    }

    @Test
    fun panda_unlocksAtRabbitSlot() {
        val panda = PetType.fromId("panda")
        assertEquals("Panda", panda.displayName)
        assertEquals(30, panda.unlockBondLevel)
        assertTrue(panda.hasFullAnimationSet)
    }

    @Test
    fun customizeSpecies_hasPanda_noRabbit() {
        assertTrue("panda" in SpeciesStyle.SPECIES)
        assertTrue("rabbit" !in SpeciesStyle.SPECIES)
        assertTrue("fox" !in SpeciesStyle.SPECIES)
        assertEquals(7, SpeciesStyle.SPECIES.size)
    }
}
