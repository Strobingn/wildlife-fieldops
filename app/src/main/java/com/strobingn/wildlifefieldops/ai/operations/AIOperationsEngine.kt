package com.strobingn.wildlife.ai.operations

import com.strobingn.wildlife.data.model.Job
import com.strobingn.wildlife.data.model.JobStatus
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Deterministic intelligence layer that works offline from real app data.
 * Live LLM/vision services can enrich these results, but this engine never depends on them.
 */
object AIOperationsEngine {

    data class PropertyInsight(
        val address: String,
        val visitCount: Int,
        val serviceTypes: List<String>,
        val totalQuoted: Double,
        val totalActual: Double,
        val repeatRiskPercent: Int,
        val recommendation: String
    )

    data class BusinessInsight(
        val totalJobs: Int,
        val completedJobs: Int,
        val closeRatePercent: Int,
        val quotedRevenue: Double,
        val actualRevenue: Double,
        val grossVariance: Double,
        val averageTicket: Double,
        val topService: String,
        val recommendation: String
    )

    data class QualityCheck(
        val jobId: String,
        val title: String,
        val score: Int,
        val missing: List<String>
    )

    data class PricingInsight(
        val jobId: String,
        val title: String,
        val estimated: Double,
        val actual: Double,
        val variance: Double,
        val marginSignal: String
    )

    data class RoutePriority(
        val jobId: String,
        val title: String,
        val address: String,
        val score: Int,
        val reason: String
    )

    data class InventoryForecast(
        val item: String,
        val expectedWeeklyUse: Int,
        val confidencePercent: Int,
        val reason: String
    )

    data class SpeciesGuidance(
        val species: String,
        val activityWindow: String,
        val fieldPriority: String,
        val exclusionNote: String
    )

    data class Dashboard(
        val business: BusinessInsight,
        val properties: List<PropertyInsight>,
        val qualityChecks: List<QualityCheck>,
        val pricing: List<PricingInsight>,
        val routePriorities: List<RoutePriority>,
        val inventory: List<InventoryForecast>,
        val speciesGuidance: List<SpeciesGuidance>
    )

    fun analyze(jobs: List<Job>): Dashboard {
        return Dashboard(
            business = businessInsight(jobs),
            properties = propertyInsights(jobs),
            qualityChecks = jobs.map(::qualityCheck).sortedBy { it.score }.take(20),
            pricing = jobs.filter { it.estimatedValue > 0.0 || it.actualCost > 0.0 }
                .map(::pricingInsight)
                .sortedByDescending { kotlin.math.abs(it.variance) }
                .take(20),
            routePriorities = jobs.filter { it.status != JobStatus.COMPLETED && it.status != JobStatus.PAID }
                .map(::routePriority)
                .sortedByDescending { it.score }
                .take(20),
            inventory = inventoryForecast(jobs),
            speciesGuidance = speciesGuidance(jobs)
        )
    }

    private fun businessInsight(jobs: List<Job>): BusinessInsight {
        val completed = jobs.count { it.status == JobStatus.COMPLETED || it.status == JobStatus.PAID }
        val quoted = jobs.sumOf { it.estimatedValue }
        val actual = jobs.sumOf { it.actualCost }
        val averageTicket = if (jobs.isEmpty()) 0.0 else quoted / jobs.size
        val topService = jobs.groupingBy { it.type.ifBlank { "Unknown" } }.eachCount()
            .maxByOrNull { it.value }?.key ?: "No data"
        val closeRate = if (jobs.isEmpty()) 0 else ((completed.toDouble() / jobs.size) * 100).toInt()
        val recommendation = when {
            jobs.isEmpty() -> "Add completed jobs to unlock forecasting."
            actual == 0.0 -> "Record actual cost on completed jobs to measure profit and estimate accuracy."
            quoted < actual -> "Actual costs exceed quoted revenue. Review labor, material, and travel assumptions."
            closeRate < 50 -> "Closing rate is below 50%. Review estimate speed, follow-up timing, and price clarity."
            else -> "Operations are stable. Focus on repeat-property prevention and top-service margins."
        }
        return BusinessInsight(
            totalJobs = jobs.size,
            completedJobs = completed,
            closeRatePercent = closeRate,
            quotedRevenue = quoted,
            actualRevenue = actual,
            grossVariance = quoted - actual,
            averageTicket = averageTicket,
            topService = topService,
            recommendation = recommendation
        )
    }

    private fun propertyInsights(jobs: List<Job>): List<PropertyInsight> {
        return jobs.filter { it.address.isNotBlank() }
            .groupBy { normalizeAddress(it.address) }
            .map { (_, propertyJobs) ->
                val address = propertyJobs.first().address
                val serviceTypes = propertyJobs.map { it.type }.filter { it.isNotBlank() }.distinct()
                val repeatRisk = min(95, 20 + ((propertyJobs.size - 1).coerceAtLeast(0) * 18) +
                    if (serviceTypes.size > 1) 10 else 0)
                val recommendation = when {
                    propertyJobs.size >= 3 -> "High repeat history. Inspect prior repair zones and building-wide exclusion points first."
                    propertyJobs.size == 2 -> "Repeat property. Compare current evidence with the previous entry point and warranty scope."
                    else -> "First recorded visit. Capture exterior elevations, attic/crawlspace findings, and repair photos."
                }
                PropertyInsight(
                    address = address,
                    visitCount = propertyJobs.size,
                    serviceTypes = serviceTypes,
                    totalQuoted = propertyJobs.sumOf { it.estimatedValue },
                    totalActual = propertyJobs.sumOf { it.actualCost },
                    repeatRiskPercent = repeatRisk,
                    recommendation = recommendation
                )
            }
            .sortedWith(compareByDescending<PropertyInsight> { it.repeatRiskPercent }.thenByDescending { it.visitCount })
            .take(30)
    }

