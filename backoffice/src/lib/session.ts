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

// ============================================================
// Session partenaire (portail auto-serveur) — cookie distinct de l'admin.
// ============================================================

export const PARTNER_COOKIE = "lissafi_partner_token";

export type PartnerSession = {
  partnerId: string;
  authUid: string;
  email: string;
  name: string;
};

/**
 * Lit le token partenaire dans le cookie, le valide auprès de GoTrue,
 * puis retrouve la ligne `partners` liée (`auth_uid`).
 * Retourne null si le token est invalide ou sans partenaire associé.
 */
export async function getPartnerSession(): Promise<PartnerSession | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get(PARTNER_COOKIE)?.value;
  if (!token) return null;

  try {
    const { data, error } = await supabaseAuth().auth.getUser(token);
    if (error || !data.user) return null;

    const { data: partner, error: partnerError } = await supabaseAdmin()
      .from("partners")
      .select("id, name, email")
      .eq("auth_uid", data.user.id)
      .maybeSingle();

    if (partnerError || !partner) return null;

    return {
      partnerId: partner.id as string,
      authUid: data.user.id,
      email: (partner.email as string) || data.user.email || "",
      name: (partner.name as string) || "",
    };
  } catch {
    return null;
  }
}

/** Exige une session partenaire ; sinon lève une erreur (actions serveur). */
export async function requirePartner(): Promise<PartnerSession> {
  const session = await getPartnerSession();
  if (!session) throw new Error("Non autorisé — session partenaire invalide.");
  return session;
}
