"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { isConfigured, supabaseAdmin, supabaseAuth } from "@/lib/supabase";
import { ADMIN_COOKIE } from "@/lib/session";

export type LoginState = { error?: string } | undefined;

// Rate-limit basique en mémoire : max 5 tentatives par email, reset après 15 min.
const loginAttempts = new Map<string, { count: number; resetAt: number }>();
const MAX_ATTEMPTS = 5;
const RESET_MS = 15 * 60 * 1000; // 15 minutes

function checkRateLimit(email: string): boolean {
  const now = Date.now();
  const entry = loginAttempts.get(email);
  if (!entry || now > entry.resetAt) {
    loginAttempts.set(email, { count: 1, resetAt: now + RESET_MS });
    return true;
  }
  if (entry.count >= MAX_ATTEMPTS) return false;
  entry.count++;
  return true;
}

// Nettoyage périodique des entrées expirées (toutes les 100 tentatives)
if (loginAttempts.size > 100) {
  const now = Date.now();
  for (const [k, v] of loginAttempts) { if (now > v.resetAt) loginAttempts.delete(k); }
}

/**
 * Connexion au back-office.
 * - Authentifie l'email/mot de passe via GoTrue (clé anon)
 * - Vérifie que le compte est bien dans la table `admins`
 * - Pose un cookie httpOnly avec le token d'accès
 */
export async function login(_prev: LoginState, formData: FormData): Promise<LoginState> {
  const email = String(formData.get("email") || "").trim().toLowerCase();
  const password = String(formData.get("password") || "");

  if (!email || !password) return { error: "Email et mot de passe requis." };
  if (!isConfigured()) return { error: "Back-office non configuré — renseigne les variables d'environnement (voir .env.example)." };
  if (!checkRateLimit(email)) return { error: "Trop de tentatives. Réessaie dans 15 minutes." };

  const auth = supabaseAuth().auth;
  const { data, error } = await auth.signInWithPassword({ email, password });
  if (error || !data.session) return { error: "Email ou mot de passe incorrect." };

  const { data: adminRow, error: adminError } = await supabaseAdmin()
    .from("admins")
    .select("user_id")
    .eq("user_id", data.user.id)
    .maybeSingle();

  if (adminError || !adminRow) {
    await auth.signOut();
    return { error: "Ce compte n'a pas accès au back-office." };
  }

  // Le token GoTrue expire après ~1 h, pas de refresh stocké.
  // Le cookie est gardé 1 h pour correspondre au TTL réel du JWT.
  const cookieStore = await cookies();
  cookieStore.set(ADMIN_COOKIE, data.session.access_token, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: 60 * 60, // 1 heure (durée du JWT GoTrue)
  });

  redirect("/");
}

/** Déconnexion : supprime le cookie puis renvoie vers /login. */
export async function logout() {
  const cookieStore = await cookies();
  cookieStore.delete(ADMIN_COOKIE);
  redirect("/login");
}
