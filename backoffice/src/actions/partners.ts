"use server";

import { revalidatePath } from "next/cache";
import { supabaseAdmin } from "@/lib/supabase";
import { requireAdmin } from "@/lib/session";
import { logAdminAction as logAction } from "@/lib/audit";
import { COMMISSIONS } from "@/lib/partners";
import type { Plan } from "@/types";

export type PartnerActionResult = { ok?: boolean; error?: string };

const PARTNER_TYPES = new Set(["agent", "ambassador", "strategic", "referral"]);
const PLANS: Plan[] = ["plus", "business", "pack"];

function isPlan(v: string): v is Plan {
  return (PLANS as string[]).includes(v);
}

/** Génère un code partenaire lisible et unique (ex. PTN-K2M7Q), sans ambiguïté 0/O/1/I. */
function makeCode(): string {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let s = "";
  for (let i = 0; i < 5; i++) s += chars[Math.floor(Math.random() * chars.length)];
  return `PTN-${s}`;
}

// ============================================================
// PARTENAIRES
// ============================================================

/** Crée un partenaire. Le code est généré si non fourni. */
export async function addPartner(
  _prev: PartnerActionResult | undefined,
  formData: FormData
): Promise<PartnerActionResult> {
  await requireAdmin();
  const name = String(formData.get("name") || "").trim();
  const type = String(formData.get("type") || "").trim();
  const phone = String(formData.get("phone") || "").trim();
  const notes = String(formData.get("notes") || "").trim();
  const code = (String(formData.get("code") || "").trim().toUpperCase()) || makeCode();

  if (!name) return { error: "Nom du partenaire obligatoire." };
  if (!PARTNER_TYPES.has(type)) return { error: "Type de partenaire invalide." };

  const { error } = await supabaseAdmin().from("partners").insert({
    name,
    type,
    phone,
    notes,
    code,
    status: "active",
  });
  if (error) {
    console.error("[admin] addPartner:", error);
    if (error.code === "23505") return { error: `Code déjà utilisé : ${code}. Choisis-en un autre.` };
    return { error: "Une erreur est survenue. Réessaie." };
  }

  await logAction("partner_add", null, { name, type, phone, code });
  revalidatePath("/", "layout");
  return { ok: true };
}

/** Change l'état actif/inactif d'un partenaire. */
export async function togglePartnerStatus(partnerId: string): Promise<PartnerActionResult> {
  await requireAdmin();
  const { data: partner } = await supabaseAdmin()
    .from("partners")
    .select("status")
    .eq("id", partnerId)
    .maybeSingle();
  if (!partner) return { error: "Partenaire introuvable." };
  const next = partner.status === "active" ? "inactive" : "active";

  const { error } = await supabaseAdmin().from("partners").update({ status: next }).eq("id", partnerId);
  if (error) {
    console.error("[admin] togglePartnerStatus:", error);
    return { error: "Une erreur est survenue. Réessaie." };
  }

  await logAction("partner_toggle_status", null, { partnerId, next });
  revalidatePath("/", "layout");
  return { ok: true };
}

// ============================================================
// VENTES ATTRIBUÉES
// ============================================================

/** Attribue une vente payante à un partenaire : crée la commission due selon le barème. */
export async function addPartnerSale(
  _prev: PartnerActionResult | undefined,
  formData: FormData
): Promise<PartnerActionResult> {
  await requireAdmin();
  const partnerId = String(formData.get("partnerId") || "").trim();
  const plan = String(formData.get("plan") || "").trim();
  const clientName = String(formData.get("clientName") || "").trim();
  const clientPhone = String(formData.get("clientPhone") || "").trim();
  const note = String(formData.get("note") || "").trim();
  const amountRaw = Number(formData.get("amountPaid") || 0);

  if (!partnerId) return { error: "Partenaire manquant." };
  if (!isPlan(plan)) return { error: "Plan invalide." };
  const amountPaidFcfa = Math.max(0, Math.floor(Number.isFinite(amountRaw) ? amountRaw : 0));
  const commissionFcfa = COMMISSIONS[plan];

  const { error } = await supabaseAdmin().from("partner_sales").insert({
    partner_id: partnerId,
    client_name: clientName,
    client_phone: clientPhone,
    plan,
    amount_paid_fcfa: amountPaidFcfa,
    commission_fcfa: commissionFcfa,
    status: "owed",
    note,
    created_at: Date.now(),
  });
  if (error) {
    console.error("[admin] addPartnerSale:", error);
    return { error: "Une erreur est survenue. Réessaie." };
  }

  await logAction("partner_sale_add", null, { partnerId, plan, commissionFcfa, clientName });
  revalidatePath("/", "layout");
  return { ok: true };
}

/** Passe une vente attribuée de 'due' à 'payée'. */
export async function markCommissionPaid(saleId: number): Promise<PartnerActionResult> {
  await requireAdmin();
  const { error } = await supabaseAdmin()
    .from("partner_sales")
    .update({ status: "paid", paid_at: new Date().toISOString() })
    .eq("id", saleId)
    .eq("status", "owed");
  if (error) {
    console.error("[admin] markCommissionPaid:", error);
    return { error: "Une erreur est survenue. Réessaie." };
  }

  await logAction("partner_commission_paid", null, { saleId });
  revalidatePath("/", "layout");
  return { ok: true };
}

/** Passe toutes les ventes dues d'un partenaire en 'payées' (confort, un clic). */
export async function markAllCommissionsPaid(partnerId: string): Promise<PartnerActionResult> {
  await requireAdmin();
  const { error } = await supabaseAdmin()
    .from("partner_sales")
    .update({ status: "paid", paid_at: new Date().toISOString() })
    .eq("partner_id", partnerId)
    .eq("status", "owed");
  if (error) {
    console.error("[admin] markAllCommissionsPaid:", error);
    return { error: "Une erreur est survenue. Réessaie." };
  }

  await logAction("partner_commission_paid_all", null, { partnerId });
  revalidatePath("/", "layout");
  return { ok: true };
}

// ============================================================
// Variantes "Quick" — pour les boutons dans les formulaires
// ============================================================

export async function markCommissionPaidQuick(saleId: number) {
  "use server";
  const r = await markCommissionPaid(saleId);
  if (r && "error" in r) console.error("[admin] markCommissionPaidQuick:", r.error);
}

export async function markAllCommissionsPaidQuick(partnerId: string) {
  "use server";
  const r = await markAllCommissionsPaid(partnerId);
  if (r && "error" in r) console.error("[admin] markAllCommissionsPaidQuick:", r.error);
}

export async function togglePartnerStatusQuick(partnerId: string) {
  "use server";
  const r = await togglePartnerStatus(partnerId);
  if (r && "error" in r) console.error("[admin] togglePartnerStatusQuick:", r.error);
}
