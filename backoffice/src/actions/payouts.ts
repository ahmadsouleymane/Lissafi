"use server";

import { revalidatePath } from "next/cache";
import { supabaseAdmin } from "@/lib/supabase";
import { requireAdmin } from "@/lib/session";
import { logAdminAction as logAction } from "@/lib/audit";
import type { PartnerActionResult } from "./partners";
import type { PartnerSale } from "@/types";

/**
 * Valide une demande de retrait (paiement physique effectué par l'admin) :
 * 1. passe la demande `requested` → `paid` ;
 * 2. marque les plus anciennes commissions dues du partenaire `paid` jusqu'à
 *    couvrir le montant retiré → zéro double comptage sur les prochains retraits.
 */
export async function approvePayout(payoutId: number): Promise<PartnerActionResult> {
  await requireAdmin();

  const { data: payout, error: payoutError } = await supabaseAdmin()
    .from("partner_payouts")
    .select("id, partner_id, amount_fcfa, status")
    .eq("id", payoutId)
    .maybeSingle();
  if (payoutError) {
    console.error("[admin] approvePayout (read):", payoutError);
    return { error: "Une erreur est survenue. Réessaie." };
  }
  if (!payout) return { error: "Demande introuvable." };
  if (payout.status !== "requested") return { error: "Cette demande est déjà traitée." };

  // 1. Marque la demande payée (garde l'idempotence via le filtre sur le statut).
  const { error: updError, data: updated } = await supabaseAdmin()
    .from("partner_payouts")
    .update({ status: "paid", paid_at: Date.now() })
    .eq("id", payoutId)
    .eq("status", "requested")
    .select("id");
  if (updError) {
    console.error("[admin] approvePayout (update):", updError);
    return { error: "Une erreur est survenue. Réessaie." };
  }
  if (!updated || updated.length === 0) return { error: "Cette demande est déjà traitée." };

  // 2. Solde les commissions dues les plus anciennes jusqu'à couvrir le montant.
  const { data: owed } = await supabaseAdmin()
    .from("partner_sales")
    .select("id, commission_fcfa")
    .eq("partner_id", payout.partner_id)
    .eq("status", "owed")
    .order("created_at", { ascending: true });

  const toMark: number[] = [];
  let covered = 0;
  for (const sale of (owed ?? []) as Pick<PartnerSale, "id" | "commission_fcfa">[]) {
    if (covered >= payout.amount_fcfa) break;
    toMark.push(sale.id);
    covered += Number(sale.commission_fcfa || 0);
  }
  if (toMark.length > 0) {
    const { error: markError } = await supabaseAdmin()
      .from("partner_sales")
      .update({ status: "paid", paid_at: new Date().toISOString() })
      .in("id", toMark)
      .eq("status", "owed");
    if (markError) console.error("[admin] approvePayout (mark sales):", markError);
  }

  await logAction("partner_payout_paid", null, { payoutId, partnerId: payout.partner_id, amount: payout.amount_fcfa });
  revalidatePath("/", "layout");
  return { ok: true };
}

export async function approvePayoutQuick(payoutId: number) {
  "use server";
  const r = await approvePayout(payoutId);
  if (r && "error" in r) console.error("[admin] approvePayoutQuick:", r.error);
}
