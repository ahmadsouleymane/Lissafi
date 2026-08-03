package com.lissafi.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val barcode: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "sell_price") val sellPrice: Int,
    @ColumnInfo(name = "buy_price") val buyPrice: Int = 0,
    @ColumnInfo(name = "stock") val stock: Int = 0,
    @ColumnInfo(name = "min_stock") val minStock: Int = 5,
    @ColumnInfo(name = "category") val category: String = "",
    @ColumnInfo(name = "has_barcode") val hasBarcode: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
