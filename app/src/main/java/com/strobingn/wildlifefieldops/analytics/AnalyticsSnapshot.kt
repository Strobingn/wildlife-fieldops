package com.strobingn.wildlifefieldops.analytics

/**
 * Live Phase 3 business metrics calculated from the local Room database.
 * No placeholder values are stored here.
 */
data class AnalyticsSnapshot(
    val totalJobs: Int = 0,
    val activeJobs: Int = 0,
    val completedJobs: Int = 0,
    val pendingJobs: Int = 0,
    val estimatedJobValue: Double = 0.0,
    val actualJobCost: Double = 0.0,
    val invoicedRevenue: Double = 0.0,
    val collectedRevenue: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val grossMargin: Double = 0.0,
    val grossMarginPercent: Double = 0.0,
    val unsyncedJobs: Int = 0,
    val unsyncedInvoices: Int = 0,
    val jobsByServiceType: Map<String, Int> = emptyMap()
)
