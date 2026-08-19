import { NextRequest } from "next/server";
import { activateAccountByEmail, geniuspayVerifySignature, type Plan } from "@/lib/payments";

// ============================================================
// POST /api/payments/webhook/geniuspay — confirmation GeniusPay.
// Vérifie la signature HMAC-SHA256 puis active le premium sur
// payment.success. Payload : data.metadata.email / .plan.
// ============================================================

export async function POST(req: NextRequest) {
  const rawBody = await req.text();
  const signature = req.headers.get("x-webhook-signature");
  const timestamp = req.headers.get("x-webhook-timestamp");
  const event = req.headers.get("x-webhook-event");

  if (!geniuspayVerifySignature(rawBody, signature, timestamp)) {
    return Response.json({ ok: false, error: "Signature invalide" }, { status: 401 });
  }

  let payload: any;
  try {
    payload = JSON.parse(rawBody);
  } catch {
    return Response.json({ ok: false, error: "Payload invalide" }, { status: 400 });
  }

  // Seul l'événement de succès déclenche l'activation.
  const isSuccess = event === "payment.success" || payload?.event === "payment.success";
  if (!isSuccess) {
    return Response.json({ ok: true, ignored: event ?? payload?.event ?? "unknown" });
  }

  const data = payload?.data ?? {};
  const email: string | undefined = data?.metadata?.email;
  const plan: Plan = data?.metadata?.plan === "business" ? "business" : "plus";
  if (!email) {
    return Response.json({ ok: false, error: "metadata.email manquant" }, { status: 400 });
  }

  const result = await activateAccountByEmail(email, plan);
  return Response.json({ ok: true, activated: result.activated, reason: result.reason });
}
