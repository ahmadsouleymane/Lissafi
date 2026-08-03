package com.lissafi.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "total") val total: Int,
    @ColumnInfo(name = "amount_paid") val amountPaid: Int = 0,
    @ColumnInfo(name = "change_given") val changeGiven: Int = 0,
    @ColumnInfo(name = "is_credit") val isCredit: Boolean = false,
    @ColumnInfo(name = "client_id") val clientId: String? = null,
    @ColumnInfo(name = "synced") val synced: Boolean = false
)
