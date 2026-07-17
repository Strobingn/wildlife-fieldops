package com.strobingn.wildlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String = "",
    val jobId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val customerAddress: String = "",
    val issueDate: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + 30 * 86400000L,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    val subtotal: Double = 0.0,
    val taxRate: Double = 0.0,
    val taxAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val balanceDue: Double = 0.0,
    val lineItems: List<InvoiceLineItem> = emptyList(),
    val notes: String = "",
    val terms: String = "Payment due within 30 days. Late payments subject to 1.5% monthly service charge.",
    val technicianSignature: String = "",
    val customerSignature: String = "",
    val pdfPath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val syncError: String? = null
)
