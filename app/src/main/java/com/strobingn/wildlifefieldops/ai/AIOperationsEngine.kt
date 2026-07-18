package com.strobingn.wildlifefieldops.ai

import com.strobingn.wildlifefieldops.data.model.InventoryItem
import com.strobingn.wildlifefieldops.data.model.Job
import com.strobingn.wildlifefieldops.data.model.JobPriority
import com.strobingn.wildlifefieldops.data.model.JobStatus
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class PrioritizedJob(
    val jobId: String,
    val title: String,
    val address: String,
    val score: Int,
    val riskLevel: String,
    val reasons: List<String>
)

data class PropertyRisk(
    val address: String,
    val score: Int,
    val riskLevel: String,
    val serviceCount: Int,
    val activeJobs: Int,
    val primaryIssue: String
)

data class SeasonalForecast(
    val species: String,
    val activity: String,
    val confidencePercent: Int,
    val evidence: String,
    val recommendedAction: String
)

data class OperationsInsight(
    val title: String,
    val value: String,
    val detail: String,
    val severity: String = "INFO"
)

data class InventoryForecast(
    val itemId: String,
    val name: String,
    val available: Double,
    val recommendedOrder: Double,
    val reason: String
)

data class SafetySignal(
    val title: String,
    val affectedJobs: Int,
    val action: String,
    val severity: String
)

data class AIOperationsSnapshot(
    val generatedAt: Long = System.currentTimeMillis(),
    val jobsAnalyzed: Int = 0,
    val activeJobs: Int = 0,
    val urgentJobs: Int = 0,
    val highRiskProperties: Int = 0,
    val prioritizedJobs: List<PrioritizedJob> = emptyList(),
    val propertyRisks: List<PropertyRisk> = emptyList(),
    val seasonalForecasts: List<SeasonalForecast> = emptyList(),
    val businessInsights: List<OperationsInsight> = emptyList(),
    val inventoryForecasts: List<InventoryForecast> = emptyList(),
    val safetySignals: List<SafetySignal> = emptyList()
) {
    fun toPrivacySafePrompt(): String = buildString {
        appendLine("Create a concise wildlife-removal operations briefing from this aggregate data.")
        appendLine("Do not invent customers, addresses, laws, prices, or events.")
        appendLine("Jobs analyzed: $jobsAnalyzed; active: $activeJobs; urgent: $urgentJobs")
        appendLine("High-risk properties: $highRiskProperties")
        appendLine("Priority scores: ${prioritizedJobs.take(5).joinToString { it.score.toString() }}")
        appendLine("Seasonal signals: ${seasonalForecasts.joinToString { "${it.species} ${it.confidencePercent}%" }}")
        appendLine("Inventory alerts: ${inventoryForecasts.size}")
        appendLine("Safety alerts: ${safetySignals.joinToString { "${it.title} (${it.affectedJobs})" }}")
        appendLine("Business indicators: ${businessInsights.joinToString { "${it.title}: ${it.value}" }}")
        appendLine("Return: 1) today's priorities, 2) field risks, 3) inventory action, 4) pricing/operations action.")
    }
}

/**
 * Local intelligence layer. Every result is derived from live Room records and
 * remains available offline. The screen can additionally send the privacy-safe
 * aggregate snapshot to the configured LLM for a generated operations briefing.
 */
object AIOperationsEngine {
    private const val DAY_MS = 86_400_000L

    fun analyze(
        jobs: List<Job>,
        inventory: List<InventoryItem>,
        now: Long = System.currentTimeMillis()
    ): AIOperationsSnapshot {
        val active = jobs.filterNot { it.status.isClosed() }
        val propertyCounts = jobs
            .filter { it.address.isNotBlank() }
            .groupingBy { normalizeAddress(it.address) }
            .eachCount()

        val prioritized = active.map { job ->
            prioritizeJob(job, propertyCounts[normalizeAddress(job.address)] ?: 0, now)
        }.sortedByDescending { it.score }

        val propertyRisks = buildPropertyRisks(jobs)
        val seasonal = buildSeasonalForecasts(jobs)
        val insights = buildBusinessInsights(jobs, active)
        val inventoryForecasts = buildInventoryForecasts(inventory, active)
        val safetySignals = buildSafetySignals(active)

        return AIOperationsSnapshot(
            jobsAnalyzed = jobs.size,
            activeJobs = active.size,
            urgentJobs = active.count { it.priority == JobPriority.URGENT },
            highRiskProperties = propertyRisks.count { it.score >= 60 },
            prioritizedJobs = prioritized.take(12),
            propertyRisks = propertyRisks.take(10),
            seasonalForecasts = seasonal,
            businessInsights = insights,
            inventoryForecasts = inventoryForecasts.take(12),
            safetySignals = safetySignals
        )
    }

