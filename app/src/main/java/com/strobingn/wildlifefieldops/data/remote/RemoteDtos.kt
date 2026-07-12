package com.strobingn.wildlifefieldops.data.remote

import com.strobingn.wildlifefieldops.data.model.Customer
import com.strobingn.wildlifefieldops.data.model.Inspection
import com.strobingn.wildlifefieldops.data.model.Job
import com.strobingn.wildlifefieldops.data.model.JobPriority
import com.strobingn.wildlifefieldops.data.model.JobStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

/**
 * DTOs match the LIVE Supabase project `wildlife_app` (hgdzmwfcghtilyqagjak).
 * Jobs table uses customer_name/title/species (NOT NULL), not the idealized schema.sql only.
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
    /** Live DB required column */
    @SerialName("customer_name") val customerName: String,
    /** Compatibility column used by older web clients */
    val customer: String? = null,
    /** Live DB required column */
    val title: String,
    /** Live DB required column */
    val species: String = "Wildlife",
    @SerialName("customer_id") val customerId: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val town: String? = null,
    val state: String? = null,
    val zip: String? = null,
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
    @SerialName("job_id") val jobId: String? = null,
    @SerialName("inspection_type") val inspectionType: String? = "ROUTINE",
    val notes: String? = null,
    val findings: JsonObject = buildJsonObject { },
    @SerialName("customer_name") val customerName: String? = null,
    val species: String? = null,
    val status: String? = null,
    val priority: String? = null
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

fun Job.toRemoteDto(): RemoteJobDto {
    val name = customerName.ifBlank { title.ifBlank { "Customer" } }
    val jobTitle = title.ifBlank { name }
    val speciesGuess = description.ifBlank { "Wildlife" }
    return RemoteJobDto(
        id = id.ifBlank { UUID.randomUUID().toString() },
        customerName = name,
        customer = name,
        title = jobTitle,
        species = speciesGuess,
        customerId = customerId.takeIf { it.isNotBlank() && isUuid(it) },
        phone = null,
        email = null,
        address = address.ifBlank { null },
        status = status.toRemoteStatus(),
        priority = priority.toRemotePriority(),
        assignedTech = assignedTo.ifBlank { null },
        notes = notes.ifBlank { null },
        scope = description.ifBlank { null },
        latitude = latitude?.toString(),
        longitude = longitude?.toString(),
        estimate = estimatedValue,
        grandTotal = actualCost,
        scheduledStart = scheduledDate?.let { Instant.ofEpochMilli(it).toString() },
        completedAt = completedDate?.let { Instant.ofEpochMilli(it).toString() }
    )
}

fun RemoteJobDto.toLocal(): Job {
    val displayCustomer = customerName.ifBlank { customer.orEmpty() }
    return Job(
        id = id,
        title = title.ifBlank { displayCustomer },
        description = species.ifBlank { scope.orEmpty() },
        customerId = customerId.orEmpty(),
        customerName = displayCustomer,
        address = address.orEmpty(),
        latitude = latitude?.toDoubleOrNull(),
        longitude = longitude?.toDoubleOrNull(),
        status = status.fromRemoteStatus(),
        priority = priority.fromRemotePriority(),
        type = "Inspection",
        estimatedValue = estimate ?: 0.0,
        actualCost = grandTotal ?: 0.0,
        assignedTo = assignedTech.orEmpty(),
        notes = notes.orEmpty(),
        scheduledDate = scheduledStart?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
        completedDate = completedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
        isSynced = true
    )
}

/** Prefer linking to a real job UUID; otherwise store as standalone inspection row. */
fun Inspection.toRemoteDtoOrNull(): RemoteInspectionDto {
    val findingsJson = buildJsonObject {
        put("text", findings)
        put("recommendations", recommendations)
        put("species", speciesIdentified)
        put("entry_points", entryPoints)
        put("severity", severity.name)
        put("customer", customerName)
        put("inspector", inspectorName)
        put("weather", weatherConditions)
        put("damage", damageAssessment)
    }
    return RemoteInspectionDto(
        id = id.ifBlank { UUID.randomUUID().toString() },
        jobId = jobId.takeIf { it.isNotBlank() && isUuid(it) },
        inspectionType = inspectionType.name,
        notes = notes.ifBlank { null },
        findings = findingsJson,
        customerName = customerName.ifBlank { null },
        species = speciesIdentified.ifBlank { null },
        status = if (followUpRequired) "follow_up" else "completed",
        priority = severity.name.lowercase()
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
