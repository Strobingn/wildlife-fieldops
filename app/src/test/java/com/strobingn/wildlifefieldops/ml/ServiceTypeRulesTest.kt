package com.strobingn.wildlifefieldops.ml

import com.strobingn.wildlifefieldops.ml.vision.OfflinePriceBands
import com.strobingn.wildlifefieldops.ml.vision.ServiceTypeRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceTypeRulesTest {

    @Test
    fun batSpecies_mapsToBatExclusion() {
        assertEquals("Bat Exclusion", ServiceTypeRules.serviceTypeFor(listOf("bat")))
    }

    @Test
    fun raccoonSpecies_mapsToRaccoonRemoval() {
        assertEquals("Raccoon Removal", ServiceTypeRules.serviceTypeFor(listOf("raccoon")))
    }

    @Test
    fun squirrelSpecies_mapsToSquirrelRemoval() {
        assertEquals("Squirrel Removal", ServiceTypeRules.serviceTypeFor(listOf("squirrel")))
    }

    @Test
    fun entryHoleOnly_mapsToExclusion() {
        assertEquals(
            "Exclusion",
            ServiceTypeRules.serviceTypeFor(emptyList(), listOf("entry_hole"))
        )
    }

    @Test
    fun empty_mapsToInspection() {
        assertEquals("Inspection", ServiceTypeRules.serviceTypeFor(emptyList(), emptyList()))
    }

    @Test
    fun priority_wireDamageIsHigh() {
        assertEquals(
            "HIGH",
            ServiceTypeRules.priorityFor(
                speciesLabelIds = emptyList(),
                damageLabelIds = listOf("wire_damage"),
                severityScore = 2
            )
        )
    }

    @Test
    fun priority_criticalSeverityIsUrgent() {
        assertEquals(
            "URGENT",
            ServiceTypeRules.priorityFor(emptyList(), emptyList(), severityScore = 4)
        )
    }

    @Test
    fun offlinePriceBands_batHigherThanDefault() {
        val bat = OfflinePriceBands.forServiceType("Bat Exclusion")
        val generic = OfflinePriceBands.forServiceType("Inspection")
        assertTrue(bat.low > generic.low)
        assertTrue(bat.rangeLabel.contains("450"))
    }

    @Test
    fun offlinePriceBands_forSpeciesDelegates() {
        val band = OfflinePriceBands.forSpecies(listOf("raccoon"))
        assertEquals(350.0, band.low, 0.01)
        assertEquals(950.0, band.high, 0.01)
    }
}