    private fun prioritizeJob(job: Job, repeatVisits: Int, now: Long): PrioritizedJob {
        var score = when (job.priority) {
            JobPriority.URGENT -> 45
            JobPriority.HIGH -> 30
            JobPriority.MEDIUM -> 15
            JobPriority.LOW -> 5
        }
        val reasons = mutableListOf<String>()
        reasons += "${job.priority.name.lowercase().replaceFirstChar { it.uppercase() }} priority"

        job.scheduledDate?.let { scheduled ->
            if (scheduled < now) {
                val overdueDays = max(1, ((now - scheduled) / DAY_MS).toInt())
                score += min(30, 10 + overdueDays * 4)
                reasons += "$overdueDays day${if (overdueDays == 1) "" else "s"} overdue"
            } else if (scheduled - now <= DAY_MS) {
                score += 8
                reasons += "Scheduled within 24 hours"
            }
        }
        if (job.status == JobStatus.IN_PROGRESS) {
            score += 8
            reasons += "Work already in progress"
        }
        if (repeatVisits > 1) {
            score += min(18, (repeatVisits - 1) * 6)
            reasons += "$repeatVisits records at this property"
        }
        val text = job.searchableText()
        if (SAFETY_TERMS.any { text.contains(it) }) {
            score += 12
            reasons += "Elevated wildlife/PPE risk"
        }
        if (DAMAGE_TERMS.any { text.contains(it) }) {
            score += 8
            reasons += "Structural or contamination indicators"
        }
        if (!job.isSynced) {
            score += 3
            reasons += "Pending cloud sync"
        }
        if (job.photos.isEmpty()) {
            score += 3
            reasons += "No job photos documented"
        }
        val finalScore = score.coerceIn(0, 100)
        return PrioritizedJob(
            jobId = job.id,
            title = job.title.ifBlank { job.type },
            address = job.address,
            score = finalScore,
            riskLevel = riskLevel(finalScore),
            reasons = reasons.distinct().take(4)
        )
    }

    private fun buildPropertyRisks(jobs: List<Job>): List<PropertyRisk> {
        return jobs.filter { it.address.isNotBlank() }
            .groupBy { normalizeAddress(it.address) }
            .map { (_, records) ->
                val active = records.count { !it.status.isClosed() }
                val urgent = records.count { it.priority == JobPriority.URGENT || it.priority == JobPriority.HIGH }
                val damage = records.count { job ->
                    val text = job.searchableText()
                    DAMAGE_TERMS.any { text.contains(it) }
                }
                val recurring = max(0, records.size - 1)
                val score = (active * 18 + urgent * 12 + damage * 10 + recurring * 9).coerceIn(0, 100)
                val primaryIssue = records.groupingBy { it.type.ifBlank { "Wildlife service" } }
                    .eachCount()
                    .maxByOrNull { it.value }
                    ?.key
                    .orEmpty()
                PropertyRisk(
                    address = records.first().address,
                    score = score,
                    riskLevel = riskLevel(score),
                    serviceCount = records.size,
                    activeJobs = active,
                    primaryIssue = primaryIssue
                )
            }
            .sortedByDescending { it.score }
    }

