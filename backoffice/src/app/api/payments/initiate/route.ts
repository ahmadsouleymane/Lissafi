import { NextRequest } from "next/server";
import {
  corsHeaders,
  corsPreflight,
  geniuspayCreatePayment,
  ipaymoneyCreatePayment,
  isGeniuspayConfigured,
  isIpaymoneyConfigured,
  landingOrigin,
  planAmount,
  type Plan,
  type PaymentMethod,
} from "@/lib/payments";

// ============================================================
// POST /api/payments/initiate — crée un paiement côté serveur.
// Public (appelé par la landing en fetch, CORS restreint à la landing).
// Routage : carte → GeniusPay ; mobile money Niger → iPayMoney ;
// mobile money ailleurs → GeniusPay (checkout hébergé).
// ============================================================

type InitiateBody = {
  plan?: string;
  method?: string;
  country?: string;
  name?: string;
  email?: string;
  phone?: string;
};

export async function OPTIONS() {
  return corsPreflight();
}

export async function POST(req: NextRequest) {
  const headers = corsHeaders();

  let body: InitiateBody;
  try {
    body = (await req.json()) as InitiateBody;
  } catch {
    return Response.json({ ok: false, error: "Corps JSON invalide" }, { status: 400, headers });
  }

  const plan: Plan = body.plan === "business" ? "business" : "plus";
  const method: PaymentMethod = body.method === "card" ? "card" : "mobile_money";
  const country = (body.country || "").trim().toUpperCase();
  const name = (body.name || "").trim();
  const email = (body.email || "").trim().toLowerCase();
  const phone = (body.phone || "").trim();

  if (!name || !email || !phone) {
    return Response.json({ ok: false, error: "Nom, email et téléphone sont requis" }, { status: 400, headers });
  }
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
    return Response.json({ ok: false, error: "Email invalide" }, { status: 400, headers });
  }

  // Niger + mobile money → iPayMoney ; tout le reste → GeniusPay.
  const useIpaymoney = method === "mobile_money" && country === "NE";

  try {
    if (useIpaymoney) {
      if (!isIpaymoneyConfigured()) {
        return Response.json(
          { ok: false, error: "Paiement mobile money Niger indisponible pour le moment. Réessaie dans quelques jours ou contacte-nous sur WhatsApp." },
          { status: 503, headers },
        );
      }
      const msisdn = phone.replace(/\D/g, "");
      const { reference } = await ipaymoneyCreatePayment({
        amount: planAmount(plan),
        email,
        name,
        msisdn,
        country: "NE",
        plan,
      });
      return Response.json(
        { ok: true, mode: "push", provider: "ipaymoney", reference, plan },
        { headers },
      );
    }

    if (!isGeniuspayConfigured()) {
      return Response.json({ ok: false, error: "GeniusPay non configuré (clés manquantes)" }, { status: 503, headers });
    }
    const origin = landingOrigin();
    const { reference, checkoutUrl } = await geniuspayCreatePayment({
      amount: planAmount(plan),
      plan,
      email,
      name,
      phone,
      country: country || "CI",
      method,
      successUrl: `${origin}/paiement/succes`,
      errorUrl: `${origin}/paiement/echec`,
    });
    return Response.json(
      { ok: true, mode: "redirect", provider: "geniuspay", reference, redirectUrl: checkoutUrl, plan },
      { headers },
    );
  } catch (e) {
    const message = e instanceof Error ? e.message : "Erreur inconnue";
    return Response.json({ ok: false, error: message }, { status: 502, headers });
  }
}
