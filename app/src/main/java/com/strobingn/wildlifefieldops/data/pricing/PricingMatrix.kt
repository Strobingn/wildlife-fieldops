package com.strobingn.wildlifefieldopsfieldops.data.pricing

import com.strobingn.wildlifefieldopsfieldops.data.model.JobType

/**
 * Regional species-based pricing matrix for wildlife removal services.
 * Prices are in USD and represent base labor + material costs.
 * Final quotes should add markup, permits, and travel.
 */
object PricingMatrix {

    data class ServicePricing(
        val baseLaborHours: Double,
        val laborRate: Double = 85.0,
        val materialCost: Double = 0.0,
        val equipmentCost: Double = 0.0,
        val permitCost: Double = 0.0,
        val disposalCost: Double = 0.0,
        val minCharge: Double = 150.0,
        val warrantyMonths: Int = 0,
        val notes: String = ""
    ) {
        val baseLaborCost: Double get() = baseLaborHours * laborRate
        val subtotal: Double get() = baseLaborCost + materialCost + equipmentCost + permitCost + disposalCost
        val totalWithMin: Double get() = kotlin.math.max(subtotal, minCharge)
    }

    private val pricingMap = mapOf(
        // Inspections
        JobType.INSPECTION to ServicePricing(
            baseLaborHours = 1.5,
            materialCost = 0.0,
            minCharge = 150.0,
            notes = "Visual inspection, photo documentation, written report"
        ),
        JobType.CONSULTATION to ServicePricing(
            baseLaborHours = 1.0,
            materialCost = 0.0,
            minCharge = 100.0,
            notes = "Phone or on-site consultation"
        ),

        // Removal
        JobType.REMOVAL to ServicePricing(
            baseLaborHours = 3.0,
            materialCost = 45.0,
            equipmentCost = 25.0,
            minCharge = 250.0,
            warrantyMonths = 30,
            notes = "Live animal removal from structure"
        ),
        JobType.TRAPPING to ServicePricing(
            baseLaborHours = 4.0,
            materialCost = 85.0, // Traps, bait
            equipmentCost = 35.0,
            minCharge = 350.0,
            warrantyMonths = 14,
            notes = "Setup, monitoring, removal of traps"
        ),
        JobType.DEAD_ANIMAL_REMOVAL to ServicePricing(
            baseLaborHours = 2.0,
            materialCost = 25.0, // PPE, disinfectant
            disposalCost = 35.0,
            minCharge = 200.0,
            notes = "Location, extraction, sanitation"
        ),

        // Exclusion
        JobType.EXCLUSION to ServicePricing(
            baseLaborHours = 6.0,
            materialCost = 150.0, // Hardware cloth, screening
            equipmentCost = 50.0,
            minCharge = 500.0,
            warrantyMonths = 12,
            notes = "Seal entry points, one-way doors, structural repairs"
        ),
        JobType.ONE_WAY_DOOR to ServicePricing(
            baseLaborHours = 2.0,
            materialCost = 65.0,
            minCharge = 200.0,
            warrantyMonths = 6,
            notes = "Installation of one-way exit device"
        ),
        JobType.BAT_EXCLUSION to ServicePricing(
            baseLaborHours = 8.0,
            materialCost = 200.0,
            equipmentCost = 75.0,
            minCharge = 750.0,
            warrantyMonths = 24,
            notes = "Bat-specific exclusion, timing restrictions apply"
        ),
        JobType.BIRD_CONTROL to ServicePricing(
            baseLaborHours = 4.0,
            materialCost = 120.0,
            equipmentCost = 40.0,
            minCharge = 400.0,
            warrantyMonths = 12,
            notes = "Spikes, netting, deterrent installation"
        ),
        JobType.CHIMNEY_CAP to ServicePricing(
            baseLaborHours = 1.5,
            materialCost = 85.0,
            minCharge = 200.0,
            warrantyMonths = 60,
            notes = "Stainless steel chimney cap installation"
        ),

        // Species-specific
        JobType.SQUIRREL_REMOVAL to ServicePricing(
            baseLaborHours = 3.5,
            materialCost = 55.0,
            equipmentCost = 30.0,
            minCharge = 300.0,
            warrantyMonths = 12,
            notes = "Squirrel removal + entry point sealing"
        ),
        JobType.RACCOON_REMOVAL to ServicePricing(
            baseLaborHours = 4.0,
            materialCost = 75.0,
            equipmentCost = 40.0,
            minCharge = 350.0,
            warrantyMonths = 12,
            notes = "Raccoon removal + heavy-duty exclusion"
        ),
        JobType.SKUNK_REMOVAL to ServicePricing(
            baseLaborHours = 3.0,
            materialCost = 60.0,
            equipmentCost = 25.0,
            disposalCost = 45.0,
            minCharge = 300.0,
            warrantyMonths = 6,
            notes = "Skunk removal + odor treatment materials"
        ),
        JobType.SNAKE_REMOVAL to ServicePricing(
            baseLaborHours = 2.0,
            materialCost = 30.0,
            minCharge = 200.0,
            notes = "Snake removal and release"
        ),

        // Cleanup
        JobType.CLEANUP to ServicePricing(
            baseLaborHours = 5.0,
            materialCost = 120.0, // Disinfectant, PPE
            equipmentCost = 50.0,
            disposalCost = 75.0,
            minCharge = 400.0,
            notes = "Biohazard cleanup, deodorization"
        ),
        JobType.ATTIC_CLEANOUT to ServicePricing(
            baseLaborHours = 8.0,
            materialCost = 200.0,
            equipmentCost = 100.0,
            disposalCost = 150.0,
            minCharge = 800.0,
            notes = "Insulation removal, vacuum, sanitize"
        ),
        JobType.CRAWLSPACE_CLEANUP to ServicePricing(
            baseLaborHours = 6.0,
            materialCost = 150.0,
            equipmentCost = 75.0,
            disposalCost = 100.0,
            minCharge = 600.0,
            notes = "Crawlspace cleanup, vapor barrier check"
        ),
        JobType.SANITATION to ServicePricing(
            baseLaborHours = 3.0,
            materialCost = 80.0,
            equipmentCost = 30.0,
            minCharge = 300.0,
            notes = "Disinfection, enzyme treatment, odor control"
        ),
        JobType.INSULATION_REMEDIATION to ServicePricing(
            baseLaborHours = 10.0,
            materialCost = 400.0,
            equipmentCost = 150.0,
            disposalCost = 200.0,
            minCharge = 1200.0,
            warrantyMonths = 12,
            notes = "Insulation removal + new insulation install"
        ),

        // Repair
        JobType.REPAIR to ServicePricing(
            baseLaborHours = 3.0,
            materialCost = 100.0,
            minCharge = 300.0,
            warrantyMonths = 6,
            notes = "General structural repair after animal damage"
        ),
        JobType.PREVENTION to ServicePricing(
            baseLaborHours = 4.0,
            materialCost = 180.0,
            minCharge = 400.0,
            warrantyMonths = 12,
            notes = "Preventive exclusion, vent covers, gap sealing"
        ),

        // Follow-up
        JobType.FOLLOW_UP to ServicePricing(
            baseLaborHours = 1.0,
            materialCost = 0.0,
            minCharge = 75.0,
            notes = "Follow-up inspection and warranty check"
        ),
        JobType.EMERGENCY to ServicePricing(
            baseLaborHours = 3.0,
            materialCost = 50.0,
            equipmentCost = 25.0,
            minCharge = 350.0,
            notes = "After-hours emergency callout (add 25% surcharge)"
        ),

        JobType.OTHER to ServicePricing(
            baseLaborHours = 2.0,
            materialCost = 50.0,
            minCharge = 200.0,
            notes = "Custom service — estimate required"
        )
    )

