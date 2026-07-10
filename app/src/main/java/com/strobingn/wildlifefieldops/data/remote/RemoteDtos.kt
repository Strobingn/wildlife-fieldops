package com.strobingn.wildlifefieldops.data.remote

import com.strobingn.wildlifefieldops.data.model.Customer
import com.strobingn.wildlifefieldops.data.model.Inspection
import com.strobingn.wildlifefieldops.data.model.Job
import com.strobingn.wildlifefieldops.data.model.JobPriority
import com.strobingn.wildlifefieldops.data.model.JobStatus
import com.strobingn.wildlifefieldops.data.model.JobType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

/**
 * Supabase (PostgREST) DTOs mapped to the public schema in supabase/schema.sql.
 * Native Room models stay local-first; these shapes are only for cloud sync.
 */
@Serializable
data class RemoteCustomerDto(
    val id: String,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val town: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val notes: String? = null
)

@Serializable
data class RemoteJobDto(
    val id: String,
    val customer: String,
    @SerialName("customer_id") val customerId: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String,
    val town: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val species: String? = null,
    val status: String = "Active",
    val priority: String? = "Normal",
    @SerialName("assigned_tech") val assignedTech: String? = null,
    val notes: String? = null,
    val scope: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val estimate: Double? = 0.0,
    @SerialName("grand_total") val grandTotal: Double? = 0.0,
    @SerialName("scheduled_start") val scheduledStart: String? = null,
    @SerialName("completed_at") val completedAt: String? = null
)

@Serializable
data class RemoteInspectionDto(
    val id: String,
    @SerialName("job_id") val jobId: String,
    @SerialName("inspection_type") val inspectionType: String = "ROUTINE",
    val notes: String? = null,
    val findings: JsonObject = buildJsonObject { }
)

@Serializable
data class AiEdgeRequest(
    val mode: String = "field_plan",
    val observation: String = "",
    val species: String = "",
    val businessContext: String = "Wildlife Whisperer LLC — native FieldOps Android app"
)

fun Customer.toRemoteDto(): RemoteCustomerDto = RemoteCustomerDto(
    id = id.ifBlank { UUID.randomUUID().toString() },
    name = fullName.trim().ifBlank { "Customer" },
    phone = phone.ifBlank { null },
    email = email.ifBlank { null },
    address = address.ifBlank { null },
    town = city.ifBlank { null },
    state = state.ifBlank { null },
    zip = zipCode.ifBlank { null },
    notes = notes.ifBlank { null }
)

fun RemoteCustomerDto.toLocal(): Customer {
    val parts = name.trim().split(" ", limit = 2)
    return Customer(
        id = id,
        firstName = parts.getOrNull(0).orEmpty(),
        lastName = parts.getOrNull(1).orEmpty(),
        email = email.orEmpty(),
        phone = phone.orEmpty(),
        address = address.orEmpty(),
        city = town.orEmpty(),
        state = state.orEmpty(),
        zipCode = zip.orEmpty(),
        notes = notes.orEmpty(),
        isSynced = true
    )
}

fun Job.toRemoteDto(): RemoteJobDto = RemoteJobDto(
    id = id.ifBlank { UUID.randomUUID().toString() },
    customer = customerName.ifBlank { title.ifBlank { "Job" } },
    customerId = customerId.takeIf { it.isNotBlank() && isUuid(it) },
    address = address.ifBlank { "Address TBD" },
    species = description.takeIf { it.isNotBlank() },
    status = status.toRemoteStatus(),
    priority = priority.toRemotePriority(),
    assignedTech = assignedTo.ifBlank { null },
    notes = notes.ifBlank { null },
    scope = title.ifBlank { null },
    latitude = latitude?.toString(),
    longitude = longitude?.toString(),
    estimate = estimatedValue,
    grandTotal = actualCost,
    scheduledStart = scheduledDate?.let { Instant.ofEpochMilli(it).toString() },
    completedAt = completedDate?.let { Instant.ofEpochMilli(it).toString() }
)

fun RemoteJobDto.toLocal(): Job = Job(
    id = id,
    title = scope ?: customer,
    description = species.orEmpty(),
    customerId = customerId.orEmpty(),
    customerName = customer,
    address = address,
    latitude = latitude?.toDoubleOrNull(),
    longitude = longitude?.toDoubleOrNull(),
    status = status.fromRemoteStatus(),
    priority = priority.fromRemotePriority(),
    type = JobType.INSPECTION,
    estimatedValue = estimate ?: 0.0,
    actualCost = grandTotal ?: 0.0,
    assignedTo = assignedTech.orEmpty(),
    notes = notes.orEmpty(),
    scheduledDate = scheduledStart?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
    completedDate = completedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
    isSynced = true
)

/** Returns null when job_id is missing (remote schema requires a real job UUID). */
fun Inspection.toRemoteDtoOrNull(): RemoteInspectionDto? {
    if (jobId.isBlank() || !isUuid(jobId)) return null
    val findingsJson = buildJsonObject {
        put("text", findings)
        put("recommendations", recommendations)
        put("species", speciesIdentified)
        put("entry_points", entryPoints)
        put("severity", severity.name)
        put("customer", customerName)
        put("inspector", inspectorName)
        put("weather", weatherConditions)
    }
    return RemoteInspectionDto(
        id = id.ifBlank { UUID.randomUUID().toString() },
        jobId = jobId,
        inspectionType = inspectionType.name,
        notes = notes.ifBlank { null },
        findings = findingsJson
    )
}

private fun isUuid(value: String): Boolean =
    runCatching { UUID.fromString(value); true }.getOrDefault(false)

private fun JobStatus.toRemoteStatus(): String = when (this) {
    JobStatus.PENDING -> "Active"
    JobStatus.IN_PROGRESS -> "In Progress"
    JobStatus.COMPLETED -> "Closed"
    JobStatus.CANCELLED -> "Cancelled"
    JobStatus.INVOICED -> "Closed"
    JobStatus.PAID -> "Closed"
}

private fun String?.fromRemoteStatus(): JobStatus = when (this?.lowercase()) {
    "active", "scheduled", "needs follow-up" -> JobStatus.PENDING
    "in progress" -> JobStatus.IN_PROGRESS
    "closed" -> JobStatus.COMPLETED
    "cancelled" -> JobStatus.CANCELLED
    else -> JobStatus.PENDING
}

private fun JobPriority.toRemotePriority(): String = when (this) {
    JobPriority.LOW -> "Low"
    JobPriority.MEDIUM -> "Normal"
    JobPriority.HIGH -> "High"
    JobPriority.URGENT -> "Critical"
}

private fun String?.fromRemotePriority(): JobPriority = when (this?.lowercase()) {
    "low" -> JobPriority.LOW
    "high" -> JobPriority.HIGH
    "critical" -> JobPriority.URGENT
    else -> JobPriority.MEDIUM
}
