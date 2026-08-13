"use client";

import { useActionState } from "react";
import { addPartner } from "@/actions/partners";
import type { PartnerActionResult } from "@/actions/partners";
import { PARTNER_TYPE_LABELS } from "@/lib/partners";
import { Button, Card, CardHeader, Field, Input, Select, Textarea } from "./ui";

function Feedback({ state }: { state: PartnerActionResult | undefined }) {
  if (!state) return null;
  if (state.ok)
    return <p className="rounded-lg bg-brand-50 px-3 py-2 text-xs font-medium text-brand-700 ring-1 ring-brand-200">Partenaire ajouté.</p>;
  if (state.error)
    return <p className="rounded-lg bg-red-50 px-3 py-2 text-xs font-medium text-red-600 ring-1 ring-red-200">{state.error}</p>;
  return null;
}

export function AddPartnerForm() {
  const [state, action, pending] = useActionState<PartnerActionResult | undefined, FormData>(addPartner, undefined);

  return (
    <Card>
      <CardHeader
        title="Ajouter un partenaire"
        subtitle="Code unique à communiquer — commission en espèces à chaque client payant amené."
      />
      <form action={action} className="grid gap-4 px-5 pb-5 sm:grid-cols-2">
        <Field label="Nom">
          <Input type="text" name="name" required placeholder="Ex: Amadou Boubacar" />
        </Field>
        <Field label="Type">
          <Select name="type" defaultValue="agent">
            {Object.entries(PARTNER_TYPE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </Select>
        </Field>
        <Field label="Téléphone">
          <Input type="tel" name="phone" placeholder="Ex: 96 12 34 56" />
        </Field>
        <Field label="Code (optionnel)" hint="Vide = généré automatiquement (ex: PTN-K2M7Q).">
          <Input type="text" name="code" placeholder="Ex: PTN-AMADOU" />
        </Field>
        <Field label="Notes">
          <Textarea name="notes" placeholder="Boutique, quartier, accord…" />
        </Field>
        <div className="flex flex-col items-start justify-end gap-2 sm:col-span-2">
          <Feedback state={state} />
          <Button type="submit" disabled={pending}>Ajouter le partenaire</Button>
        </div>
      </form>
    </Card>
  );
}
