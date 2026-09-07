package com.lissafi.app.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Clôture de caisse (« Z ») — offre Grand boutique. Enregistre, pour une caisse
 * (auteur = user_id) et une boutique (shop_id), le montant attendu en espèces
 * depuis la clôture précédente, le montant compté, et l'écart.
 * Montants en Int FCFA. Dates en Long epoch millis.
 */
@Serializable
data class CashClosure(
    val id: Long = 0,
    @SerialName("closed_at") val closedAt: Long,
    @SerialName("expected_total") val expectedTotal: Int,
    @SerialName("counted_total") val countedTotal: Int,
    val diff: Int,
    val note: String = "",
    @SerialName("user_id") val userId: String = "",
    // Vide au push → trigger serveur default_shop_id pose shop_id = user_id.
    @SerialName("shop_id") val shopId: String = ""
)
