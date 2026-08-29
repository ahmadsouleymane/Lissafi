import { NextRequest } from "next/server";
import {
  corsHeaders,
  corsPreflight,
  geniuspayCreatePayment,
  isGeniuspayConfigured,
  landingOrigin,
  planAmount,
  type Plan,
  type PaymentMethod,
} from "@/lib/payments";

// ============================================================
// POST /api/payments/initiate — crée un paiement côté serveur.
// Public (appelé par la landing en fetch, CORS restreint à la landing).
// Carte ET mobile money passent par GeniusPay (checkout hébergé).
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

  try {
    if (!isGeniuspayConfigured()) {
      return Response.json(
        { ok: false, error: "Paiement en ligne indisponible pour le moment. Réessaie plus tard ou active ton compte via WhatsApp." },
        { status: 503, headers },
      );
    }
    const origin = landingOrigin();
    const { reference, checkoutUrl } = await geniuspayCreatePayment({
      amount: planAmount(plan),
      plan,
      email,
      name,
      phone,
      country: country || "NE",
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
