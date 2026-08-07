package com.lissafi.app.data.remote

import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Recherche des informations produit à partir d'un code-barres (EAN/UPC).
 *
 * Utilise l'API publique Open Food Facts (gratuite, sans clé) :
 * `https://world.openfoodfacts.org/api/v2/product/{barcode}.json`.
 * Retourne null si le code n'est pas trouvé (repli sur la saisie manuelle).
 */
object ProductLookupService {

    @Serializable
    private data class OffResponse(
        val status: Int = 0,
        val product: OffProduct? = null
    )

    @Serializable
    private data class OffProduct(
        @SerialName("product_name") val productName: String = "",
        @SerialName("brands") val brands: String = "",
        @SerialName("categories") val categories: String = "",
        @SerialName("quantity") val quantity: String = ""
    )

    data class LookupResult(
        val name: String = "",
        val category: String = "",
        val brand: String = ""
    )

    /**
     * Tente de récupérer un produit pour un code-barres donné.
     * Retourne null si le code n'est pas trouvable (ou en cas d'échec réseau),
     * afin que l'utilisateur puisse saisir les infos manuellement.
     */
    suspend fun lookup(barcode: String): LookupResult? = withContext(Dispatchers.IO) {
        // Les codes EAN/UPC sont numériques ; un code non numérique n'est pas recherchable.
        if (barcode.isBlank() || !barcode.all { it.isDigit() }) return@withContext null
        try {
            val client = SupabaseManager.getHttpClient()
            val response = client.get("https://world.openfoodfacts.org/api/v2/product/${barcode.trim()}.json") {
                timeout { requestTimeoutMillis = 8_000 }
            }
            if (response.status.value != 200) return@withContext null
            val body = response.body<OffResponse>()
            if (body.status != 1 || body.product == null) return@withContext null
            val p = body.product
            LookupResult(
                name = p.productName.takeIf { it.isNotBlank() } ?: "",
                category = p.categories.split(",").firstOrNull()?.trim().orEmpty(),
                brand = p.brands.takeIf { it.isNotBlank() } ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}
