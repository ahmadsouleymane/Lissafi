package com.lissafi.app.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SaleItem(
    val id: Long = 0,
    @SerialName("sale_id") val saleId: Long,
    val barcode: String,
    val name: String,
    val price: Int,
    val quantity: Double = 1.0,
    @SerialName("user_id") val userId: String = ""
)
