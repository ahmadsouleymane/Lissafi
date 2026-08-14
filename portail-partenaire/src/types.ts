// ============================================================
// Types du portail partenaire
// ============================================================

export type PartnerType = "agent" | "ambassador" | "strategic" | "referral";
export type Plan = "plus" | "business" | "pack";

export type PartnerSale = {
  id: number;
  partner_id: string;
  client_name: string;
  client_phone: string;
  plan: Plan;
  amount_paid_fcfa: number;
  commission_fcfa: number;
  status: "owed" | "paid";
  paid_at: string | null;
  note: string;
  created_at: number;
};

export type PartnerVisit = {
  id: number;
  partner_id: string;
  created_at: number;
};

export type PartnerPayout = {
  id: number;
  partner_id: string;
  amount_fcfa: number;
  status: "requested" | "paid";
  requested_at: number;
  paid_at: number | null;
};

// Tableau de bord de l'espace partenaire.
export type PartnerDashboard = {
  id: string;
  name: string;
  code: string;
  phone: string;
  email: string;
  status: "active" | "inactive";
  visits: number;
  clients: number; // ventes confirmées attribuées
  earnings_month: number; // commissions du mois courant
  earnings_total: number; // commissions cumulées (toutes)
  commission_due: number;
  commission_paid: number;
  available: number; // due − retraits demandés non payés
  sales: PartnerSale[];
  payouts: PartnerPayout[];
  recent_visits: PartnerVisit[];
};
