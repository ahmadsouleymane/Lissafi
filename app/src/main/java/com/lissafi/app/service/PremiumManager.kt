package com.lissafi.app.service

import android.net.Uri
import com.lissafi.app.data.remote.SupabaseApi
import com.lissafi.app.data.repository.LissafiRepository

class PremiumManager(
    private val repository: LissafiRepository,
    private val api: SupabaseApi
) {

    companion object {
        const val DEMO_DAYS = 7
        const val PREMIUM_DAYS = 365

        // Limites gratuites (V1 : 10 produits / 10 clients) + plafond de ventes
        const val MAX_FREE_PRODUCTS = 10
        const val MAX_FREE_CREDITS = 10
        const val MAX_FREE_SALES_PER_DAY = 10

        // Limites du plan Lissafi Plus (plan de volume)
        const val MAX_PLUS_PRODUCTS = 100
        const val MAX_PLUS_CREDITS = 100

        // Page de paiement en ligne (landing statique, aucune clé côté client).
        private const val PAYMENT_URL = "https://lissafi-one.vercel.app/payer"

        /** Lien vers la page de paiement en ligne, plan et email pré-remplis. */
        fun buildActivationPaymentLink(plan: String, email: String?): String {
            val builder = Uri.parse(PAYMENT_URL).buildUpon().appendQueryParameter("plan", plan)
            if (!email.isNullOrBlank()) builder.appendQueryParameter("email", email)
            return builder.build().toString()
        }
    }

    /** Les trois niveaux : gratuits, plan de volume, plan haut de gamme. */
    enum class Plan { FREE, PLUS, BUSINESS }

    /** Plan effectif : FREE, PLUS ou BUSINESS selon l'abonnement et l'expiration. */
    suspend fun getPlan(): Plan {
        if (!isPremium()) return Plan.FREE
        return if (repository.getSetting("plan") == "business") Plan.BUSINESS else Plan.PLUS
    }

    suspend fun isPremium(): Boolean {
        val premium = repository.isPremium()
        if (!premium) return false

        val expiry = repository.getPremiumExpiry() ?: return false
        return if (System.currentTimeMillis() >= expiry) {
            // Expiré -> rétrograder
            repository.setSetting("is_premium", "false")
            false
        } else {
            true
        }
    }

    suspend fun activateDemo(): Boolean {
        val currentExpiry = repository.getPremiumExpiry() ?: 0
        // Ne pas réactiver si déjà premium ou si démo déjà utilisée
        if (currentExpiry > System.currentTimeMillis()) return false

        // Vérifier si la démo a déjà été utilisée
        val demoTaken = repository.getSetting("demo_taken") == "true"
        if (demoTaken) return false

        val newExpiry = System.currentTimeMillis() + DEMO_DAYS * 24 * 60 * 60 * 1000L
        repository.setSetting("is_premium", "true")
        repository.setSetting("plan", "plus")
        repository.setSetting("premium_expiry", newExpiry.toString())
        repository.setSetting("demo_taken", "true")
        return true
    }

    /**
     * Active Plus ou Business via un code VALIDÉ CÔTÉ SERVEUR
     * (`redeem_premium_code`). Les codes ne sont plus embarqués dans l'APK :
     * un code est à usage unique, lié au compte qui l'a saisi. Sans réseau ou
     * sans session, l'activation échoue (retour false).
     */
    suspend fun activateWithCode(code: String): Boolean {
        val trimmed = code.trim()
        if (trimmed.isBlank()) return false
        return try {
            val result = api.redeemPremiumCode(trimmed)
            if (!result.ok) return false

            // On répercute localement l'état posé par le serveur.
            repository.setSetting("is_premium", "true")
            repository.setSetting("plan", if (result.plan == "business") "business" else "plus")
            result.premium_expiry?.let { repository.setSetting("premium_expiry", it.toString()) }
            repository.setSetting("activation_code", trimmed)
            // La démo est consommée dès qu'un code est utilisé.
            repository.setSetting("demo_taken", "true")
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Libre selon le plan : FREE ≤10, PLUS ≤100, BUSINESS illimité. */
    suspend fun canAddProduct(): Boolean = when (getPlan()) {
        Plan.BUSINESS -> true
        Plan.PLUS -> repository.getProductCount() < MAX_PLUS_PRODUCTS
        Plan.FREE -> repository.getProductCount() < MAX_FREE_PRODUCTS
    }

    suspend fun canAddClient(): Boolean = when (getPlan()) {
        Plan.BUSINESS -> true
        Plan.PLUS -> repository.getClientCount() < MAX_PLUS_CREDITS
        Plan.FREE -> repository.getClientCount() < MAX_FREE_CREDITS
    }

    /** Limite gratuite : 10 ventes par jour. Plus et Business : ventes illimitées. */
    suspend fun canMakeSale(): Boolean {
        if (getPlan() != Plan.FREE) return true
        val (start, end) = FormatUtils.todayRange()
        return repository.countSalesBetween(start, end) < MAX_FREE_SALES_PER_DAY
    }
}
