package com.strobingn.wildlifefieldops.analytics

import com.strobingn.wildlifefieldops.data.local.InvoiceDao
import com.strobingn.wildlifefieldops.data.local.JobDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository @Inject constructor(
    jobDao: JobDao,
    invoiceDao: InvoiceDao
) {
    val snapshot: Flow<AnalyticsSnapshot> = combine(
        jobDao.getAll(),
        invoiceDao.getAll()
    ) { jobs, invoices ->
        val completedJobs = jobs.count { it.status.name == "COMPLETED" }
        val pendingJobs = jobs.count { it.status.name == "PENDING" }
        val activeJobs = jobs.count {
            it.status.name != "COMPLETED" && it.status.name != "CANCELLED"
        }

        val estimatedJobValue = jobs.sumOf { it.estimatedValue }
        val actualJobCost = jobs.sumOf { it.actualCost }
        val invoicedRevenue = invoices.sumOf { it.totalAmount }
        val collectedRevenue = invoices.sumOf { it.amountPaid }
        val outstandingBalance = invoices.sumOf { it.balanceDue }
        val grossMargin = collectedRevenue - actualJobCost
        val grossMarginPercent = if (collectedRevenue > 0.0) {
            (grossMargin / collectedRevenue) * 100.0
        } else {
            0.0
        }

        AnalyticsSnapshot(
            totalJobs = jobs.size,
            activeJobs = activeJobs,
            completedJobs = completedJobs,
            pendingJobs = pendingJobs,
            estimatedJobValue = estimatedJobValue,
            actualJobCost = actualJobCost,
            invoicedRevenue = invoicedRevenue,
            collectedRevenue = collectedRevenue,
            outstandingBalance = outstandingBalance,
            grossMargin = grossMargin,
            grossMarginPercent = grossMarginPercent,
            unsyncedJobs = jobs.count { !it.isSynced },
            unsyncedInvoices = invoices.count { !it.isSynced },
            jobsByServiceType = jobs
                .groupingBy { it.type.ifBlank { "Unspecified" } }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
                .toMap()
        )
    }
}