    fun getPricing(jobType: JobType): ServicePricing {
        return pricingMap[jobType] ?: pricingMap[JobType.OTHER]!!
    }

    fun calculateEstimate(
        jobType: JobType,
        propertySize: PropertySize = PropertySize.MEDIUM,
        severity: Severity = Severity.MODERATE,
        travelMiles: Double = 0.0,
        taxRate: Double = 8.0
    ): EstimateBreakdown {
        val base = getPricing(jobType)

        val sizeMultiplier = when (propertySize) {
            PropertySize.SMALL -> 0.85
            PropertySize.MEDIUM -> 1.0
            PropertySize.LARGE -> 1.25
            PropertySize.COMMERCIAL -> 1.5
        }

        val severityMultiplier = when (severity) {
            Severity.MINIMAL -> 0.75
            Severity.LOW -> 0.9
            Severity.MODERATE -> 1.0
            Severity.HIGH -> 1.35
            Severity.SEVERE -> 1.75
        }

        val adjustedLaborHours = base.baseLaborHours * sizeMultiplier * severityMultiplier
        val adjustedLaborCost = adjustedLaborHours * base.laborRate
        val adjustedMaterialCost = base.materialCost * severityMultiplier
        val travelCost = travelMiles * 0.65

        val subtotal = adjustedLaborCost + adjustedMaterialCost + base.equipmentCost + base.permitCost + base.disposalCost + travelCost
        val total = kotlin.math.max(subtotal, base.minCharge)
        val tax = total * (taxRate / 100)
        val grandTotal = total + tax

        return EstimateBreakdown(
            laborHours = adjustedLaborHours,
            laborCost = adjustedLaborCost,
            materialCost = adjustedMaterialCost,
            equipmentCost = base.equipmentCost,
            permitCost = base.permitCost,
            disposalCost = base.disposalCost,
            travelCost = travelCost,
            subtotal = total,
            tax = tax,
            grandTotal = grandTotal,
            warrantyMonths = base.warrantyMonths,
            notes = base.notes
        )
    }

    enum class PropertySize { SMALL, MEDIUM, LARGE, COMMERCIAL }
    enum class Severity { MINIMAL, LOW, MODERATE, HIGH, SEVERE }

    data class EstimateBreakdown(
        val laborHours: Double,
        val laborCost: Double,
        val materialCost: Double,
        val equipmentCost: Double,
        val permitCost: Double,
        val disposalCost: Double,
        val travelCost: Double,
        val subtotal: Double,
        val tax: Double,
        val grandTotal: Double,
        val warrantyMonths: Int,
        val notes: String
    )
}
