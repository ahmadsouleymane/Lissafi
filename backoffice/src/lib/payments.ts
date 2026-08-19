import "server-only";
import { createHmac, timingSafeEqual } from "crypto";
import { supabaseAdmin } from "./supabase";

// ============================================================
// PAIEMENT EN LIGNE — iPayMoney (Niger) + GeniusPay (reste de l'Afrique).
//
// Les clés secrètes ne vivent QU'ICI (côté serveur). La landing (statique)
// appelle les routes /api/payments/* qui délèguent à ce module. Après un
// paiement réussi, on active le premium directement sur le compte Lissafi
// identifié par son email (même mécanisme que activatePremium, mais sans
// session admin — voir guard_premium_keys : service_role passe toujours).
// ============================================================

const GENIUSPAY_BASE = "https://geniuspay.ci/api/v1/merchant";
const IPAYMONEY_BASE = "https://i-pay.money/api/v1";

const DAY_MS = 86400000;
const PREMIUM_DAYS = 365;

export type Plan = "plus" | "business";
export type PaymentMethod = "mobile_money" | "card";

// Prix alignés sur la landing (Pricing.jsx) : 30 000 F Plus / 75 000 F Business.
const PLAN_PRICES: Record<Plan, number> = { plus: 30000, business: 75000 };

export function planAmount(plan: Plan): number {
  return PLAN_PRICES[plan];
}

// ------------------------------------------------------------
// CORS — la landing (domaine distinct) appelle initiate/status en fetch.
// ------------------------------------------------------------
export function landingOrigin(): string {
  // URL publique de la landing — sert au CORS et aux retours success/error.
  // Var DÉDIÉE : NEXT_PUBLIC_LANDING_URL a une autre sémantique ailleurs et peut
  // pointer vers un autre domaine — ne pas en dépendre pour les paiements.
  return (process.env.PAYMENTS_LANDING_URL || "https://lissafi-one.vercel.app").replace(/\/+$/, "");
}

export function corsHeaders(): Record<string, string> {
  return {
    "Access-Control-Allow-Origin": landingOrigin(),
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type",
    "Access-Control-Max-Age": "86400",
  };
}

/** Réponse OPTIONS (preflight) pour les routes publiques. */
export function corsPreflight(): Response {
  return new Response(null, { status: 204, headers: corsHeaders() });
}

// ------------------------------------------------------------
// Token signé pour le transaction_id iPayMoney (stateless).
// iPayMoney renvoie ce transaction_id en `external_reference` dans ses
// webhooks/réponses — on y encode email + plan pour résoudre le compte.
// ------------------------------------------------------------
function signingSecret(): string {
  return (
    process.env.PAYMENTS_SIGNING_SECRET ||
    process.env.IPAYMONEY_PRIVATE_KEY ||
    "lissafi-dev-insecure"
  );
}

function sign(data: string): string {
  return createHmac("sha256", signingSecret()).update(data).digest("hex");
}

export function encodePaymentToken(email: string, plan: Plan): string {
  const payload = Buffer.from(JSON.stringify({ email, plan })).toString("base64url");
  return `${payload}.${sign(payload)}`;
}

export function decodePaymentToken(token: string): { email: string; plan: Plan } | null {
  const [payload, sig] = token.split(".");
  if (!payload || !sig) return null;
  const expected = sign(payload);
  const a = Buffer.from(sig);
  const b = Buffer.from(expected);
  if (a.length !== b.length || !timingSafeEqual(a, b)) return null;
  try {
    const parsed = JSON.parse(Buffer.from(payload, "base64url").toString());
    if (typeof parsed.email === "string" && (parsed.plan === "plus" || parsed.plan === "business")) {
      return { email: parsed.email, plan: parsed.plan };
    }
    return null;
  } catch {
    return null;
  }
}

// ------------------------------------------------------------
// GéniusPay
// ------------------------------------------------------------
function geniuspayEnv(): { key: string; secret: string } {
  return {
    key: (process.env.GENIUSPAY_API_KEY || "").trim(),
    secret: (process.env.GENIUSPAY_API_SECRET || "").trim(),
  };
}

export function isGeniuspayConfigured(): boolean {
  const { key, secret } = geniuspayEnv();
  return Boolean(key && secret);
}

export type GeniuspayCreateResult = {
  reference: string;
  checkoutUrl: string;
  status: string;
};

