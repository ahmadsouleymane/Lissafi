// Fichier APK servi depuis le site (public/lissafi.apk).
// À mettre à jour après chaque build Android : `npm run update-apk`.
export const APK = "lissafi.apk";

// Numéro WhatsApp d'activation (même numéro que dans l'app — PremiumManager.kt).
export const WHATSAPP_NUMBER = "22799281491";
export const WHATSAPP_URL = `https://wa.me/${WHATSAPP_NUMBER}`;

// Back-office où vit le portail partenaire (auto-inscription + espace) et le
// beacon de visite (/api/visits). Surchargé via VITE_PORTAL_URL au déploiement.
export const PORTAL_URL = (import.meta.env.VITE_PORTAL_URL || "https://lissafi-admin.vercel.app").replace(/\/+$/, "");
export const BECOME_PARTNER_URL = `${PORTAL_URL}/partenaire`;

// Back-office où vivent les API de paiement (/api/payments/*). Les clés secrètes
// iPayMoney/GeniusPay n'existent que là (côté serveur) — la landing ne les voit
// jamais. Surchargé via VITE_PAYMENTS_URL au déploiement.
export const PAYMENTS_API_URL = (import.meta.env.VITE_PAYMENTS_URL || "https://lissafi-admin.vercel.app").replace(/\/+$/, "");
