package com.strobingn.wildlifefieldops.ml.vision

/**
 * Offline Good–Better price bands used until the P1 margin model ships.
 * Values extracted from the prior PhotoAIHelper heuristics (centralized here).
 */
data class PriceBand(
    val low: Double,
    val high: Double
) {
    val rangeLabel: String
        get() = "$${low.toInt()} - $${"%,d".format(high.toInt())}"
}

object OfflinePriceBands {

    fun forServiceType(serviceType: String): PriceBand {
        val s = serviceType.lowercase()
        return when {
            s.contains("bat") -> PriceBand(450.0, 1200.0)
            s.contains("raccoon") -> PriceBand(350.0, 950.0)
            s.contains("squirrel") -> PriceBand(275.0, 750.0)
            s.contains("skunk") -> PriceBand(300.0, 850.0)
            s.contains("snake") -> PriceBand(250.0, 700.0)
            s.contains("bird") -> PriceBand(225.0, 650.0)
            s.contains("insulation") -> PriceBand(400.0, 1500.0)
            s.contains("exclusion") -> PriceBand(350.0, 1100.0)
            s.contains("sanitation") || s.contains("disinfection") || s.contains("clean") ->
                PriceBand(300.0, 900.0)
            s.contains("repair") -> PriceBand(250.0, 800.0)
            s.contains("emergency") -> PriceBand(400.0, 1200.0)
            else -> PriceBand(200.0, 600.0)
        }
    }

    fun forSpecies(speciesLabelIds: List<String>): PriceBand {
        val service = ServiceTypeRules.serviceTypeFor(speciesLabelIds)
        return forServiceType(service)
    }
}
