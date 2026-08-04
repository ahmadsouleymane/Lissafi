package com.lissafi.app.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val barcode: String,
    val name: String,
    @SerialName("sell_price") val sellPrice: Int = 0,
    @SerialName("buy_price") val buyPrice: Int = 0,
    val stock: Int = 0,
    @SerialName("min_stock") val minStock: Int = 5,
    val category: String = "",
    @SerialName("has_barcode") val hasBarcode: Boolean = true,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @SerialName("user_id") val userId: String = ""
)
