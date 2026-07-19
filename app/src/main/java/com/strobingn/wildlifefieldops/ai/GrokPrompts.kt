package com.strobingn.wildlifefieldops.ai

object GrokPrompts {
    const val SYSTEM = "You are an expert wildlife removal technician and field ops AI. Always return valid JSON only. Be precise, use realistic pricing for New York / New Jersey wildlife jobs in 2026. Prioritize safety, compliance (rabies, permits), insurance documentation, and customer satisfaction."

    fun photoToFormFill(speciesTags: List<String>, damageTags: List<String>, location: String? = null, recentJobs: String = ""): String = """
Analyze wildlife job photo analysis: species=${speciesTags.joinToString()}, damage=${damageTags.joinToString()}, location=$location.
Return strict JSON with species, serviceType, priority, notes, recommendedActions, estimatedPriceLow, estimatedPriceHigh, complianceFlags.
"""

    fun tieredEstimatePrompt(analysis: AiAnalysisResult, jobContext: String = ""): String = """
Create a professional wildlife removal estimate for ${analysis.suggestedServiceType}.
Species: ${analysis.species.joinToString()}
Notes: ${analysis.suggestedNotes}
Context: $jobContext
Return Good/Better/Best options with realistic pricing.
"""

    fun complianceAuditPrompt(formText: String): String = """
Audit this wildlife removal job form for compliance issues: $formText
Return a bullet list of issues and recommendations.
"""

    fun complianceAudit(serviceType: String, formText: String): String = complianceAuditPrompt("$serviceType $formText")

    fun voiceToStructuredJob(voiceText: String, currentJobContext: String = ""): String =
        voiceToMultimodalJson(voiceText, currentJobContext)

    /**
     * Strict JSON for [com.strobingn.wildlifefieldops.ml.model.GrokVoiceJsonDto].
     * Taxonomy ids only — see assets/ml/taxonomy_v1.json.
     */
    fun voiceToMultimodalJson(voiceText: String, knownAddress: String = ""): String = """
Convert this wildlife field voice note into strict JSON only (no markdown).

Transcript:
""" + voiceText + """

Known address hint (may be empty): $knownAddress

Return exactly this shape:
{
  "speciesLabelIds": ["raccoon"],
  "damageLabelIds": ["entry_hole","chew_marks"],
  "severity": 0,
  "customerName": "",
  "address": "",
  "findings": "",
  "recommendations": "",
  "entryPoints": "",
  "notes": "",
  "serviceType": "",
  "priority": "MEDIUM"
}

Rules:
- speciesLabelIds / damageLabelIds MUST use taxonomy ids only:
  species: raccoon,squirrel,bat,bird,rodent,opossum,skunk,snake,coyote,insect_other,unknown,none
  damage: entry_hole,chew_marks,droppings,urine_stain,grease_rub,nest,insulation_damage,wire_damage,scratch_marks,latrine,odor_only,structural_damage,unknown,none
- severity integer 0-4 (0 none … 4 critical). Use >=3 for emergency/urgent/fire/chewed wires.
- priority one of: LOW, MEDIUM, HIGH, URGENT
- Omit unknown guesses; empty arrays are fine.
- Do not invent phone numbers or SSNs.
"""

    fun predictTrapCheckPriority(trapHistory: String, species: String, weather: String, season: String): String = "Predict trap check priority for $species based on $weather $season $trapHistory"

    fun arMeasurementToReport(measurements: String, species: String, damageType: String): String = "Create report from $measurements for $species $damageType"
}