    private fun buildSeasonalForecasts(jobs: List<Job>): List<SeasonalForecast> {
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        val patterns = when (month) {
            1, 2 -> listOf(
                SeasonalPattern("Rodents", listOf("mouse", "mice", "rat", "rodent"), "High indoor pressure", "Inspect foundations, pipe chases, and garage seals"),
                SeasonalPattern("Squirrels", listOf("squirrel"), "Denning and attic activity", "Check soffits, roof returns, and chewed vents"),
                SeasonalPattern("Bats", listOf("bat"), "Overwintering disturbance", "Avoid winter exclusion; document access points")
            )
            3, 4 -> listOf(
                SeasonalPattern("Raccoons", listOf("raccoon"), "Maternity den activity rising", "Prioritize attic/chimney inspections and dependent checks"),
                SeasonalPattern("Squirrels", listOf("squirrel"), "Spring nesting peak", "Inspect roof edges before sealing entries"),
                SeasonalPattern("Birds", listOf("bird", "sparrow", "starling"), "Nesting pressure rising", "Verify nest status before exclusion")
            )
            5, 6 -> listOf(
                SeasonalPattern("Bats", listOf("bat"), "Maternity colonies active", "Plan compliant exclusion timing and document all exits"),
                SeasonalPattern("Raccoons", listOf("raccoon"), "Juvenile activity high", "Use family-unit protocols before repairs"),
                SeasonalPattern("Woodchucks", listOf("woodchuck", "groundhog"), "Burrow and garden damage peak", "Map burrows and check structural undermining"),
                SeasonalPattern("Wasps", listOf("wasp", "hornet", "yellowjacket"), "Nest establishment accelerating", "Carry sting PPE and inspect voids")
            )
            7, 8 -> listOf(
                SeasonalPattern("Bats", listOf("bat"), "Flight and entry activity high", "Use dusk observations to confirm exits"),
                SeasonalPattern("Wasps", listOf("wasp", "hornet", "yellowjacket"), "Colony size near annual peak", "Prioritize occupied structures and allergy risks"),
                SeasonalPattern("Snakes", listOf("snake"), "Warm-weather encounters elevated", "Inspect prey sources and foundation gaps")
            )
            9, 10 -> listOf(
                SeasonalPattern("Rodents", listOf("mouse", "mice", "rat", "rodent"), "Fall entry pressure rising", "Pre-stage exclusion materials and inspect lower gaps"),
                SeasonalPattern("Bats", listOf("bat"), "Migration and swarming activity", "Document routes and schedule eligible exclusions"),
                SeasonalPattern("Squirrels", listOf("squirrel"), "Fall den preparation", "Inspect roofline and attic insulation disturbance")
            )
            else -> listOf(
                SeasonalPattern("Rodents", listOf("mouse", "mice", "rat", "rodent"), "Cold-weather entry pressure high", "Prioritize foundation and utility penetrations"),
                SeasonalPattern("Squirrels", listOf("squirrel"), "Attic denning pressure", "Check vents, fascia, and roof returns"),
                SeasonalPattern("Raccoons", listOf("raccoon"), "Shelter-seeking activity", "Inspect chimneys and weak roof intersections")
            )
        }
        return patterns.map { pattern ->
            val historical = jobs.count { job ->
                val text = job.searchableText()
                pattern.terms.any { text.contains(it) }
            }
            val confidence = min(94, 58 + historical * 4)
            SeasonalForecast(
                species = pattern.species,
                activity = pattern.activity,
                confidencePercent = confidence,
                evidence = if (historical > 0) {
                    "$historical matching historical job${if (historical == 1) "" else "s"} plus seasonal behavior"
                } else {
                    "Seasonal behavior baseline; no matching local jobs recorded yet"
                },
                recommendedAction = pattern.action
            )
        }.sortedByDescending { it.confidencePercent }
    }

    private fun buildBusinessInsights(jobs: List<Job>, active: List<Job>): List<OperationsInsight> {
        val priced = jobs.filter { it.actualCost > 0.0 }
        val paired = jobs.filter { it.estimatedValue > 0.0 && it.actualCost > 0.0 }
        val averageTicket = priced.map { it.actualCost }.average().takeUnless { it.isNaN() } ?: 0.0
        val averageVariance = paired.map { (it.actualCost - it.estimatedValue) / it.estimatedValue * 100.0 }
            .average()
            .takeUnless { it.isNaN() }
            ?: 0.0
        val unpriced = active.count { it.estimatedValue <= 0.0 }
        val repeatProperties = jobs.filter { it.address.isNotBlank() }
            .groupingBy { normalizeAddress(it.address) }
            .eachCount()
            .count { it.value > 1 }

        return listOf(
            OperationsInsight(
                title = "Average recorded ticket",
                value = "$${"%.0f".format(averageTicket)}",
                detail = "Based on ${priced.size} job${if (priced.size == 1) "" else "s"} with actual amounts"
            ),
            OperationsInsight(
                title = "Estimate variance",
                value = "${if (averageVariance >= 0) "+" else ""}${"%.1f".format(averageVariance)}%",
                detail = if (paired.isEmpty()) "Record estimate and actual amounts to unlock this signal" else "Actual amount compared with estimate across ${paired.size} jobs",
                severity = if (abs(averageVariance) >= 20.0) "WARNING" else "INFO"
            ),
            OperationsInsight(
                title = "Active jobs missing estimates",
                value = unpriced.toString(),
                detail = if (unpriced == 0) "All active work has an estimate" else "Generate or enter estimates before scheduling work",
                severity = if (unpriced > 0) "WARNING" else "GOOD"
            ),
            OperationsInsight(
                title = "Repeat-service properties",
                value = repeatProperties.toString(),
                detail = "Properties with multiple job records; review for exclusion or warranty opportunities"
            )
        )
    }

