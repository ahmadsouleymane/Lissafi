package com.lissafi.app.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DebtTransaction(
    val id: Long = 0,
    @SerialName("client_id") val clientId: String,
    @SerialName("sale_id") val saleId: Long? = null,
    val amount: Int,
    val date: Long,
    val note: String = "",
    @SerialName("user_id") val userId: String = "",
    // Boutique partagée (Grand boutique) : vide → omis au push, trigger serveur = user_id.
    @SerialName("shop_id") val shopId: String = ""
)
