"use server";

import { revalidatePath } from "next/cache";
import { supabaseAdmin } from "@/lib/supabase";
import { requireAdmin } from "@/lib/session";
import { logAdminAction as logAction } from "@/lib/audit";

const DAY_MS = 86400000;

// ============================================================
// PREMIUM
// ============================================================

export type ActionResult = { ok?: boolean; error?: string; newExpiry?: number };

/** Active le premium pour N jours (défaut 365) — plan "plus" (défaut) ou "business". */
export async function activatePremium(userId: string, days = 365, plan = "plus"): Promise<ActionResult> {
  await requireAdmin();
  const n = Math.max(1, Math.floor(days));
  const expiry = Date.now() + n * DAY_MS;

  const { error } = await supabaseAdmin().from("app_settings").upsert(
    [
      { key: "is_premium", value: "true", user_id: userId },
      { key: "plan", value: plan === "business" ? "business" : "plus", user_id: userId },
      { key: "premium_expiry", value: String(expiry), user_id: userId },
      { key: "activation_method", value: "admin", user_id: userId },
    ],
    { onConflict: "key,user_id" }
  );
  if (error) return { error: error.message };

  await logAction("premium_activate", userId, { days: n, expiry, plan });
  revalidatePath("/", "layout");
  return { ok: true, newExpiry: expiry };
}

/** Ajoute N jours au premium (prolonge depuis l'expiration actuelle si future). */
export async function addPremiumDays(userId: string, days: number): Promise<ActionResult> {
  await requireAdmin();
  const n = Math.max(1, Math.floor(days));

  const { data: row } = await supabaseAdmin()
    .from("app_settings")
    .select("value")
    .eq("key", "premium_expiry")
    .eq("user_id", userId)
    .maybeSingle();

  const current = row && /^\d+$/.test(row.value) ? Number(row.value) : 0;
  const base = Math.max(current, Date.now());
  const newExpiry = base + n * DAY_MS;

  const { error } = await supabaseAdmin().from("app_settings").upsert(
    [
      { key: "is_premium", value: "true", user_id: userId },
      { key: "premium_expiry", value: String(newExpiry), user_id: userId },
    ],
    { onConflict: "key,user_id" }
  );
  if (error) return { error: error.message };

  await logAction("premium_add_days", userId, { days: n, newExpiry });
  revalidatePath("/", "layout");
  return { ok: true, newExpiry };
}

/** Variante formulaire : activation premium avec durée personnalisée (useActionState). */
export async function activatePremiumWithDays(_prev: ActionResult | undefined, formData: FormData): Promise<ActionResult> {
  const userId = String(formData.get("userId") || "");
  const days = Math.floor(Number(formData.get("days")) || 365);
  const plan = String(formData.get("plan") || "plus");
  if (!userId) return { error: "Utilisateur manquant." };
  return activatePremium(userId, days, plan);
}

/** Désactive le premium. */
export async function deactivatePremium(userId: string): Promise<ActionResult> {
  await requireAdmin();
  const { error } = await supabaseAdmin().from("app_settings").upsert(
    [
      { key: "is_premium", value: "false", user_id: userId },
      { key: "plan", value: "free", user_id: userId },
      { key: "premium_expiry", value: "0", user_id: userId },
    ],
    { onConflict: "key,user_id" }
  );
  if (error) return { error: error.message };

  await logAction("premium_deactivate", userId, {});
  revalidatePath("/", "layout");
  return { ok: true };
}

// ============================================================
// SUPPORT
// ============================================================

export async function updateTicketStatus(ticketId: number, status: string): Promise<ActionResult> {
  await requireAdmin();
  const { error } = await supabaseAdmin()
    .from("support_tickets")
    .update({ status, updated_at: Date.now() })
    .eq("id", ticketId);
  if (error) return { error: error.message };

  await logAction("ticket_status", null, { ticketId, status });
  revalidatePath("/support", "layout");
  return { ok: true };
}

export async function replyToTicket(ticketId: number, message: string): Promise<ActionResult> {
  const session = await requireAdmin();
  const trimmed = message.trim();
  if (!trimmed) return { error: "Message vide." };

  const { error } = await supabaseAdmin().from("ticket_replies").insert({
    ticket_id: ticketId,
    user_id: session.userId,
    message: trimmed,
    is_admin: true,
    created_at: Date.now(),
  });
  if (error) return { error: error.message };

  await supabaseAdmin()
    .from("support_tickets")
    .update({ status: "resolved", updated_at: Date.now() })
    .eq("id", ticketId);

  await logAction("ticket_reply", null, { ticketId });
  revalidatePath("/support", "layout");
  return { ok: true };
}

