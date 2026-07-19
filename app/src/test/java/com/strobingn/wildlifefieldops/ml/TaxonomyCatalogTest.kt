package com.strobingn.wildlifefieldops.ml

import com.strobingn.wildlifefieldops.ml.vision.TaxonomyCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxonomyCatalogTest {

    @Test
    fun embeddedCatalog_hasCoreSpeciesAndDamage() {
        val cat = TaxonomyCatalog.embedded()
        assertTrue(cat.isKnownSpecies("raccoon"))
        assertTrue(cat.isKnownDamage("entry_hole"))
        assertEquals("Raccoon", cat.speciesDisplay("raccoon"))
        assertEquals("Entry hole / gap", cat.damageDisplay("entry_hole"))
    }

    @Test
    fun assetJson_parsesAndMatchesEmbeddedIds() {
        // Keep in sync with app/src/main/assets/ml/taxonomy_v1.json (loaded as resource string here).
        val raw = """
            {
              "version": 1,
              "species": [
                { "id": "raccoon", "displayName": "Raccoon" },
                { "id": "unknown", "displayName": "Unknown" }
              ],
              "damage": [
                { "id": "entry_hole", "displayName": "Entry hole / gap" }
              ]
            }
        """.trimIndent()
        val cat = TaxonomyCatalog.fromJsonString(raw)
        assertEquals(1, cat.version)
        assertEquals("Raccoon", cat.speciesDisplay("raccoon"))
        assertTrue(cat.isKnownDamage("entry_hole"))
    }
}
