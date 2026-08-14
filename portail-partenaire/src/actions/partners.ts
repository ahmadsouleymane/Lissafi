"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { revalidatePath } from "next/cache";
import { isConfigured, supabaseAdmin, supabaseAuth } from "@/lib/supabase";
import { PARTNER_COOKIE, requirePartner } from "@/lib/session";
import { MIN_PASSWORD_LENGTH } from "@/lib/partners";

export type PartnerActionResult = { ok?: boolean; error?: string };

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Génère un code partenaire lisible et unique (ex. PTN-K2M7Q), sans ambiguïté 0/O/1/I. */
function makeCode(): string {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let s = "";
  for (let i = 0; i < 5; i++) s += chars[Math.floor(Math.random() * chars.length)];
  return `PTN-${s}`;
}

/** Pose le cookie de session partenaire (token GoTrue, ~1 h). */
async function setPartnerCookie(accessToken: string) {
  const cookieStore = await cookies();
  cookieStore.set(PARTNER_COOKIE, accessToken, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: 60 * 60, // 1 heure (durée du JWT GoTrue)
  });
}

/**
 * Auto-inscription d'un partenaire : crée le compte Supabase Auth (côté serveur,
 * email confirmé d'office) + la ligne `partners` liée, puis connecte.
 * Actif immédiatement, type `agent` par défaut, code auto PTN-XXXXX.
 */
export async function partnerSignUp(
  _prev: PartnerActionResult | undefined,
  formData: FormData
): Promise<PartnerActionResult> {
  const name = String(formData.get("name") || "").trim();
  const phone = String(formData.get("phone") || "").trim();
  const email = String(formData.get("email") || "").trim().toLowerCase();
  const password = String(formData.get("password") || "");

  if (!isConfigured()) return { error: "Service non configuré. Réessaie plus tard." };
  if (!name) return { error: "Ton nom est obligatoire." };
  if (!EMAIL_RE.test(email)) return { error: "Email invalide." };
  if (password.length < MIN_PASSWORD_LENGTH)
    return { error: `Mot de passe trop court (${MIN_PASSWORD_LENGTH} caractères minimum).` };

  // 1. Compte Supabase Auth (email confirmé — pas de dépendance au toggle signups).
  const { data: created, error: createError } = await supabaseAdmin().auth.admin.createUser({
    email,
    password,
    email_confirm: true,
  });
  if (createError || !created.user) {
    const msg = String(createError?.message || "").toLowerCase();
    if (msg.includes("already") || msg.includes("registered") || msg.includes("exist"))
      return { error: "Cet email a déjà un compte. Connecte-toi plutôt." };
    console.error("[portail] partnerSignUp createUser:", createError);
    return { error: "Impossible de créer le compte. Réessaie." };
  }
  const authUid = created.user.id;

  // 2. Ligne partenaire liée. Nouvel essai si collision de code (rare).
  let insertOk = false;
  for (let attempt = 0; attempt < 5 && !insertOk; attempt++) {
    const code = makeCode();
    const { error } = await supabaseAdmin().from("partners").insert({
      name,
      type: "agent",
      phone,
      email,
      code,
      status: "active",
      auth_uid: authUid,
    });
    if (!error) {
      insertOk = true;
      break;
    }
    if (error.code !== "23505") {
      console.error("[portail] partnerSignUp insert:", error);
      break;
    }
    // 23505 sur auth_uid = partenaire déjà créé pour ce compte → on sort proprement.
    if (String(error.message || "").includes("auth_uid")) {
      insertOk = true;
      break;
    }
  }
  if (!insertOk) {
    // Nettoyage : le compte auth existe mais pas de partenaire → on le supprime.
    await supabaseAdmin().auth.admin.deleteUser(authUid).catch(() => {});
    return { error: "Une erreur est survenue. Réessaie." };
  }

  // 3. Connexion immédiate.
  const { data: signIn, error: signInError } = await supabaseAuth().auth.signInWithPassword({ email, password });
  if (signInError || !signIn.session) {
    return { error: "Compte créé. Connecte-toi pour accéder à ton espace." };
  }
  await setPartnerCookie(signIn.session.access_token);

  redirect("/partenaire/espace");
}

/** Connexion d'un partenaire déjà inscrit. */
export async function partnerSignIn(
  _prev: PartnerActionResult | undefined,
  formData: FormData
): Promise<PartnerActionResult> {
  const email = String(formData.get("email") || "").trim().toLowerCase();
  const password = String(formData.get("password") || "");

  if (!isConfigured()) return { error: "Service non configuré. Réessaie plus tard." };
  if (!email || !password) return { error: "Email et mot de passe requis." };

  const { data, error } = await supabaseAuth().auth.signInWithPassword({ email, password });
  if (error || !data.session) return { error: "Email ou mot de passe incorrect." };

  // Vérifie qu'un partenaire est bien lié à ce compte.
  const { data: partner } = await supabaseAdmin()
    .from("partners")
    .select("id")
    .eq("auth_uid", data.user.id)
    .maybeSingle();
  if (!partner) {
    await supabaseAuth().auth.signOut();
    return { error: "Aucun espace partenaire lié à ce compte." };
  }

  await setPartnerCookie(data.session.access_token);
  redirect("/partenaire/espace");
}

/** Déconnexion du partenaire : supprime le cookie et renvoie à l'accueil du portail. */
export async function partnerSignOut() {
  const cookieStore = await cookies();
  cookieStore.delete(PARTNER_COOKIE);
  redirect("/partenaire");
}

/**
 * Demande de retrait par le partenaire connecté.
 * Montant libre, entier > 0 et ≤ disponible (recalculé côté serveur).
 */
export async function requestPayout(
  _prev: PartnerActionResult | undefined,
  formData: FormData
): Promise<PartnerActionResult> {
  const session = await requirePartner();
  const amount = Math.floor(Number(formData.get("amount") || 0));
  if (!Number.isFinite(amount) || amount <= 0) return { error: "Montant invalide." };

  // Disponible = commissions dues − retraits demandés non payés.
  const [salesRes, payoutsRes] = await Promise.all([
    supabaseAdmin().from("partner_sales").select("commission_fcfa").eq("partner_id", session.partnerId).eq("status", "owed"),
    supabaseAdmin().from("partner_payouts").select("amount_fcfa").eq("partner_id", session.partnerId).eq("status", "requested"),
  ]);
  const due = (salesRes.data ?? []).reduce((acc, s) => acc + Number(s.commission_fcfa || 0), 0);
  const requestedUnpaid = (payoutsRes.data ?? []).reduce((acc, p) => acc + Number(p.amount_fcfa || 0), 0);
  const available = Math.max(0, due - requestedUnpaid);

  if (available <= 0) return { error: "Aucun montant disponible au retrait." };
  if (amount > available) return { error: `Montant supérieur au disponible (${available.toLocaleString("fr-FR")} F).` };

  const { error } = await supabaseAdmin().from("partner_payouts").insert({
    partner_id: session.partnerId,
    amount_fcfa: amount,
    status: "requested",
    requested_at: Date.now(),
  });
  if (error) {
    console.error("[portail] requestPayout:", error);
    return { error: "Une erreur est survenue. Réessaie." };
  }

  revalidatePath("/partenaire/espace");
  return { ok: true };
}
