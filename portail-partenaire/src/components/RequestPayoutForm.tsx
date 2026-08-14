"use client";

import { useActionState } from "react";
import { requestPayout } from "@/actions/partners";
import type { PartnerActionResult } from "@/actions/partners";
import { Button, Card, Field, Input, Spinner } from "./ui";
import { formatFCFA } from "@/lib/format";

// Formulaire de demande de retrait — montant libre ≤ disponible.
export function RequestPayoutForm({ available }: { available: number }) {
  const [state, action, pending] = useActionState<PartnerActionResult | undefined, FormData>(requestPayout, undefined);

  return (
    <Card className="p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold text-slate-900">Demander un retrait</h2>
          <p className="mt-0.5 text-xs text-slate-500">Payé en mobile money après validation.</p>
        </div>
        <div className="text-right">
          <p className="text-xs text-slate-500">Disponible</p>
          <p className="text-lg font-bold text-brand-600">{formatFCFA(available)}</p>
        </div>
      </div>

      {available <= 0 ? (
        <p className="mt-4 rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-500">
          Rien à retirer pour l'instant. Reviens quand tes commissions seront confirmées.
        </p>
      ) : (
        <form action={action} className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end">
          <div className="flex-1">
            <Field label="Montant (FCFA)">
              <Input type="number" name="amount" min={1} max={available} step={100} placeholder={String(available)} required />
            </Field>
          </div>
          <Button type="submit" disabled={pending} className="sm:mb-0">
            {pending ? <><Spinner /> Envoi…</> : "Demander"}
          </Button>
        </form>
      )}

      {state?.ok && (
        <p className="mt-3 rounded-lg bg-brand-50 px-3 py-2 text-xs font-medium text-brand-700 ring-1 ring-brand-200">
          Demande envoyée. Tu seras payé après validation.
        </p>
      )}
      {state?.error && (
        <p className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-xs font-medium text-red-600 ring-1 ring-red-200">
          {state.error}
        </p>
      )}
    </Card>
  );
}
