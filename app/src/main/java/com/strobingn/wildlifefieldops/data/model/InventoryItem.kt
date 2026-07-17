package com.strobingn.wildlifefieldops.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sku: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val unitOfMeasure: String = "each",
    val quantityOnHand: Double = 0.0,
    val quantityReserved: Double = 0.0,
    val reorderLevel: Double = 0.0,
    val reorderQuantity: Double = 0.0,
    val unitCost: Double = 0.0,
    val unitPrice: Double = 0.0,
    val supplierName: String = "",
    val supplierSku: String = "",
    val location: String = "",
    val isActive: Boolean = true,
    val lastRestockDate: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) {
    val quantityAvailable: Double
        get() = quantityOnHand - quantityReserved

    val isLowStock: Boolean
        get() = quantityOnHand <= reorderLevel && reorderLevel > 0
}
