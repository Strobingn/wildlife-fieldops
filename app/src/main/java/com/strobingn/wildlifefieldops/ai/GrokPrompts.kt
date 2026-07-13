package com.strobingn.wildlifefieldops.ai

// Heavy Grok prompts for wildlife ops - optimized for structured JSON output
// Use with your existing XAI_API_KEY via Ktor or OkHttp

object GrokPrompts {

    const val SYSTEM = "You are an expert wildlife removal technician and field ops AI. Always return valid JSON only. Be precise, use realistic pricing for New York / New Jersey wildlife jobs in 2026. Prioritize safety, compliance (rabies, permits), insurance documentation, and customer satisfaction."

    // 1. Photo + context to full form fill (species, service, priority, notes, actions)
    fun photoToFormFill(speciesTags: List<String>, damageTags: List<String>, location: String? = null, recentJobs: String = ""): String =
        """
        Analyze this wildlife job photo analysis: species=${speciesTags.joinToString()}, damage=${damageTags.joinToString()}, location=$location.
        Recent similar jobs: $recentJobs
        Return strict JSON: {
          "species": "primary species or comma list",
          "serviceType": "exact service name from standard list (Bat Exclusion, Raccoon Removal, Squirrel Removal, Entry Sealing, etc.)",
          "priority": "HIGH|MEDIUM|LOW",
          "notes": "detailed professional notes including observations, recommended actions, safety notes",
          "recommendedActions": ["action1", "action2"],
          "estimatedPriceLow": number,
          "estimatedPriceHigh": number,
          "complianceFlags": ["flag1 if any missing like rabies protocol"]
        }
        """

    // 2. Tiered Good/Better/Best estimate from photo + job context
    fun photoToTieredEstimate(serviceType: String, species: String, damage: String, photosCount: Int): String =
        """
        Create 3-tier estimate for $serviceType on $species with $damage. Photos: $photosCount.
        Use realistic 2026 NJ/NY pricing. Return JSON array of 3 objects:
        [{
          "tier": "Good|Better|Best",
          "lineItems": ["item1", "item2"],
          "totalLow": number,
          "totalHigh": number,
          "notes": "why this tier, what is included, close rate tip"
        }]
        """

    // 3. Compliance audit on form text or notes
    fun complianceAudit(serviceType: String, formText: String): String =
        """
        Review this completed $serviceType job form/notes for compliance gaps (rabies vector protocol, GPS photos, permit numbers, exclusion standards, customer signature, insurance docs).
        Return JSON: { "issues": ["issue1", "issue2"], "score": 0-100, "recommendations": ["rec1"] }
        """

    // 4. Voice note to structured job data
    fun voiceToStructuredJob(voiceText: String, currentJobContext: String = ""): String =
        """
        Convert this voice note from the field into structured job data.
        Voice: $voiceText
        Context: $currentJobContext
        Return JSON matching Job model fields + recommendedActions array.
        """

    // 5. Predictive trap check priority
    fun predictTrapCheckPriority(trapHistory: String, species: String, weather: String, season: String): String =
        """
        Given trap history, species, current weather and season, predict optimal next check time and urgency.
        Return JSON: { "nextCheckDays": number, "urgency": "HIGH|MEDIUM", "reason": "...", "recommendedAction": "..." }
        """

    // 6. AR measurement interpretation + report text
    fun arMeasurementToReport(measurements: String, species: String, damageType: String): String =
        """
        Turn these AR measurements into professional insurance/perm it ready text.
        Measurements: $measurements
        Return professional paragraph + suggested photos needed.
        """

    // More can be added here for route optimization, inventory prediction, etc.
}
