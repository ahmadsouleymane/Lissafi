import { createClient } from "@supabase/supabase-js";

// ============================================================
// Clients Supabase — TOUS utilisés côté serveur uniquement.
// Le navigateur n'a jamais accès à ces clés.
// ============================================================

export const DEFAULT_URL = "https://fnyuhpfzkvunscuylvqv.supabase.co";

export function getSupabaseUrl(): string {
  return process.env.SUPABASE_URL?.trim() || DEFAULT_URL;
}

export function getAnonKey(): string {
  return process.env.SUPABASE_ANON_KEY?.trim() || "";
}

export function getServiceKey(): string {
  return process.env.SUPABASE_SERVICE_ROLE_KEY?.trim() || "";
}

/** Le back-office est opérationnel si les 2 clés sont renseignées. */
export function isConfigured(): boolean {
  return Boolean(getAnonKey() && getServiceKey());
}

/**
 * Client "auth" — connexion admin (GoTrue) avec la clé anon.
 */
export function supabaseAuth() {
  return createClient(getSupabaseUrl(), getAnonKey() || "sb-empty", {
    auth: { persistSession: false, autoRefreshToken: false },
  });
}

/**
 * Client "admin" — accès total aux données (clé service_role).
 * Bypasse la RLS : réservé au code serveur du back-office.
 */
export function supabaseAdmin() {
  return createClient(getSupabaseUrl(), getServiceKey() || "sb-empty", {
    auth: { persistSession: false, autoRefreshToken: false },
  });
}
