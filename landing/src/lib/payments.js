import { PAYMENTS_API_URL } from "../config";

// ============================================================
// Client de paiement — la landing appelle le back-office, qui détient les
// clés secrètes iPayMoney/GeniusPay. Aucune clé ne transite ici.
// ============================================================

/** Initialise un paiement côté serveur. Retourne {mode, provider, reference, redirectUrl}. */
export async function initiatePayment(payload) {
  const res = await fetch(`${PAYMENTS_API_URL}/api/payments/initiate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok || !data.ok) {
    throw new Error(data.error || "Impossible d'initialiser le paiement.");
  }
  return data;
}

const TERMINAL = new Set(["completed", "succeeded", "failed", "expired", "cancelled"]);

/**
 * Interroge le statut jusqu'à un état terminal. Utilisé après un paiement
 * push (iPayMoney) ou au retour de la page checkout (GeniusPay).
 * Résout avec {ok, status, activated, plan}.
 */
export async function pollStatus(provider, reference, { intervalMs = 3000, maxTries = 40, onTick } = {}) {
  for (let i = 0; i < maxTries; i++) {
    const res = await fetch(
      `${PAYMENTS_API_URL}/api/payments/status?provider=${encodeURIComponent(provider)}&reference=${encodeURIComponent(reference)}`,
    );
    const data = await res.json().catch(() => ({}));
    if (data.ok && data.status) {
      onTick?.(data);
      if (TERMINAL.has(data.status)) return data;
    }
    await new Promise((r) => setTimeout(r, intervalMs));
  }
  return { ok: false, status: "timeout" };
}
