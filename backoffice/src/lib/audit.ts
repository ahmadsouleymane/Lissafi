import "server-only";
import { supabaseAdmin } from "./supabase";
import { requireAdmin } from "./session";

/** Journalise une action admin (traçabilité). Ne bloque jamais l'action appelante. */
export async function logAdminAction(action: string, targetUserId: string | null, details: Record<string, unknown>) {
  const session = await requireAdmin();
  try {
    await supabaseAdmin().from("admin_actions").insert({
      admin_user_id: session.userId,
      action,
      target_user_id: targetUserId,
      details: JSON.stringify(details),
    });
  } catch {
    // Le journal ne doit jamais faire échouer l'action principale.
  }
}
