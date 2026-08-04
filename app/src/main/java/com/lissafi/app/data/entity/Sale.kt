package com.lissafi.app.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Sale(
    val id: Long = 0,
    val date: Long,
    val total: Int,
    @SerialName("amount_paid") val amountPaid: Int = 0,
    @SerialName("change_given") val changeGiven: Int = 0,
    @SerialName("is_credit") val isCredit: Boolean = false,
    @SerialName("client_id") val clientId: String? = null,
    val synced: Boolean = false,
    @SerialName("user_id") val userId: String = ""
)
