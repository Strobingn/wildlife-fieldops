package com.strobingn.wildlifefieldops.ml.vision

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TaxonomyEntry(
    val id: String,
    val displayName: String
)

@Serializable
data class TaxonomyCatalog(
    val version: Int = 1,
    val species: List<TaxonomyEntry> = emptyList(),
    val damage: List<TaxonomyEntry> = emptyList()
) {
    fun speciesDisplay(id: String): String =
        species.firstOrNull { it.id == id }?.displayName ?: id

    fun damageDisplay(id: String): String =
        damage.firstOrNull { it.id == id }?.displayName ?: id

    fun isKnownSpecies(id: String): Boolean =
        species.any { it.id == id }

    fun isKnownDamage(id: String): Boolean =
        damage.any { it.id == id }

    companion object {
        const val ASSET_PATH = "ml/taxonomy_v1.json"

        private val json = Json { ignoreUnknownKeys = true }

        /** Embedded fallback so unit tests and offline never depend on assets. */
        fun embedded(): TaxonomyCatalog = TaxonomyCatalog(
            version = 1,
            species = listOf(
                TaxonomyEntry("raccoon", "Raccoon"),
                TaxonomyEntry("squirrel", "Squirrel"),
                TaxonomyEntry("bat", "Bat"),
                TaxonomyEntry("bird", "Bird"),
                TaxonomyEntry("rodent", "Rodent"),
                TaxonomyEntry("opossum", "Opossum"),
                TaxonomyEntry("skunk", "Skunk"),
                TaxonomyEntry("snake", "Snake"),
                TaxonomyEntry("coyote", "Coyote"),
                TaxonomyEntry("insect_other", "Insect / other pest"),
                TaxonomyEntry("unknown", "Unknown"),
                TaxonomyEntry("none", "No animal")
            ),
            damage = listOf(
                TaxonomyEntry("entry_hole", "Entry hole / gap"),
                TaxonomyEntry("chew_marks", "Chew marks"),
                TaxonomyEntry("droppings", "Droppings / scat"),
                TaxonomyEntry("urine_stain", "Urine / ammonia stain"),
                TaxonomyEntry("grease_rub", "Grease rub marks"),
                TaxonomyEntry("nest", "Nest / bedding"),
                TaxonomyEntry("insulation_damage", "Insulation damage"),
                TaxonomyEntry("wire_damage", "Chewed wiring"),
                TaxonomyEntry("scratch_marks", "Scratch marks"),
                TaxonomyEntry("latrine", "Latrine"),
                TaxonomyEntry("odor_only", "Odor (no visual)"),
                TaxonomyEntry("structural_damage", "Structural damage"),
                TaxonomyEntry("unknown", "Unknown damage"),
                TaxonomyEntry("none", "No damage observed")
            )
        )

        fun fromJsonString(raw: String): TaxonomyCatalog =
            json.decodeFromString(serializer(), raw)

        fun load(context: Context): TaxonomyCatalog {
            return runCatching {
                context.assets.open(ASSET_PATH).bufferedReader().use { reader ->
                    fromJsonString(reader.readText())
                }
            }.getOrElse { embedded() }
        }
    }
}
