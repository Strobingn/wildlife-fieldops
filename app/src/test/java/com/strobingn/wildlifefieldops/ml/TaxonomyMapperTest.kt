package com.strobingn.wildlifefieldops.ml

import com.strobingn.wildlifefieldops.ml.model.PredictionTarget
import com.strobingn.wildlifefieldops.ml.vision.TaxonomyMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxonomyMapperTest {

    private val mapper = TaxonomyMapper.default()

    @Test
    fun mapsRaccoonSpecies() {
        val m = mapper.mapSpecies("Raccoon", 0.91f)
        assertEquals("raccoon", m.labelId)
        assertEquals("Raccoon", m.displayLabel)
        assertEquals(PredictionTarget.SPECIES, m.target)
        assertEquals(0.91f, m.confidence, 0.001f)
    }

    @Test
    fun mapsBirdSynonyms() {
        assertEquals("bird", mapper.mapSpecies("pigeon").labelId)
        assertEquals("bird", mapper.mapSpecies("American crow").labelId)
    }

    @Test
    fun mapsRodentSynonyms() {
        assertEquals("rodent", mapper.mapSpecies("brown rat").labelId)
        assertEquals("rodent", mapper.mapSpecies("house mouse").labelId)
    }

    @Test
    fun mapsEntryHoleDamage() {
        val m = mapper.mapDamage("roof vent gap opening", 0.8f)
        assertEquals("entry_hole", m.labelId)
        assertEquals(PredictionTarget.DAMAGE, m.target)
    }

    @Test
    fun mapsWireDamage() {
        assertEquals("wire_damage", mapper.mapDamage("chewed electrical wire").labelId)
    }

    @Test
    fun mapAll_dedupesAndKeepsBestConfidence() {
        val mapped = mapper.mapAll(
            listOf(
                TaxonomyMapper.RawLabel("raccoon", 0.6f),
                TaxonomyMapper.RawLabel("raccoon face", 0.95f),
                TaxonomyMapper.RawLabel("entry hole", 0.7f)
            )
        )
        val raccoon = mapped.first { it.labelId == "raccoon" }
        assertEquals(0.95f, raccoon.confidence, 0.001f)
        assertTrue(mapped.any { it.labelId == "entry_hole" })
    }

    @Test
    fun primarySpeciesAndDamage() {
        val mapped = mapper.mapAll(
            listOf(
                TaxonomyMapper.RawLabel("squirrel", 0.88f),
                TaxonomyMapper.RawLabel("chew marks", 0.77f),
                TaxonomyMapper.RawLabel("unknown blob", 0.4f)
            )
        )
        assertEquals("squirrel", mapper.primarySpeciesId(mapped))
        assertEquals("chew_marks", mapper.primaryDamageId(mapped))
    }

    @Test
    fun severity_wireIsHigh() {
        val score = mapper.estimateSeverityScore(listOf("wire_damage", "entry_hole"))
        assertTrue(score >= 3)
    }

    @Test
    fun foxMapsToUnknownPerPolicy() {
        assertEquals("unknown", mapper.mapSpecies("red fox").labelId)
    }
}
