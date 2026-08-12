package com.lissafi.app.service

import android.net.Uri
import com.lissafi.app.data.repository.LissafiRepository

class PremiumManager(private val repository: LissafiRepository) {

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

        // ⚠️ AVANT DE DISTRIBUER L'APK : remplace `227XXXXXXXX` par le numéro
        // de contact pour l'activation (format international, sans espaces ni +).
        const val SUPPORT_WHATSAPP_NUMBER = "227XXXXXXXX"

        /** Message pré-rempli envoyé sur WhatsApp pour demander l'activation. */
        fun buildActivationMessage(shopName: String, email: String?, userId: String?): String {
            val lines = mutableListOf("Bonjour, je voudrais activer Lissafi Plus.")
            lines += "Boutique : ${shopName.ifBlank { "—" }}"
            lines += "Compte : ${email ?: "compte local"}"
            if (!userId.isNullOrBlank()) lines += "ID : $userId"
            return lines.joinToString("\n")
        }

        /** Lien wa.me vers le contact d'activation, message pré-rempli inclus. */
        fun buildActivationWhatsAppLink(shopName: String, email: String?, userId: String?): String {
            val message = buildActivationMessage(shopName, email, userId)
            return "https://wa.me/$SUPPORT_WHATSAPP_NUMBER?text=${Uri.encode(message)}"
        }

        // Codes Plus (V1) — activation du plan de volume
        private val VALID_CODES = setOf(
            "LISSAFI-PREMIUM-0001", "LISSAFI-PREMIUM-0002", "LISSAFI-PREMIUM-0003",
            "LISSAFI-PREMIUM-0004", "LISSAFI-PREMIUM-0005", "LISSAFI-PREMIUM-0006",
            "LISSAFI-PREMIUM-0007", "LISSAFI-PREMIUM-0008", "LISSAFI-PREMIUM-0009",
            "LISSAFI-PREMIUM-0010", "LISSAFI-PREMIUM-0011", "LISSAFI-PREMIUM-0012",
            "LISSAFI-PREMIUM-0013", "LISSAFI-PREMIUM-0014", "LISSAFI-PREMIUM-0015",
            "LISSAFI-PREMIUM-0016", "LISSAFI-PREMIUM-0017", "LISSAFI-PREMIUM-0018",
            "LISSAFI-PREMIUM-0019", "LISSAFI-PREMIUM-0020"
        )

        // Codes Business — activation du plan haut de gamme (tout illimité)
        private val BUSINESS_CODES = setOf(
            "LISSAFI-BUSINESS-0001", "LISSAFI-BUSINESS-0002", "LISSAFI-BUSINESS-0003",
            "LISSAFI-BUSINESS-0004", "LISSAFI-BUSINESS-0005"
        )
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
     * Active Plus (code LISSAFI-PREMIUM-*) ou Business (code LISSAFI-BUSINESS-*).
     * Un code ne peut être utilisé qu'UNE fois sur cet appareil : sinon, un
     * code partagé re-grantait 365 jours à l'infini.
     */
    suspend fun activateWithCode(code: String): Boolean {
        val plan = when {
            BUSINESS_CODES.contains(code) -> "business"
            VALID_CODES.contains(code) -> "plus"
            else -> return false
        }
        val previous = repository.getSetting("activation_code")
        if (previous == code) return false

        val expiry = System.currentTimeMillis() + PREMIUM_DAYS * 24 * 60 * 60 * 1000L
        repository.setSetting("is_premium", "true")
        repository.setSetting("plan", plan)
        repository.setSetting("premium_expiry", expiry.toString())
        repository.setSetting("activation_code", code)
        // La démo est consommée dès qu'un code est utilisé : pas de 7 jours bonus
        // après l'expiration du code.
        repository.setSetting("demo_taken", "true")
        return true
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
