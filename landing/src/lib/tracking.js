import { PORTAL_URL, WHATSAPP_NUMBER, APK } from "../config";

// ============================================================
// Attribution partenaire : lecture du code `?p=CODE`, beacon de visite
// et lien WhatsApp pré-rempli. Le code voyage dans le message WhatsApp.
// ============================================================

const PARTNER_CODE_STORAGE_KEY = "lissafi_partner_code";

/** Lit et valide le code partenaire dans l'URL (?p=PTN-XXXXX). "" si absent/invalide. */
export function getPartnerCode() {
  if (typeof window === "undefined") return "";
  const raw = (new URLSearchParams(window.location.search).get("p") || "").trim().toUpperCase();
  return /^PTN-[A-Z0-9]{3,}$/.test(raw) ? raw : "";
}

/**
 * Persiste le code partenaire (localStorage) dès qu'il est détecté dans l'URL,
 * pour qu'il survive la navigation sur la page (le visiteur peut scroller
 * jusqu'aux tarifs ou à l'installation avant de télécharger, bien après le
 * chargement initial).
 */
export function savePartnerCode(code) {
  if (typeof window === "undefined" || !code) return;
  try {
    window.localStorage.setItem(PARTNER_CODE_STORAGE_KEY, code);
  } catch {
    // Stockage indisponible (navigation privée…) — sans impact, juste moins fiable.
  }
}

/** Code partenaire mémorisé lors d'une visite précédente sur cette page. */
function getStoredPartnerCode() {
  if (typeof window === "undefined") return "";
  try {
    return window.localStorage.getItem(PARTNER_CODE_STORAGE_KEY) || "";
  } catch {
    return "";
  }
}

/**
 * Copie le code partenaire dans le presse-papiers au moment du clic sur
 * "Télécharger l'APK" — c'est le seul relais possible entre le navigateur et
 * l'app une fois hors Play Store (pas d'Install Referrer). Doit être appelée
 * SYNCHRONEMENT depuis le handler onClick (geste utilisateur), sinon Safari
 * refuse l'accès au presse-papiers.
 * Retourne le code copié, ou "" si aucun code n'est connu pour ce visiteur.
 */
export function copyPartnerCodeForDownload() {
  const code = getPartnerCode() || getStoredPartnerCode();
  if (!code) return "";
  try {
    navigator.clipboard?.writeText(code).catch(() => {});
  } catch {
    // Presse-papiers indisponible — l'utilisateur pourra toujours saisir le
    // code à la main dans l'app (affiché en clair dans la bannière).
  }
  return code;
}

/**
 * URL du téléchargement de l'APK. Si un code partenaire est connu (URL `?p=`
 * ou localStorage d'une visite précédente), on passe par la route du portail
 * qui EMBARQUE le code dans le commentaire ZIP de l'APK (attribution qui
 * survit au partage du fichier). Sinon, l'APK statique de la landing suffit.
 */
export function downloadUrl() {
  if (typeof window === "undefined") return APK;
  const code = getPartnerCode() || getStoredPartnerCode();
  return code
    ? `${PORTAL_URL}/api/download?p=${encodeURIComponent(code)}`
    : APK;
}

// Une seule visite enregistrée par chargement de page.
let visitSent = false;

/** Envoie un beacon de visite au back-office (fire-and-forget). */
export function recordVisit(code) {
  if (!code || visitSent) return;
  visitSent = true;
  try {
    fetch(`${PORTAL_URL}/api/visits`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ code }),
      keepalive: true,
    }).catch(() => {});
  } catch {
    // Réseau indisponible — la visite est simplement perdue, sans impact.
  }
}

/** Lien WhatsApp d'activation, pré-rempli avec le code partenaire s'il existe. */
export function whatsappUrl(code) {
  const text = code
    ? `Bonjour, je veux activer Lissafi. Je viens de la part de ${code}.`
    : "Bonjour, je veux activer Lissafi.";
  return `https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent(text)}`;
}
