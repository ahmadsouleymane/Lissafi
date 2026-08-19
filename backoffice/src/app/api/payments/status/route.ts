import { NextRequest } from "next/server";
import {
  activateAccountByEmail,
  corsHeaders,
  corsPreflight,
  geniuspayGetStatus,
  ipaymoneyGetStatus,
} from "@/lib/payments";

// ============================================================
// GET /api/payments/status?provider=&reference=
// Public (polling de la landing). Au statut terminal "succès", active le
// premium du compte (idempotent) — c'est la voie fiable tant que le webhook
// n'est pas configuré.
// ============================================================

export async function OPTIONS() {
  return corsPreflight();
}

export async function GET(req: NextRequest) {
  const headers = corsHeaders();
  const provider = req.nextUrl.searchParams.get("provider");
  const reference = req.nextUrl.searchParams.get("reference");

  if (!provider || !reference) {
    return Response.json({ ok: false, error: "provider et reference sont requis" }, { status: 400, headers });
  }

  try {
    const result =
      provider === "ipaymoney"
        ? await ipaymoneyGetStatus(reference)
        : await geniuspayGetStatus(reference);

    let activated = false;
    let reason: string | undefined;
    const isTerminal =
      result.status === "succeeded" || result.status === "completed";
    if (isTerminal && result.email && result.plan) {
      const res = await activateAccountByEmail(result.email, result.plan);
      activated = res.activated;
      reason = res.reason;
    }

    return Response.json(
      { ok: true, status: result.status, activated, reason, plan: result.plan },
      { headers },
    );
  } catch (e) {
    const message = e instanceof Error ? e.message : "Erreur inconnue";
    return Response.json({ ok: false, error: message }, { status: 502, headers });
  }
}
