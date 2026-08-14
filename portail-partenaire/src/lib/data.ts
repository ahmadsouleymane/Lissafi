import "server-only";
import { supabaseAdmin } from "./supabase";
import { toNumber } from "./format";
import type { PartnerDashboard, PartnerPayout, PartnerSale, PartnerVisit } from "@/types";

// ============================================================
// Lectures — toutes via le client service_role (serveur), filtrées par
// l'id du partenaire issu du cookie de session.
// ============================================================

/** Historique des ventes attribuées à un partenaire (plus récentes d'abord). */
export async function getPartnerSales(partnerId: string): Promise<PartnerSale[]> {
  const { data, error } = await supabaseAdmin()
    .from("partner_sales")
    .select("*")
    .eq("partner_id", partnerId)
    .order("created_at", { ascending: false })
    .limit(200);
  if (error) {
    console.error("[portail] getPartnerSales:", error);
    return [];
  }
  return (data ?? []) as PartnerSale[];
}

/** Visites récentes d'un partenaire (clics sur son lien). */
export async function getPartnerVisits(partnerId: string, limit = 30): Promise<PartnerVisit[]> {
  const { data, error } = await supabaseAdmin()
    .from("partner_visits")
    .select("*")
    .eq("partner_id", partnerId)
    .order("created_at", { ascending: false })
    .limit(limit);
  if (error) {
    console.error("[portail] getPartnerVisits:", error);
    return [];
  }
  return (data ?? []) as PartnerVisit[];
}

/**
 * Tableau de bord complet de l'espace partenaire.
 * Toutes les lectures via service_role, filtrées par l'id du partenaire.
 */
export async function getPartnerDashboard(partnerId: string): Promise<PartnerDashboard | null> {
  const { data: partner, error: partnerError } = await supabaseAdmin()
    .from("partners")
    .select("id, name, code, phone, email, status")
    .eq("id", partnerId)
    .maybeSingle();
  if (partnerError || !partner) {
    if (partnerError) console.error("[portail] getPartnerDashboard:", partnerError);
    return null;
  }

  const [sales, payouts, recentVisits, visitCount] = await Promise.all([
    getPartnerSales(partnerId),
    (async () => {
      const { data } = await supabaseAdmin()
        .from("partner_payouts")
        .select("*")
        .eq("partner_id", partnerId)
        .order("requested_at", { ascending: false })
        .limit(50);
      return (data ?? []) as PartnerPayout[];
    })(),
    getPartnerVisits(partnerId, 30),
    (async () => {
      const { count } = await supabaseAdmin()
        .from("partner_visits")
        .select("*", { count: "exact", head: true })
        .eq("partner_id", partnerId);
      return toNumber(count);
    })(),
  ]);

  const commissionDue = sales
    .filter((s) => s.status === "owed")
    .reduce((acc, s) => acc + toNumber(s.commission_fcfa), 0);
  const commissionPaid = sales
    .filter((s) => s.status === "paid")
    .reduce((acc, s) => acc + toNumber(s.commission_fcfa), 0);
  const earningsTotal = commissionDue + commissionPaid;

  const now = new Date();
  const monthStart = new Date(now.getFullYear(), now.getMonth(), 1).getTime();
  const earningsMonth = sales
    .filter((s) => toNumber(s.created_at) >= monthStart)
    .reduce((acc, s) => acc + toNumber(s.commission_fcfa), 0);

  const requestedUnpaid = payouts
    .filter((p) => p.status === "requested")
    .reduce((acc, p) => acc + toNumber(p.amount_fcfa), 0);
  const available = Math.max(0, commissionDue - requestedUnpaid);

  return {
    id: partner.id as string,
    name: (partner.name as string) || "",
    code: (partner.code as string) || "",
    phone: (partner.phone as string) || "",
    email: (partner.email as string) || "",
    status: (partner.status as "active" | "inactive") || "active",
    visits: visitCount,
    clients: sales.length,
    earnings_month: earningsMonth,
    earnings_total: earningsTotal,
    commission_due: commissionDue,
    commission_paid: commissionPaid,
    available,
    sales,
    payouts,
    recent_visits: recentVisits,
  };
}
