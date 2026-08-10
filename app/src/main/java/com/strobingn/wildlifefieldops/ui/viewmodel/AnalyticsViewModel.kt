package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.local.InvoiceDao
import com.strobingn.wildlifefieldops.data.model.JobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AnalyticsSnapshot(
    val totalJobs: Int = 0,
    val completedJobs: Int = 0,
    val openJobs: Int = 0,
    val totalInvoices: Int = 0,
    val revenueEstimate: Double = 0.0,
    val jobsByType: Map<String, Int> = emptyMap()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    jobDao: JobDao,
    invoiceDao: InvoiceDao
) : ViewModel() {

    val snapshot = combine(jobDao.getAll(), invoiceDao.getAll()) { jobs, invoices ->
        val completed = jobs.count { it.status == JobStatus.COMPLETED || it.status == JobStatus.PAID }
        val open = jobs.count { it.status != JobStatus.COMPLETED && it.status != JobStatus.PAID && it.status != JobStatus.CANCELLED }
        val byType = jobs.groupingBy { it.type.ifBlank { "Other" } }.eachCount()
        val revenue = invoices.sumOf { it.totalAmount }

        AnalyticsSnapshot(
            totalJobs = jobs.size,
            completedJobs = completed,
            openJobs = open,
            totalInvoices = invoices.size,
            revenueEstimate = revenue,
            jobsByType = byType
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsSnapshot())
}
