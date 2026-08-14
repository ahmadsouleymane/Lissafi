import { cookies } from "next/headers";
import { supabaseAdmin, supabaseAuth } from "./supabase";

export const ADMIN_COOKIE = "lissafi_admin_token";

export type AdminSession = {
  userId: string;
  email: string;
};

/**
 * Lit le token admin dans le cookie, le valide auprès de GoTrue,
 * puis vérifie que le compte figure bien dans la table `admins`.
 * Retourne null si l'utilisateur n'est pas autorisé.
 */
export async function getAdminSession(): Promise<AdminSession | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get(ADMIN_COOKIE)?.value;
  if (!token) return null;

  try {
    const { data, error } = await supabaseAuth().auth.getUser(token);
    if (error || !data.user) return null;

    const { data: adminRow, error: adminError } = await supabaseAdmin()
      .from("admins")
      .select("user_id")
      .eq("user_id", data.user.id)
      .maybeSingle();

    if (adminError || !adminRow) return null;

    return { userId: data.user.id, email: data.user.email ?? "" };
  } catch {
    return null;
  }
}

/** Exige une session admin ; sinon lève une erreur (utilisé par les actions serveur). */
export async function requireAdmin(): Promise<AdminSession> {
  const session = await getAdminSession();
  if (!session) throw new Error("Non autorisé — session admin invalide.");
  return session;
}
