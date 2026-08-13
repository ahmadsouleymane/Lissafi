"use client";

import { useActionState } from "react";
import { addPartnerSale } from "@/actions/partners";
import type { PartnerActionResult } from "@/actions/partners";
import { COMMISSIONS, PLAN_LABELS } from "@/lib/partners";
import type { Plan } from "@/types";
import { Button, Card, CardHeader, Field, Input, Select, Textarea } from "./ui";

function Feedback({ state }: { state: PartnerActionResult | undefined }) {
  if (!state) return null;
  if (state.ok)
    return <p className="rounded-lg bg-brand-50 px-3 py-2 text-xs font-medium text-brand-700 ring-1 ring-brand-200">Vente attribuée — commission enregistrée.</p>;
  if (state.error)
    return <p className="rounded-lg bg-red-50 px-3 py-2 text-xs font-medium text-red-600 ring-1 ring-red-200">{state.error}</p>;
  return null;
}

export function AddPartnerSaleForm({ partnerId }: { partnerId: string }) {
  const [state, action, pending] = useActionState<PartnerActionResult | undefined, FormData>(addPartnerSale, undefined);

  return (
    <Card>
      <CardHeader
        title="Attribuer une vente"
        subtitle="Le client a payé et a été activé — enregistre la commission due au partenaire."
      />
      <form action={action} className="grid gap-4 px-5 pb-5 sm:grid-cols-2">
        <input type="hidden" name="partnerId" value={partnerId} />
        <Field label="Plan vendu">
          <Select name="plan" defaultValue="plus">
            {(Object.keys(PLAN_LABELS) as Plan[]).map((p) => (
              <option key={p} value={p}>
                {PLAN_LABELS[p]} — commission {COMMISSIONS[p].toLocaleString("fr-FR")} F
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Montant payé (FCFA)">
          <Input type="number" name="amountPaid" min={0} step={100} placeholder="Ex: 30000" />
        </Field>
        <Field label="Client — nom">
          <Input type="text" name="clientName" placeholder="Ex: Boutique Awa" />
        </Field>
        <Field label="Client — téléphone">
          <Input type="tel" name="clientPhone" placeholder="Ex: 90 11 22 33" />
        </Field>
        <Field label="Note">
          <Textarea name="note" placeholder="Ex: activé avec le code PTN-AMADOU" />
        </Field>
        <div className="flex flex-col items-start justify-end gap-2 sm:col-span-2">
          <Feedback state={state} />
          <Button type="submit" disabled={pending}>Enregistrer la vente</Button>
        </div>
      </form>
    </Card>
  );
}
