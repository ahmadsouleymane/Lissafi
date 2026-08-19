import { NextRequest } from "next/server";
import {
  activateAccountByEmail,
  decodePaymentToken,
  ipaymoneyVerifySecret,
} from "@/lib/payments";

// ============================================================
// POST /api/payments/webhook/ipaymoney — confirmation iPayMoney.
// Vérifie le header `secret-hash` (== clé privée) puis active le premium
// quand data.status == "succeeded". data.external_reference = notre
// transaction_id (token signé contenant email + plan).
// ============================================================

export async function POST(req: NextRequest) {
  const secretHash = req.headers.get("secret-hash");
  if (!ipaymoneyVerifySecret(secretHash)) {
    return Response.json({ ok: false, error: "Secret invalide" }, { status: 401 });
  }

  let payload: any;
  try {
    payload = await req.json();
  } catch {
    return Response.json({ ok: false, error: "Payload invalide" }, { status: 400 });
  }

  const data = payload?.data ?? {};
  if (data.status !== "succeeded") {
    return Response.json({ ok: true, ignored: data.status ?? "unknown" });
  }

  const token = data.external_reference ? decodePaymentToken(data.external_reference) : null;
  if (!token) {
    return Response.json({ ok: false, error: "external_reference invalide" }, { status: 400 });
  }

  const result = await activateAccountByEmail(token.email, token.plan);
  return Response.json({ ok: true, activated: result.activated, reason: result.reason });
}
