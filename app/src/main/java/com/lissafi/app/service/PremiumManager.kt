package com.lissafi.app.service

import com.lissafi.app.data.repository.LissafiRepository

class PremiumManager(private val repository: LissafiRepository) {

    companion object {
        const val DEMO_DAYS = 7
        const val PREMIUM_DAYS = 365

        // Codes pré-générés pour la V1 (liste statique)
        private val VALID_CODES = setOf(
            "LISSAFI-PREMIUM-0001", "LISSAFI-PREMIUM-0002", "LISSAFI-PREMIUM-0003",
            "LISSAFI-PREMIUM-0004", "LISSAFI-PREMIUM-0005", "LISSAFI-PREMIUM-0006",
            "LISSAFI-PREMIUM-0007", "LISSAFI-PREMIUM-0008", "LISSAFI-PREMIUM-0009",
            "LISSAFI-PREMIUM-0010", "LISSAFI-PREMIUM-0011", "LISSAFI-PREMIUM-0012",
            "LISSAFI-PREMIUM-0013", "LISSAFI-PREMIUM-0014", "LISSAFI-PREMIUM-0015",
            "LISSAFI-PREMIUM-0016", "LISSAFI-PREMIUM-0017", "LISSAFI-PREMIUM-0018",
            "LISSAFI-PREMIUM-0019", "LISSAFI-PREMIUM-0020"
        )

        const val MAX_FREE_PRODUCTS = 10
        const val MAX_FREE_CREDITS = 10
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
        repository.setSetting("premium_expiry", newExpiry.toString())
        repository.setSetting("demo_taken", "true")
        return true
    }

    suspend fun activateWithCode(code: String): Boolean {
        if (!VALID_CODES.contains(code)) return false

        val expiry = System.currentTimeMillis() + PREMIUM_DAYS * 24 * 60 * 60 * 1000L
        repository.setSetting("is_premium", "true")
        repository.setSetting("premium_expiry", expiry.toString())
        repository.setSetting("activation_code", code)
        return true
    }

    suspend fun canAddProduct(): Boolean {
        if (isPremium()) return true
        return repository.getProductCount() < MAX_FREE_PRODUCTS
    }

    suspend fun canAddClient(): Boolean {
        if (isPremium()) return true
        return repository.getClientCount() < MAX_FREE_CREDITS
    }
}