/** Variante formulaire : répondre à un ticket (useActionState). */
export async function replyToTicketForm(_prev: ActionResult | undefined, formData: FormData): Promise<ActionResult> {
  const ticketId = Number(formData.get("ticketId"));
  const message = String(formData.get("message") || "");
  if (!ticketId) return { error: "Ticket invalide." };
  return replyToTicket(ticketId, message);
}

export async function deleteTicket(ticketId: number): Promise<ActionResult> {
  await requireAdmin();
  const { error } = await supabaseAdmin().from("support_tickets").delete().eq("id", ticketId);
  if (error) return { error: error.message };

  await logAction("ticket_delete", null, { ticketId });
  revalidatePath("/support", "layout");
  return { ok: true };
}

// ============================================================
// OUTILS DE SUPPORT — compte utilisateur
// ============================================================

/** Variante formulaire : réinitialiser le mot de passe (useActionState). */
export async function resetUserPasswordForm(_prev: ActionResult | undefined, formData: FormData): Promise<ActionResult> {
  const userId = String(formData.get("userId") || "");
  const password = String(formData.get("password") || "");
  if (!userId) return { error: "Utilisateur manquant." };
  return resetUserPassword(userId, password);
}

/** Réinitialise le mot de passe d'un utilisateur (GoTrue admin API). */
export async function resetUserPassword(userId: string, password: string): Promise<ActionResult> {
  await requireAdmin();
  if (password.length < 6) return { error: "Mot de passe trop court (6 caractères minimum)." };

  const { error } = await supabaseAdmin().auth.admin.updateUserById(userId, { password });
  if (error) return { error: error.message };

  await logAction("user_password_reset", userId, {});
  return { ok: true };
}

// ============================================================
// RÉGLAGES
// ============================================================

// Allowlist stricte des réglages globaux : on n'écrit jamais une clé arbitraire
// (un admin compromis ne peut pas altérer d'autres mécanismes via upsert).
const ADMIN_SETTING_KEYS = new Set([
  "premium_price_fcfa",
  "premium_days",
  "demo_days",
  "recap_notifications_enabled",
]);

export async function updateAdminSetting(key: string, value: string): Promise<ActionResult> {
  await requireAdmin();
  if (!ADMIN_SETTING_KEYS.has(key)) {
    return { error: "Clé de réglage inconnue" };
  }
  if (value.length > 200) {
    return { error: "Valeur trop longue" };
  }
  const { error } = await supabaseAdmin().from("admin_settings").upsert({ key, value });
  if (error) {
    console.error("[admin] updateAdminSetting:", error);
    return { error: "Erreur lors de la mise à jour du réglage" };
  }

  await logAction("admin_setting_update", null, { key, value });
  revalidatePath("/reglages", "layout");
  return { ok: true };
}

// ============================================================
// Variantes "quick" — pour les boutons dans les formulaires
// (compatibles avec l'attribut `action` de <form>).
// Les erreurs sont loguées côté serveur ; l'UI se met à jour via
// revalidatePath dans la fonction sous-jacente.
// Pour avoir le résultat (succès/échec) côté client, utiliser
// les fonctions non-quick avec useActionState.
// ============================================================

export async function activatePremiumQuick(userId: string, days = 365) {
  "use server";
  const r = await activatePremium(userId, days);
  if (r && "error" in r) console.error("[admin] activatePremiumQuick:", r.error);
}

/** Activation rapide du plan Business (tout illimité). */
export async function activateBusinessQuick(userId: string, days = 365) {
  "use server";
  const r = await activatePremium(userId, days, "business");
  if (r && "error" in r) console.error("[admin] activateBusinessQuick:", r.error);
}

export async function addPremiumDaysQuick(userId: string, days = 30) {
  "use server";
  const r = await addPremiumDays(userId, days);
  if (r && "error" in r) console.error("[admin] addPremiumDaysQuick:", r.error);
}

export async function deactivatePremiumQuick(userId: string) {
  "use server";
  const r = await deactivatePremium(userId);
  if (r && "error" in r) console.error("[admin] deactivatePremiumQuick:", r.error);
}

export async function updateTicketStatusQuick(ticketId: number, status: string) {
  "use server";
  const r = await updateTicketStatus(ticketId, status);
  if (r && "error" in r) console.error("[admin] updateTicketStatusQuick:", r.error);
}

export async function deleteTicketQuick(ticketId: number) {
  "use server";
  const r = await deleteTicket(ticketId);
  if (r && "error" in r) console.error("[admin] deleteTicketQuick:", r.error);
}

/** Variante formulaire quick : mettre à jour un paramètre du back-office. */
export async function updateAdminSettingQuickForm(formData: FormData) {
  "use server";
  const key = String(formData.get("key") || "");
  const value = String(formData.get("value") || "");
  if (key) {
    const r = await updateAdminSetting(key, value);
    if (r && "error" in r) console.error("[admin] updateAdminSettingQuickForm:", r.error);
  }
}