    private fun buildInventoryForecasts(
        inventory: List<InventoryItem>,
        activeJobs: List<Job>
    ): List<InventoryForecast> {
        val activeText = activeJobs.joinToString(" ") { it.searchableText() }
        return inventory.filter { it.isActive }.mapNotNull { item ->
            val available = item.quantityAvailable
            val low = item.reorderLevel > 0.0 && available <= item.reorderLevel
            val itemTerms = "${item.name} ${item.category} ${item.description}".lowercase()
                .split(Regex("[^a-z0-9]+"))
                .filter { it.length >= 4 }
            val demandMatch = itemTerms.any { activeText.contains(it) }
            val demandTight = demandMatch && item.reorderLevel > 0.0 && available <= item.reorderLevel * 1.5
            if (!low && !demandTight) return@mapNotNull null

            val target = max(item.reorderLevel * 2.0, item.reorderQuantity)
            val order = max(0.0, target - available)
            InventoryForecast(
                itemId = item.id,
                name = item.name,
                available = available,
                recommendedOrder = order,
                reason = when {
                    low && demandMatch -> "Below reorder level and matches current job demand"
                    low -> "At or below configured reorder level"
                    else -> "Current job demand leaves a narrow stock buffer"
                }
            )
        }.sortedWith(compareByDescending<InventoryForecast> { it.recommendedOrder }.thenBy { it.name })
    }

    private fun buildSafetySignals(activeJobs: List<Job>): List<SafetySignal> {
        val rules = listOf(
            SafetyRule("Rabies-vector protocol", listOf("raccoon", "bat", "skunk", "fox"), "Verify vaccination/PPE, avoid direct handling, and document exposure response", "HIGH"),
            SafetyRule("Respiratory protection", listOf("guano", "dropping", "attic", "crawlspace", "cleanup"), "Use fitted respiratory protection and control contaminated dust", "HIGH"),
            SafetyRule("Fall protection", listOf("roof", "chimney", "soffit", "ladder"), "Confirm ladder setup, tie-off requirements, weather, and a ground spotter", "HIGH"),
            SafetyRule("Sting hazard", listOf("wasp", "hornet", "yellowjacket", "bee"), "Confirm allergy risk, escape route, suit condition, and treatment plan", "MEDIUM"),
            SafetyRule("Bite/envenomation precautions", listOf("snake", "bite"), "Keep capture tools ready and do not handle unidentified animals barehanded", "MEDIUM")
        )
        return rules.mapNotNull { rule ->
            val count = activeJobs.count { job ->
                val text = job.searchableText()
                rule.terms.any { text.contains(it) }
            }
            if (count == 0) null else SafetySignal(rule.title, count, rule.action, rule.severity)
        }
    }

    private fun Job.searchableText(): String = "$title $type $description $notes".lowercase()

    private fun JobStatus.isClosed(): Boolean =
        this == JobStatus.COMPLETED || this == JobStatus.CANCELLED || this == JobStatus.PAID

    private fun normalizeAddress(value: String): String = value.trim().lowercase().replace(Regex("\\s+"), " ")

    private fun riskLevel(score: Int): String = when {
        score >= 75 -> "CRITICAL"
        score >= 60 -> "HIGH"
        score >= 35 -> "MODERATE"
        else -> "LOW"
    }

    private data class SeasonalPattern(
        val species: String,
        val terms: List<String>,
        val activity: String,
        val action: String
    )

    private data class SafetyRule(
        val title: String,
        val terms: List<String>,
        val action: String,
        val severity: String
    )

    private val SAFETY_TERMS = listOf("raccoon", "bat", "skunk", "fox", "snake", "wasp", "hornet", "yellowjacket")
    private val DAMAGE_TERMS = listOf("damage", "hole", "entry", "chew", "guano", "dropping", "contamination", "attic", "roof", "soffit")
}
