package com.strobingn.wildlifefieldops.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "digital_contracts")
data class DigitalContract(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val contractNumber: String = "",
    val jobId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val serviceAddress: String = "",
    val title: String = "Wildlife Control Service Agreement",
    val scopeOfWork: String = "",
    val terms: String = DEFAULT_TERMS,
    val totalAmount: Double = 0.0,
    val status: ContractStatus = ContractStatus.DRAFT,
    val technicianName: String = "",
    val technicianSignaturePath: String = "",
    val technicianSignedAt: Long? = null,
    val customerSignerName: String = "",
    val customerSignaturePath: String = "",
    val customerSignedAt: Long? = null,
    val acceptedAt: Long? = null,
    val pdfPath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val syncError: String? = null
) {
    val isFullySigned: Boolean
        get() = technicianSignaturePath.isNotBlank() && customerSignaturePath.isNotBlank()

    companion object {
        const val DEFAULT_TERMS =
            "Customer authorizes Wildlife FieldOps to perform the scope of work described above. " +
                "Customer acknowledges that wildlife activity can recur when new entry points or environmental conditions arise. " +
                "Payment is due according to the agreed invoice terms."
    }
}

enum class ContractStatus {
    DRAFT,
    AWAITING_SIGNATURES,
    EXECUTED,
    VOID
}
