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

    fun getInvoicesByJob(jobId: String): Flow<List<Invoice>> =
        invoiceDao.getByJob(jobId)

    fun saveInvoice(invoice: Invoice) = viewModelScope.launch {
        // Recalculate totals before saving to ensure consistency
        val subtotal = invoice.lineItems.sumOf { it.calculateTotal() }
        val taxAmount = subtotal * (invoice.taxRate / 100.0)
        val total = subtotal + taxAmount - invoice.discountAmount
        val updated = invoice.copy(
            subtotal = subtotal,
            taxAmount = taxAmount,
            totalAmount = total,
            balanceDue = total - invoice.amountPaid,
            updatedAt = System.currentTimeMillis(),
            isSynced = false  // Mark as needing sync after edit
        )
        invoiceDao.insert(updated)
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
        val job = jobDao.getById(jobId)
        job?.let {
            val subtotal = lineItems.sumOf { it.calculateTotal() }
            val taxAmount = subtotal * (taxRate / 100.0)
            val total = subtotal + taxAmount - discountAmount

            val invoice = Invoice(
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
                lineItems = lineItems,
                notes = notes,
                terms = terms
            )
            invoiceDao.insert(invoice)
            jobDao.update(job.copy(status = JobStatus.INVOICED))
        }
    }

    fun markAsPaid(invoiceId: String) = viewModelScope.launch {
        val invoice = invoiceDao.getById(invoiceId)
        invoice?.let {
            invoiceDao.update(it.copy(
                status = InvoiceStatus.PAID,
                amountPaid = it.totalAmount,
                balanceDue = 0.0,
                updatedAt = System.currentTimeMillis()
            ))
            val job = jobDao.getById(it.jobId)
            job?.let { j ->
                jobDao.update(j.copy(status = JobStatus.PAID))
            }
        }
    }

    private fun generateInvoiceNumber(): String {
        return "INV-${System.currentTimeMillis()}"
    }
}
