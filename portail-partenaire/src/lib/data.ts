import "server-only";
import { supabaseAdmin } from "./supabase";
import { toNumber } from "./format";
import type { PartnerDashboard, PartnerMember, PartnerPayout, PartnerSale, PartnerVisit } from "@/types";

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

/** Compte des installations attribuées à un partenaire (beacon au 1er lancement). */
async function getPartnerInstallsCount(partnerId: string): Promise<number> {
  const { count } = await supabaseAdmin()
    .from("partner_installs")
    .select("*", { count: "exact", head: true })
    .eq("partner_id", partnerId);
  return toNumber(count);
}

/** Membres parrainés (utilisateurs dont partner_code = code) + leur plan. */
async function getPartnerMembers(code: string): Promise<PartnerMember[]> {
  const { data: refRows } = await supabaseAdmin()
    .from("app_settings")
    .select("user_id")
    .eq("key", "partner_code")
    .eq("value", code);
  const userIds = Array.from(new Set(((refRows ?? []) as { user_id: string }[]).map((r) => r.user_id)));
  if (userIds.length === 0) return [];

  const { data: detailRows } = await supabaseAdmin()
    .from("app_settings")
    .select("user_id, key, value")
    .in("user_id", userIds)
    .in("key", ["shop_name", "is_premium", "plan"]);

  const byUser: Record<string, { name: string; premium: boolean; plan: string }> = {};
  for (const row of (detailRows ?? []) as { user_id: string; key: string; value: string }[]) {
    byUser[row.user_id] ??= { name: "", premium: false, plan: "" };
    if (row.key === "shop_name") byUser[row.user_id].name = row.value;
    else if (row.key === "is_premium") byUser[row.user_id].premium = row.value === "true";
    else if (row.key === "plan") byUser[row.user_id].plan = row.value;
  }

  return userIds.map((uid) => {
    const d = byUser[uid] ?? { name: "", premium: false, plan: "" };
    const plan: PartnerMember["plan"] = !d.premium ? "free" : d.plan === "business" ? "business" : "plus";
    return { user_id: uid, name: d.name || "Commerçant", plan };
  });
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

  const [sales, payouts, recentVisits, visitCount, installsCount, members] = await Promise.all([
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
    getPartnerInstallsCount(partnerId),
    getPartnerMembers((partner.code as string) || ""),
  ]);

  const accounts = members.length;
  const paid = members.filter((m) => m.plan !== "free").length;
  const conversion_rate = accounts > 0 ? paid / accounts : null;

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
    installs: installsCount,
    accounts,
    paid,
    conversion_rate,
    members,
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
