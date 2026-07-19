package com.strobingn.wildlifefieldops.ml

import com.strobingn.wildlifefieldops.ml.model.DraftHints
import com.strobingn.wildlifefieldops.ml.vision.TaxonomyMapper
import com.strobingn.wildlifefieldops.ml.voice.RegexVoiceJobParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegexVoiceJobParserTest {

    private val parser = RegexVoiceJobParser(TaxonomyMapper.default())

    @Test
    fun raccoonAtticChewedWires() = runBlocking {
        val r = parser.parse(
            "Raccoon in the attic chewed wires near the vent, customer is John Smith at 12 Main Street"
        )
        assertTrue(r.speciesLabelIds.any { it.labelId == "raccoon" })
        assertTrue(r.damageLabelIds.any { it.labelId == "wire_damage" })
        assertTrue(r.severity >= 3)
        assertTrue(r.address.contains("Main", ignoreCase = true))
        assertEquals("John Smith", r.customerName)
        assertTrue(r.serviceType.contains("Raccoon", ignoreCase = true))
    }

    @Test
    fun batEmergencyBoostsSeverity() = runBlocking {
        val r = parser.parse("Urgent bat colony in soffit, emergency call")
        assertTrue(r.speciesLabelIds.any { it.labelId == "bat" })
        assertTrue(r.urgencyKeywords.isNotEmpty())
        assertTrue(r.severity >= 3)
    }

    @Test
    fun squirrelDroppingsNest() = runBlocking {
        val r = parser.parse("Squirrel nest and droppings in insulation")
        assertTrue(r.speciesLabelIds.any { it.labelId == "squirrel" })
        assertTrue(r.damageLabelIds.any { it.labelId == "nest" })
        assertTrue(r.damageLabelIds.any { it.labelId == "droppings" })
        assertTrue(r.damageLabelIds.any { it.labelId == "insulation_damage" })
    }

    @Test
    fun emptyTranscript_usesHints() = runBlocking {
        val r = parser.parse(
            "",
            DraftHints(knownCustomerName = "Ada", knownAddress = "9 Oak Rd")
        )
        assertEquals("Ada", r.customerName)
        assertEquals("9 Oak Rd", r.address)
        assertTrue(r.speciesLabelIds.isEmpty())
    }

    @Test
    fun rodentSynonyms() = runBlocking {
        val r = parser.parse("rats and mice in the crawl space")
        assertTrue(r.speciesLabelIds.any { it.labelId == "rodent" })
        assertTrue(r.entryPoints.contains("crawl", ignoreCase = true))
    }
}
