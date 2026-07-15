package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.local.InvoiceDao
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val invoiceDao: InvoiceDao,
    private val jobDao: JobDao
) : ViewModel() {

    val invoices = invoiceDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getInvoiceById(id: String): Flow<Invoice?> = flow {
        emit(invoiceDao.getById(id))
    }

    fun getInvoicesByJob(jobId: String): Flow<List<Invoice>> = invoiceDao.getByJob(jobId)

    fun saveInvoice(invoice: Invoice) = viewModelScope.launch {
        val normalizedItems = invoice.lineItems.map { item ->
            item.copy(total = item.calculateTotal())
        }
        val subtotal = normalizedItems.sumOf { it.total }
        val taxAmount = subtotal * (invoice.taxRate / 100.0)
        val totalAmount = (subtotal + taxAmount - invoice.discountAmount).coerceAtLeast(0.0)
        val balanceDue = (totalAmount - invoice.amountPaid).coerceAtLeast(0.0)

        invoiceDao.insert(
            invoice.copy(
                lineItems = normalizedItems,
                subtotal = subtotal,
                taxAmount = taxAmount,
                totalAmount = totalAmount,
                balanceDue = balanceDue,
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
                syncError = null
            )
        )
    }

    fun deleteInvoice(invoice: Invoice) = viewModelScope.launch {
        invoiceDao.delete(invoice)
    }

    fun generateInvoiceFromJob(
        jobId: String,
        lineItems: List<InvoiceLineItem>,
        taxRate: Double,
        discountAmount: Double,
        notes: String,
        terms: String
    ) = viewModelScope.launch {
        val job = jobDao.getById(jobId) ?: return@launch
        val normalizedItems = lineItems.map { item -> item.copy(total = item.calculateTotal()) }
        val subtotal = normalizedItems.sumOf { it.total }
        val taxAmount = subtotal * (taxRate / 100.0)
        val total = (subtotal + taxAmount - discountAmount).coerceAtLeast(0.0)

        invoiceDao.insert(
            Invoice(
                invoiceNumber = generateInvoiceNumber(),
                jobId = jobId,
                customerId = job.customerId,
                customerName = job.customerName,
                subtotal = subtotal,
                taxRate = taxRate,
                taxAmount = taxAmount,
                discountAmount = discountAmount,
                totalAmount = total,
                balanceDue = total,
                lineItems = normalizedItems,
                notes = notes,
                terms = terms,
                isSynced = false
            )
        )
        jobDao.update(job.copy(status = JobStatus.INVOICED, isSynced = false, updatedAt = System.currentTimeMillis()))
    }

    fun markAsPaid(invoiceId: String) = viewModelScope.launch {
        val invoice = invoiceDao.getById(invoiceId) ?: return@launch
        invoiceDao.update(
            invoice.copy(
                status = InvoiceStatus.PAID,
                amountPaid = invoice.totalAmount,
                balanceDue = 0.0,
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
                syncError = null
            )
        )
        val job = jobDao.getById(invoice.jobId)
        job?.let {
            jobDao.update(it.copy(status = JobStatus.PAID, isSynced = false, updatedAt = System.currentTimeMillis()))
        }
    }

    private fun generateInvoiceNumber(): String = "INV-${System.currentTimeMillis()}"
}