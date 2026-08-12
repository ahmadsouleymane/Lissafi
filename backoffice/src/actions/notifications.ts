"use server";

import { revalidatePath } from "next/cache";
import { supabaseAdmin } from "@/lib/supabase";
import { requireAdmin } from "@/lib/session";
import { logAdminAction } from "@/lib/audit";
import { sendPushToTokens } from "@/lib/notifications";
import { getUserSummaries } from "@/lib/data";
import type { ActionResult } from "./admin";
import type { UserSummary } from "@/types";

const STATUS_LABELS: Record<string, string> = {
  all: "tous les comptes",
  premium: "comptes premium actifs",
  free: "comptes gratuits",
  expired: "comptes premium expirés",
};

function resolveStatusTargets(users: UserSummary[], status: string): UserSummary[] {
  const now = Date.now();
  switch (status) {
    case "premium":
      return users.filter((u) => u.premium && u.premium_expiry > now);
    case "expired":
      return users.filter((u) => u.premium && u.premium_expiry <= now);
    case "free":
      return users.filter((u) => !u.premium);
    default:
      return users;
  }
}

/** Variante formulaire : envoi manuel d'une notification push (useActionState). */
export async function sendManualNotificationForm(_prev: ActionResult | undefined, formData: FormData): Promise<ActionResult> {
  const session = await requireAdmin();

  const title = String(formData.get("title") || "").trim();
  const body = String(formData.get("body") || "").trim();
  const mode = String(formData.get("mode") || "status");
  if (!title || !body) return { error: "Titre et message sont requis." };

  let targetUserIds: string[];
  let targetSummary: string;

  if (mode === "accounts") {
    targetUserIds = formData.getAll("userIds").map(String).filter(Boolean);
    if (targetUserIds.length === 0) return { error: "Sélectionnez au moins un compte." };
    targetSummary = `${targetUserIds.length} compte(s) sélectionné(s)`;
  } else {
    const status = String(formData.get("status") || "all");
    const users = await getUserSummaries();
    targetUserIds = resolveStatusTargets(users, status).map((u) => u.user_id);
    targetSummary = `${targetUserIds.length} ${STATUS_LABELS[status] ?? "comptes"}`;
    if (targetUserIds.length === 0) return { error: "Aucun compte ne correspond à ce ciblage." };
  }

  const { data: tokenRows, error: tokenError } = await supabaseAdmin()
    .from("device_tokens")
    .select("fcm_token")
    .in("user_id", targetUserIds);
  if (tokenError) return { error: tokenError.message };

  const tokens = (tokenRows ?? []).map((r) => r.fcm_token as string);
  const { success, failed } = await sendPushToTokens(tokens, title, body);

  const { error: logError } = await supabaseAdmin().from("notification_log").insert({
    kind: "manuel",
    title,
    body,
    admin_user_id: session.userId,
    target_summary: targetSummary,
    recipients: tokens.length,
    success,
    failed,
  });
  if (logError) {
    // L'envoi a réussi mais le journal n'est pas enregistré : on le signale
    // plutôt que de faire échouer l'action (le push est déjà parti).
    console.error("[notifications] journal non enregistré :", logError.message);
  }

  await logAdminAction("notification_send", null, { title, targetSummary, recipients: tokens.length, success, failed });
  revalidatePath("/notifications", "layout");
  return { ok: true };
}

/** Active/désactive le récap automatique quotidien. */
export async function setRecapNotificationsEnabled(enabled: boolean): Promise<ActionResult> {
  await requireAdmin();
  const { error } = await supabaseAdmin()
    .from("admin_settings")
    .upsert({ key: "recap_notifications_enabled", value: enabled ? "true" : "false" });
  if (error) return { error: error.message };

  await logAdminAction("recap_toggle", null, { enabled });
  revalidatePath("/notifications", "layout");
  return { ok: true };
}

/** Variante "quick" pour un bouton `<form action={...}>` sans retour d'état. */
export async function setRecapNotificationsEnabledQuick(enabled: boolean) {
  "use server";
  const r = await setRecapNotificationsEnabled(enabled);
  if (r && "error" in r) console.error("[notifications] setRecapNotificationsEnabledQuick:", r.error);
}
