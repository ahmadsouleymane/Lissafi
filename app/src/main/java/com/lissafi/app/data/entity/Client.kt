package com.lissafi.app.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Client(
    val id: String,
    val name: String,
    val phone: String = "",
    @SerialName("total_debt") val totalDebt: Int = 0,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("user_id") val userId: String = "",
    // Boutique partagée (Grand boutique) : vide → omis au push, trigger serveur = user_id.
    @SerialName("shop_id") val shopId: String = ""
)
