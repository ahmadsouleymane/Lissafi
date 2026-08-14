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