    private fun qualityCheck(job: Job): QualityCheck {
        val missing = buildList {
            if (job.address.isBlank()) add("address")
            if (job.customerName.isBlank()) add("customer")
            if (job.description.isBlank()) add("description")
            if (job.estimatedValue <= 0.0) add("estimate")
            if ((job.status == JobStatus.COMPLETED || job.status == JobStatus.PAID) && job.actualCost <= 0.0) add("actual cost")
            if ((job.status == JobStatus.COMPLETED || job.status == JobStatus.PAID) && job.photos.isEmpty()) add("completion photos")
            if (job.type.isBlank()) add("service type")
        }
        return QualityCheck(
            jobId = job.id,
            title = job.title.ifBlank { "Untitled job" },
            score = max(0, 100 - missing.size * 14),
            missing = missing
        )
    }

    private fun pricingInsight(job: Job): PricingInsight {
        val variance = job.estimatedValue - job.actualCost
        val signal = when {
            job.actualCost <= 0.0 -> "Actual cost missing"
            variance < 0.0 -> "Over cost by ${money(-variance)}"
            job.estimatedValue > 0.0 && variance / job.estimatedValue < 0.15 -> "Thin margin"
            else -> "Healthy spread"
        }
        return PricingInsight(job.id, job.title.ifBlank { "Untitled job" }, job.estimatedValue, job.actualCost, variance, signal)
    }

    private fun routePriority(job: Job): RoutePriority {
        var score = when (job.priority.name) {
            "URGENT" -> 100
            "HIGH" -> 80
            "MEDIUM" -> 55
            else -> 35
        }
        val now = System.currentTimeMillis()
        val scheduled = job.scheduledDate
        if (scheduled != null) {
            val hours = (scheduled - now) / 3_600_000.0
            score += when {
                hours < -1 -> 35
                hours <= 4 -> 25
                hours <= 24 -> 15
                else -> 0
            }
        }
        if (job.address.isBlank()) score -= 20
        if (job.estimatedValue >= 1000.0) score += 10
        val reason = when {
            scheduled != null && scheduled < now -> "Overdue appointment"
            job.priority.name == "URGENT" -> "Urgent priority"
            job.estimatedValue >= 1000.0 -> "High-value open job"
            else -> "Priority and schedule score"
        }
        return RoutePriority(job.id, job.title.ifBlank { "Untitled job" }, job.address, score.coerceIn(0, 150), reason)
    }

    private fun inventoryForecast(jobs: List<Job>): List<InventoryForecast> {
        val recent = jobs.filter { it.createdAt >= System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000 }
        val services = recent.map { it.type.lowercase(Locale.US) }
        fun count(vararg words: String) = services.count { value -> words.any(value::contains) }
        val forecasts = listOf(
            InventoryForecast("One-way doors", max(1, count("bat", "squirrel", "raccoon", "one-way")), 78, "Based on exclusion/removal job mix"),
            InventoryForecast("Hardware cloth / screening", max(1, count("exclusion", "repair", "bird", "crawlspace")), 82, "Based on repair and exclusion volume"),
            InventoryForecast("Sealant / flashing", max(1, count("repair", "chimney", "roof", "bat")), 75, "Based on structural repair demand"),
            InventoryForecast("Traps", max(1, count("trapping", "raccoon", "skunk", "woodchuck")), 72, "Based on trapping-type jobs"),
            InventoryForecast("Sanitation product", max(1, count("cleanup", "sanitation", "dead animal", "attic")), 77, "Based on remediation and cleanup jobs")
        )
        return forecasts.sortedByDescending { it.expectedWeeklyUse }
    }

    private fun speciesGuidance(jobs: List<Job>): List<SpeciesGuidance> {
        val text = jobs.joinToString(" ") { "${it.title} ${it.description} ${it.notes} ${it.type}" }.lowercase(Locale.US)
        val known = listOf(
            SpeciesGuidance("Bat", "Dusk through dawn", "Inspect roofline, ridge vents, gable vents, fascia transitions", "Confirm seasonal/legal timing before exclusion; seal secondary gaps first."),
            SpeciesGuidance("Raccoon", "Dusk through early morning", "Inspect soffits, roof returns, chimneys, decks and crawlspaces", "Verify dependent young before eviction or exclusion."),
            SpeciesGuidance("Squirrel", "Morning and late afternoon", "Inspect roof edges, dormers, vents, trees contacting structure", "Locate all travel routes before installing one-way devices."),
            SpeciesGuidance("Skunk", "Dusk through night", "Inspect decks, sheds, crawlspaces and foundation voids", "Use low-stress exclusion and verify den status before sealing."),
            SpeciesGuidance("Woodchuck", "Morning and late afternoon", "Inspect burrow network, sheds, decks, gardens and foundation edges", "Account for secondary burrow exits and structural undermining."),
            SpeciesGuidance("Bird", "Dawn through daylight", "Inspect vents, soffits, signs, ledges and roof cavities", "Confirm nest/egg status and applicable protected-species rules."),
            SpeciesGuidance("Snake", "Warm daylight and dusk", "Inspect foundation gaps, clutter, rodents and moisture sources", "Correct prey and entry conditions; do not rely on repellents alone.")
        )
        return known.filter { text.contains(it.species.lowercase(Locale.US)) }.ifEmpty { known.take(3) }
    }

    private fun normalizeAddress(address: String): String = address.trim().lowercase(Locale.US).replace(Regex("\\s+"), " ")
    private fun money(value: Double): String = "$" + String.format(Locale.US, "%,.2f", value)
}
