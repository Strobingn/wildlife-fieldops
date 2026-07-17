package com.strobingn.wildlife.data.remote

import com.strobingn.wildlife.data.model.Invoice
import com.strobingn.wildlife.data.model.InvoiceLineItem
import com.strobingn.wildlife.data.model.InvoiceStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class RemoteInvoiceDto(
    val id: String,
    @SerialName("invoice_number") val invoiceNumber: String,
    @SerialName("job_id") val jobId: String,
    @SerialName("customer_id") val customerId: String,
    @SerialName("customer_name") val customerName: String,
    @SerialName("customer_email") val customerEmail: String,
    @SerialName("customer_address") val customerAddress: String,
    @SerialName("issue_date") val issueDate: Long,
    @SerialName("due_date") val dueDate: Long,
    val status: String,
    val subtotal: Double,
    @SerialName("tax_rate") val taxRate: Double,
    @SerialName("tax_amount") val taxAmount: Double,
    @SerialName("discount_amount") val discountAmount: Double,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("amount_paid") val amountPaid: Double,
    @SerialName("balance_due") val balanceDue: Double,
    @SerialName("line_items") val lineItemsJson: String,
    val notes: String,
    val terms: String,
    @SerialName("technician_signature") val technicianSignature: String,
    @SerialName("customer_signature") val customerSignature: String,
    @SerialName("pdf_path") val pdfPath: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

private val json = Json { ignoreUnknownKeys = true }

fun Invoice.toRemoteDto(): RemoteInvoiceDto = RemoteInvoiceDto(
    id = id,
    invoiceNumber = invoiceNumber,
    jobId = jobId,
    customerId = customerId,
    customerName = customerName,
    customerEmail = customerEmail,
    customerAddress = customerAddress,
    issueDate = issueDate,
    dueDate = dueDate,
    status = status.name,
    subtotal = subtotal,
    taxRate = taxRate,
    taxAmount = taxAmount,
    discountAmount = discountAmount,
    totalAmount = totalAmount,
    amountPaid = amountPaid,
    balanceDue = balanceDue,
    lineItemsJson = json.encodeToString(ListSerializer(InvoiceLineItem.serializer()), lineItems),
    notes = notes,
    terms = terms,
    technicianSignature = technicianSignature,
    customerSignature = customerSignature,
    pdfPath = pdfPath,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun RemoteInvoiceDto.toLocal(): Invoice = Invoice(
    id = id,
    invoiceNumber = invoiceNumber,
    jobId = jobId,
    customerId = customerId,
    customerName = customerName,
    customerEmail = customerEmail,
    customerAddress = customerAddress,
    issueDate = issueDate,
    dueDate = dueDate,
    status = try { InvoiceStatus.valueOf(status) } catch (_: Exception) { InvoiceStatus.DRAFT },
    subtotal = subtotal,
    taxRate = taxRate,
    taxAmount = taxAmount,
    discountAmount = discountAmount,
    totalAmount = totalAmount,
    amountPaid = amountPaid,
    balanceDue = balanceDue,
    lineItems = try { json.decodeFromString(ListSerializer(InvoiceLineItem.serializer()), lineItemsJson) } catch (_: Exception) { emptyList() },
    notes = notes,
    terms = terms,
    technicianSignature = technicianSignature,
    customerSignature = customerSignature,
    pdfPath = pdfPath,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isSynced = true
)
