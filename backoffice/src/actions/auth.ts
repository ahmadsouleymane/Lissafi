"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { isConfigured, supabaseAdmin, supabaseAuth } from "@/lib/supabase";
import { ADMIN_COOKIE } from "@/lib/session";

export type LoginState = { error?: string } | undefined;

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

  const cookieStore = await cookies();
  cookieStore.set(ADMIN_COOKIE, data.session.access_token, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: 60 * 60 * 24 * 7, // 7 jours
  });

  redirect("/");
}

/** Déconnexion : supprime le cookie puis renvoie vers /login. */
export async function logout() {
  const cookieStore = await cookies();
  cookieStore.delete(ADMIN_COOKIE);
  redirect("/login");
}
