import { PORTAL_URL, WHATSAPP_NUMBER } from "../config";

// ============================================================
// Attribution partenaire : lecture du code `?p=CODE`, beacon de visite
// et lien WhatsApp pré-rempli. Le code voyage dans le message WhatsApp.
// ============================================================

/** Lit et valide le code partenaire dans l'URL (?p=PTN-XXXXX). "" si absent/invalide. */
export function getPartnerCode() {
  if (typeof window === "undefined") return "";
  const raw = (new URLSearchParams(window.location.search).get("p") || "").trim().toUpperCase();
  return /^PTN-[A-Z0-9]{3,}$/.test(raw) ? raw : "";
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
