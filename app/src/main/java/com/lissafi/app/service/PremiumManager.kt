package com.lissafi.app.service

import android.net.Uri
import com.lissafi.app.data.remote.SupabaseApi
import com.lissafi.app.data.repository.LissafiRepository
import kotlin.math.ceil

class PremiumManager(
    private val repository: LissafiRepository,
    private val api: SupabaseApi
) {

    companion object {
        /** Essai gratuit full-option offert à l'inscription (compteur dès le 1er login). */
        const val TRIAL_DAYS = 14

        /** Limite de la Petite boutique (PLUS). Les ventes ne sont JAMAIS limitées sur un plan payant. */
        const val MAX_PLUS_PRODUCTS = 200

        // ── Tarifs abonnement (FCFA) ──
        const val PLUS_MONTHLY = 3_000
        const val PLUS_QUARTERLY = 7_500
        const val PLUS_YEARLY = 24_000
        const val BUSINESS_MONTHLY = 6_000
        const val BUSINESS_QUARTERLY = 15_000
        const val BUSINESS_YEARLY = 50_000
        /** Pack Boutique : paiement unique = imprimante 58 mm + 2 rouleaux + 1 an Petite boutique. */
        const val PACK_ONE_TIME = 60_000

        /** WhatsApp pro pour l'activation manuelle / paiement en espèces. */
        const val WHATSAPP_NUMBER = "22799281491"

        // Page de paiement en ligne (landing statique, aucune clé côté client).
        private const val PAYMENT_URL = "https://lissafi-one.vercel.app/payer"

        /** Lien vers la page de paiement carte / Mobile Money, plan+période+email pré-remplis. */
        fun buildActivationPaymentLink(plan: String, period: String? = null, email: String? = null): String {
            val builder = Uri.parse(PAYMENT_URL).buildUpon().appendQueryParameter("plan", plan)
            if (!period.isNullOrBlank()) builder.appendQueryParameter("period", period)
            if (!email.isNullOrBlank()) builder.appendQueryParameter("email", email)
            return builder.build().toString()
        }

        /** Lien WhatsApp pré-rempli pour payer/activer un abonnement (espèces ou repli). */
        fun buildWhatsAppActivationLink(planLabel: String, periodLabel: String, price: Int, email: String?): String {
            val msg = buildString {
                append("Bonjour, je souhaite activer mon abonnement Lissafi.\n")
                append("Formule : $planLabel ($periodLabel) — $price FCFA\n")
                if (!email.isNullOrBlank()) append("Compte : $email\n")
                append("Comment payer ?")
            }
            return "https://wa.me/$WHATSAPP_NUMBER?text=${Uri.encode(msg)}"
        }
    }

    /**
     * État d'abonnement effectif :
     * - TRIAL   : essai 14 j full-option (tout illimité)
     * - PLUS    : Petite boutique (payant, ~200 produits)
     * - BUSINESS: Commerce/Supermarché (payant, illimité, multi-postes)
     * - LOCKED  : essai terminé et aucun abonnement → app bloquée (paywall)
     */
    enum class Plan { TRIAL, PLUS, BUSINESS, LOCKED }

    suspend fun getPlan(): Plan {
        if (isPremium()) {
            return if (repository.getSetting("plan") == "business") Plan.BUSINESS else Plan.PLUS
        }
        return if (isInTrial()) Plan.TRIAL else Plan.LOCKED
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

    /** Démarre l'essai à la première ouverture du compte (idempotent, jamais si déjà abonné). */
    suspend fun startTrialIfNeeded() {
        if (isPremium()) return
        if (repository.getSetting("trial_start").isNullOrBlank()) {
            repository.setSetting("trial_start", System.currentTimeMillis().toString())
            api.logEvent("trial_start", "info", "Essai 14 jours démarré")
        }
    }

    /** Fin de l'essai (epoch millis) ou null si l'essai n'a jamais démarré. */
    suspend fun trialEndMillis(): Long? {
        val start = repository.getSetting("trial_start")?.toLongOrNull() ?: return null
        return start + TRIAL_DAYS * 24L * 60 * 60 * 1000
    }

    suspend fun isInTrial(): Boolean {
        if (isPremium()) return false
        val end = trialEndMillis() ?: return false
        return System.currentTimeMillis() < end
    }

    /** Jours d'essai restants (0 si terminé ou non démarré). */
    suspend fun trialDaysLeft(): Int {
        val end = trialEndMillis() ?: return 0
        val remaining = end - System.currentTimeMillis()
        if (remaining <= 0) return 0
        return ceil(remaining / (24.0 * 60 * 60 * 1000)).toInt()
    }

    /** App bloquée : essai terminé et aucun abonnement actif. */
    suspend fun isLocked(): Boolean = getPlan() == Plan.LOCKED

    /**
     * Active Plus ou Business via un code VALIDÉ CÔTÉ SERVEUR (`redeem_premium_code`).
     * Un code est à usage unique, lié au compte qui l'a saisi. Sans réseau ou session,
     * l'activation échoue (retour false).
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
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Ajout produit : TRIAL/BUSINESS illimité, PLUS ≤200, LOCKED interdit. */
    suspend fun canAddProduct(): Boolean = when (getPlan()) {
        Plan.TRIAL, Plan.BUSINESS -> true
        Plan.PLUS -> repository.getProductCount() < MAX_PLUS_PRODUCTS
        Plan.LOCKED -> false
    }

    /** Ajout client : autorisé sur tout plan actif, interdit si LOCKED. */
    suspend fun canAddClient(): Boolean = getPlan() != Plan.LOCKED

    /** Vente : jamais limitée sur un plan actif ou en essai, interdite si LOCKED. */
    suspend fun canMakeSale(): Boolean = getPlan() != Plan.LOCKED
}
