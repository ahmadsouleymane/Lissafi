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

export type MemberPlan = "free" | "plus" | "business";

// Un membre parrainé (utilisateur dont le code partenaire est celui du
// partenaire), avec son plan effectif déduit des réglages premium.
export type PartnerMember = {
  user_id: string;
  name: string;
  plan: MemberPlan;
};

// Tableau de bord de l'espace partenaire.
export type PartnerDashboard = {
  id: string;
  name: string;
  code: string;
  phone: string;
  email: string;
  status: "active" | "inactive";
  visits: number; // clics sur le lien
  installs: number; // (a) installs attribués (beacon au 1er lancement)
  accounts: number; // (b) comptes créés (utilisateurs avec ce partner_code)
  paid: number; // (c) abonnements payants (Plus/Business) parmi les comptes
  conversion_rate: number | null; // paid / accounts (0..1), null si aucun compte
  members: PartnerMember[]; // liste des parrainés + leur plan
  clients: number; // ventes confirmées attribuées (commissions)
  earnings_month: number; // commissions du mois courant
  earnings_total: number; // commissions cumulées (toutes)
  commission_due: number;
  commission_paid: number;
  available: number; // due − retraits demandés non payés
  sales: PartnerSale[];
  payouts: PartnerPayout[];
  recent_visits: PartnerVisit[];
};
