import { NextRequest, NextResponse } from "next/server";
import { supabaseAdmin } from "@/lib/supabase";
import { sendPushToTokens } from "@/lib/notifications";

export const dynamic = "force-dynamic";

// Niamey = UTC+1 toute l'année (pas de changement d'heure).
const NIAMEY_OFFSET_MS = 60 * 60 * 1000;

function niameyTodayDateStr(nowMs: number): string {
  const d = new Date(nowMs + NIAMEY_OFFSET_MS);
  const y = d.getUTCFullYear();
  const m = String(d.getUTCMonth() + 1).padStart(2, "0");
  const day = String(d.getUTCDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function yesterdayWindowMs(nowMs: number): { from: number; to: number } {
  const d = new Date(nowMs + NIAMEY_OFFSET_MS);
  const todayMidnightNiameyUTC = Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()) - NIAMEY_OFFSET_MS;
  return { from: todayMidnightNiameyUTC - 86400000, to: todayMidnightNiameyUTC - 1 };
}

function buildMessage(salesTotal: number, salesCount: number, newDebtsTotal: number): { title: string; body: string } {
  if (salesCount === 0) {
    return { title: "Nouvelle journée, bonne chance !", body: "Aucune vente enregistrée hier. Bonne journée !" };
  }
  const fcfa = new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 0 }).format(salesTotal);
  let body = `Hier : ${fcfa} FCFA de ventes en ${salesCount} transaction${salesCount > 1 ? "s" : ""}.`;
  if (newDebtsTotal > 0) {
    const debtFcfa = new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 0 }).format(newDebtsTotal);
    body += ` Nouvelles dettes : ${debtFcfa} FCFA.`;
  }
  body += " Bonne journée !";
  return { title: "Récap d'hier", body };
}

export async function GET(request: NextRequest) {
  const authHeader = request.headers.get("authorization");
  if (authHeader !== `Bearer ${process.env.CRON_SECRET}`) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }

  const { data: settingRow } = await supabaseAdmin()
    .from("admin_settings")
    .select("value")
    .eq("key", "recap_notifications_enabled")
    .maybeSingle();
  if (settingRow && settingRow.value === "false") {
    return NextResponse.json({ skipped: "disabled" });
  }

  const now = Date.now();
  const recapDate = niameyTodayDateStr(now);

  // Réservation anti-doublon : si l'insertion échoue (contrainte unique sur
  // recap_date), le récap d'aujourd'hui a déjà été traité (ou est en cours).
  const { error: insertError } = await supabaseAdmin().from("notification_log").insert({
    kind: "recap_quotidien",
    recap_date: recapDate,
    title: "Récap quotidien",
    body: "",
    recipients: 0,
    success: 0,
    failed: 0,
  });
  if (insertError) {
    return NextResponse.json({ skipped: "already_sent_or_error", detail: insertError.message });
  }

  const { from, to } = yesterdayWindowMs(now);

  const [{ data: recapRows }, { data: deviceRows }] = await Promise.all([
    supabaseAdmin().rpc("admin_recap_yesterday", { from_ts: from, to_ts: to }),
    supabaseAdmin().from("device_tokens").select("user_id, fcm_token"),
  ]);

  const recapByUser = new Map<string, { sales_total: number; sales_count: number; new_debts_total: number }>();
  for (const row of recapRows ?? []) {
    recapByUser.set(row.user_id, {
      sales_total: row.sales_total,
      sales_count: row.sales_count,
      new_debts_total: row.new_debts_total,
    });
  }

  const tokensByUser = new Map<string, string[]>();
  for (const row of (deviceRows ?? []) as { user_id: string; fcm_token: string }[]) {
    const list = tokensByUser.get(row.user_id) ?? [];
    list.push(row.fcm_token);
    tokensByUser.set(row.user_id, list);
  }

  let recipients = 0;
  let success = 0;
  let failed = 0;

  for (const [userId, tokens] of tokensByUser) {
    const recap = recapByUser.get(userId) ?? { sales_total: 0, sales_count: 0, new_debts_total: 0 };
    const { title, body } = buildMessage(recap.sales_total, recap.sales_count, recap.new_debts_total);
    const result = await sendPushToTokens(tokens, title, body);
    recipients += tokens.length;
    success += result.success;
    failed += result.failed;
  }

  await supabaseAdmin()
    .from("notification_log")
    .update({ recipients, success, failed })
    .eq("kind", "recap_quotidien")
    .eq("recap_date", recapDate);

  return NextResponse.json({ recipients, success, failed });
}
