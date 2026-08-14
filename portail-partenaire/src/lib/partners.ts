import type { Plan, PartnerType } from "@/types";

// Barème fixe et permanent — commission une fois par vente, abonnement récurrent.
export const COMMISSIONS: Record<Plan, number> = {
  plus: 10_000,
  business: 15_000,
  pack: 5_000,
};

export const PARTNER_TYPE_LABELS: Record<PartnerType, string> = {
  agent: "Revendeur / agent",
  ambassador: "Ambassadeur / influenceur",
  strategic: "Partenariat stratégique",
  referral: "Parrainage utilisateur",
};

export const PLAN_LABELS: Record<Plan, string> = {
  plus: "Lissafi Plus",
  business: "Lissafi Business",
  pack: "Pack Boutique",
};

// Plan effectif d'un membre parrainé (Gratuit / Plus / Business).
export const MEMBER_PLAN_LABELS: Record<"free" | "plus" | "business", string> = {
  free: "Gratuit",
  plus: "Plus",
  business: "Business",
};

// Longueur minimale du mot de passe partenaire.
export const MIN_PASSWORD_LENGTH = 8;

/**
 * Montant qu'un partenaire peut retirer :
 * commissions dues − retraits déjà demandés mais pas encore payés.
 * Jamais négatif.
 */
export function computeAvailable(commissionDue: number, requestedUnpaid: number): number {
  return Math.max(0, Math.floor(commissionDue) - Math.floor(requestedUnpaid));
}

/**
 * URL publique de la landing (où atterrissent les clients d'un partenaire).
 * NEXT_PUBLIC_LANDING_URL est exposé au navigateur — valeur non secrète.
 */
export function landingBaseUrl(): string {
  const raw = process.env.NEXT_PUBLIC_LANDING_URL?.trim() || "https://lissafi-one.vercel.app";
  return raw.replace(/\/+$/, "");
}

/** Lien personnel d'un partenaire : landing + code d'attribution. */
export function partnerLink(code: string): string {
  return `${landingBaseUrl()}/?p=${encodeURIComponent(code)}`;
}

/** URL d'une image QR (service public) encodant un lien — téléchargeable. */
export function qrImageUrl(data: string, size = 240): string {
  return `https://api.qrserver.com/v1/create-qr-code/?size=${size}x${size}&margin=8&data=${encodeURIComponent(data)}`;
}