/** Crée un paiement GeniusPay (checkout hébergé) et renvoie l'URL de redirection. */
export async function geniuspayCreatePayment(params: {
  amount: number;
  plan: Plan;
  email: string;
  name: string;
  phone: string;
  country: string;
  method: PaymentMethod;
  successUrl: string;
  errorUrl: string;
}): Promise<GeniuspayCreateResult> {
  const { key, secret } = geniuspayEnv();
  const body: Record<string, unknown> = {
    amount: params.amount,
    currency: "XOF",
    description: `Lissafi ${params.plan === "business" ? "Business" : "Plus"} — 1 an`,
    customer: {
      name: params.name,
      email: params.email,
      phone: params.phone,
      country: params.country,
    },
    success_url: params.successUrl,
    error_url: params.errorUrl,
    metadata: { email: params.email, plan: params.plan },
  };
  // Carte → gateway explicite "card" ; mobile money hors Niger → checkout hébergé
  // (le client choisit Wave/Orange/MTN/Moov). Laisser le champ absent maximise la
  // conversion (recommandé par GeniusPay).
  if (params.method === "card") body.payment_method = "card";

  const res = await fetch(`${GENIUSPAY_BASE}/payments`, {
    method: "POST",
    headers: {
      "X-API-Key": key,
      "X-API-Secret": secret,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  const json = (await res.json().catch(() => ({}))) as any;

  if (!res.ok || !json?.success) {
    const code = json?.error?.code || `HTTP_${res.status}`;
    const msg = json?.error?.message || `Échec GeniusPay (${res.status})`;
    throw new Error(`${code}: ${msg}`);
  }
  const data = json.data;
  return {
    reference: data.reference,
    checkoutUrl: data.checkout_url || data.payment_url,
    status: data.status,
  };
}

/** Statut d'un paiement GeniusPay par référence. */
export async function geniuspayGetStatus(reference: string): Promise<{
  status: string;
  email?: string;
  plan?: Plan;
}> {
  const { key, secret } = geniuspayEnv();
  const res = await fetch(`${GENIUSPAY_BASE}/payments/${encodeURIComponent(reference)}`, {
    headers: { "X-API-Key": key, "X-API-Secret": secret },
  });
  const json = (await res.json().catch(() => ({}))) as any;
  if (!res.ok || !json?.success) {
    throw new Error(json?.error?.message || `Statut GeniusPay indisponible (${res.status})`);
  }
  const data = json.data;
  return {
    status: data.status,
    email: data.metadata?.email,
    plan: data.metadata?.plan === "business" ? "business" : data.metadata?.plan === "plus" ? "plus" : undefined,
  };
}

/** Vérifie la signature HMAC-SHA256 d'un webhook GeniusPay. */
export function geniuspayVerifySignature(rawBody: string, signature: string | null, timestamp: string | null): boolean {
  const secret = (process.env.GENIUSPAY_WEBHOOK_SECRET || "").trim();
  if (!secret || !signature || !timestamp) return false;
  // Rejeu : timestamp trop ancien (> 5 min).
  if (Math.abs(Date.now() / 1000 - Number(timestamp)) > 300) return false;
  const expected = createHmac("sha256", secret).update(`${timestamp}.${rawBody}`).digest("hex");
  const a = Buffer.from(signature);
  const b = Buffer.from(expected);
  return a.length === b.length && timingSafeEqual(a, b);
}

// ------------------------------------------------------------
// iPayMoney (Niger)
// ------------------------------------------------------------

// Gel volontaire : pas encore de clés live iPayMoney (seulement sandbox).
// Empêche toute initiation même si des clés sandbox traînent dans l'env —
// on ne veut pas qu'un vrai client déclenche un paiement sandbox qui ne
// débouche sur rien. Repasser à `false` quand les clés live sont en place.
const IPAYMONEY_FROZEN = true;

function ipaymoneyEnv(): { privateKey: string; environment: string } {
  return {
    privateKey: (process.env.IPAYMONEY_PRIVATE_KEY || "").trim(),
    environment: (process.env.IPAYMONEY_ENVIRONMENT || "sandbox").trim(),
  };
}

export function isIpaymoneyConfigured(): boolean {
  return !IPAYMONEY_FROZEN && Boolean(ipaymoneyEnv().privateKey);
}

/** Crée un paiement iPayMoney (push-to-phone) et renvoie sa référence. */
export async function ipaymoneyCreatePayment(params: {
  amount: number;
  email: string;
  name: string;
  msisdn: string;
  country: string;
  plan: Plan;
}): Promise<{ reference: string; status: string }> {
  const { privateKey, environment } = ipaymoneyEnv();
  const transactionId = encodePaymentToken(params.email, params.plan);

  const res = await fetch(`${IPAYMONEY_BASE}/payments`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${privateKey}`,
      "Ipay-Payment-Type": "mobile",
      "Ipay-Target-Environment": environment,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      customer_name: params.name,
      currency: "XOF",
      country: params.country,
      amount: String(params.amount),
      transaction_id: transactionId,
      msisdn: params.msisdn,
    }),
  });
  const json = (await res.json().catch(() => ({}))) as any;

  if (res.status === 422 || json?.status === "failed") {
    throw new Error(json?.message || "Référence non valide");
  }
  if (!res.ok || (json?.status !== "succeeded" && json?.status !== "pending")) {
    throw new Error(json?.message || `Échec iPayMoney (${res.status})`);
  }
  return { reference: json.reference, status: json.status };
}

/** Statut d'un paiement iPayMoney par référence (retourne email/plan depuis le token). */
export async function ipaymoneyGetStatus(reference: string): Promise<{
  status: string;
  email?: string;
  plan?: Plan;
}> {
  const { privateKey, environment } = ipaymoneyEnv();
  const res = await fetch(`${IPAYMONEY_BASE}/payments/${encodeURIComponent(reference)}`, {
    headers: {
      Authorization: `Bearer ${privateKey}`,
      "Ipay-Payment-Type": "mobile",
      "Ipay-Target-Environment": environment,
      "Content-Type": "application/json",
    },
  });
  const json = (await res.json().catch(() => ({}))) as any;
  if (!res.ok) {
    throw new Error(json?.message || `Statut iPayMoney indisponible (${res.status})`);
  }
  const token = json.external_reference ? decodePaymentToken(json.external_reference) : null;
  return {
    status: json.status,
    email: token?.email,
    plan: token?.plan,
  };
}

/** Vérifie le header `secret-hash` d'un webhook iPayMoney (== clé privée). */
export function ipaymoneyVerifySecret(secretHash: string | null): boolean {
  const { privateKey } = ipaymoneyEnv();
  if (!privateKey || !secretHash) return false;
  const a = Buffer.from(secretHash);
  const b = Buffer.from(privateKey);
  return a.length === b.length && timingSafeEqual(a, b);
}

// ------------------------------------------------------------
// Activation premium par email (service_role — sans session admin).
// ------------------------------------------------------------

export type ActivationResult = { activated: boolean; userId?: string; reason?: string };

/** Résout un email vers un user_id Supabase (via admin_user_emails, service_role). */
async function lookupUserByEmail(email: string): Promise<string | null> {
  const normalized = email.trim().toLowerCase();
  const { data, error } = await supabaseAdmin().rpc("admin_user_emails");
  if (error) {
    console.error("[payments] admin_user_emails:", error);
    return null;
  }
  const row = (data ?? []).find(
    (r: { user_id: string; email: string }) => (r.email || "").toLowerCase() === normalized,
  );
  return row?.user_id ?? null;
}

/** Active le premium (1 an) sur le compte identifié par email. Idempotent. */
export async function activateAccountByEmail(email: string, plan: Plan): Promise<ActivationResult> {
  const userId = await lookupUserByEmail(email);
  if (!userId) {
    return { activated: false, reason: "account_not_found" };
  }

  const expiry = Date.now() + PREMIUM_DAYS * DAY_MS;
  const cleanPlan = plan === "business" ? "business" : "plus";

  const { error } = await supabaseAdmin().from("app_settings").upsert(
    [
      { key: "is_premium", value: "true", user_id: userId },
      { key: "plan", value: cleanPlan, user_id: userId },
      { key: "premium_expiry", value: String(expiry), user_id: userId },
      { key: "demo_taken", value: "true", user_id: userId },
    ],
    { onConflict: "key,user_id" },
  );
  if (error) {
    console.error("[payments] activation premium:", error);
    return { activated: false, userId, reason: "activation_failed" };
  }

  // Commission partenaire si le compte a un partner_code (jamais bloquant).
  try {
    await supabaseAdmin().rpc("attribute_partner_sale", {
      p_user_id: userId,
      p_plan: cleanPlan,
    });
  } catch (e) {
    console.error("[payments] attribute_partner_sale:", e);
  }

  return { activated: true, userId };
}
