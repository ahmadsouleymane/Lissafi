package com.lissafi.app.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Trace inaltérable d'une opération sur une vente (création, modification,
 * annulation). Journal APPEND-ONLY : on n'efface JAMAIS une entrée — c'est la
 * garantie anti-fraude, y compris côté Supabase (une vente ne peut pas être
 * dissimulée sans laisser de trace horodatée synchronisée dans le cloud).
 *
 * `action` : "created" | "modified" | "cancelled".
 * `details` : description lisible du changement (ex. « Total : 5 000 → 4 500 FCFA »).
 */
@Serializable
data class SaleAuditEntry(
    val id: Long = 0,
    @SerialName("sale_id") val saleId: Long,
    val action: String,
    val details: String = "",
    val date: Long,
    @SerialName("user_id") val userId: String = ""
)
