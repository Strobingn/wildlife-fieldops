package com.strobingn.wildlifefieldops.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val jobId: String? = null,
    val employeeName: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val description: String = "",
    val amount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val expenseDate: Long = System.currentTimeMillis(),
    val receiptPhotoPath: String = "",
    val vendorName: String = "",
    val status: ExpenseStatus = ExpenseStatus.PENDING,
    val approvedBy: String = "",
    val approvedDate: Long? = null,
    val notes: String = "",
    val mileage: Double? = null,
    val vehicleId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
