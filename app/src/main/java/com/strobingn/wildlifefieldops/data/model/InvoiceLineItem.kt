package com.strobingn.wildlife.data.model

import kotlinx.serialization.Serializable

@Serializable
data class InvoiceLineItem(
    val id: String = "",
    val description: String = "",
    val quantity: Double = 1.0,
    val unit: String = "ea",
    val unitPrice: Double = 0.0,
    val total: Double = 0.0
) {
    fun calculateTotal(): Double = quantity * unitPrice
}
