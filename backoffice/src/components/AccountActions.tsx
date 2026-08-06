"use client";

import { useActionState } from "react";
import {
  activatePremiumQuick,
  activatePremiumWithDays,
  addPremiumDaysQuick,
  deactivatePremiumQuick,
  resetUserPasswordForm,
  ActionResult,
} from "@/actions/admin";
import { Button, Card, CardHeader, Field, Input, Spinner } from "./ui";
import { ConfirmForm } from "./ConfirmForm";

/** Petite alerte de résultat (succès/erreur) d'une action serveur. */
function Feedback({ state }: { state: ActionResult | undefined }) {
  if (!state) return null;
  if (state.ok) return <p className="rounded-lg bg-brand-50 px-3 py-2 text-xs font-medium text-brand-700 ring-1 ring-brand-200">Action effectuée.</p>;
  if (state.error) return <p className="rounded-lg bg-red-50 px-3 py-2 text-xs font-medium text-red-600 ring-1 ring-red-200">{state.error}</p>;
  return null;
}

export function AccountActions({ userId }: { userId: string }) {
  const [activateState, activateAction, activatePending] = useActionState<ActionResult | undefined, FormData>(activatePremiumWithDays, undefined);
  const [resetState, resetAction, resetPending] = useActionState<ActionResult | undefined, FormData>(resetUserPasswordForm, undefined);

  return (
    <Card>
      <CardHeader title="Gestion Premium" subtitle="Activation manuelle (flux WhatsApp → ici)" />

      <div className="space-y-4 px-5 pb-5">
        {/* Activation / prolongation rapide */}
        <div className="flex flex-wrap gap-2">
          <form action={activatePremiumQuick.bind(null, userId, 365)}>
            <Button type="submit" size="sm">Activer 1 an</Button>
          </form>
          <form action={activatePremiumQuick.bind(null, userId, 30)}>
            <Button type="submit" size="sm" variant="secondary">Activer 30 j</Button>
          </form>
          <form action={addPremiumDaysQuick.bind(null, userId, 30)}>
            <Button type="submit" size="sm" variant="secondary">+30 j</Button>
          </form>
          <form action={addPremiumDaysQuick.bind(null, userId, 90)}>
            <Button type="submit" size="sm" variant="secondary">+90 j</Button>
          </form>
          <ConfirmForm action={deactivatePremiumQuick.bind(null, userId)} confirmText="Désactiver le premium de ce compte ?" className="ml-auto">
            <Button type="submit" size="sm" variant="dangerOutline">Désactiver</Button>
          </ConfirmForm>
        </div>

        {/* Activation avec durée personnalisée */}
        <form action={activateAction} className="flex items-end gap-2">
          <input type="hidden" name="userId" value={userId} />
          <Field label="Durée personnalisée">
            <div className="flex items-center gap-2">
              <Input type="number" name="days" min={1} max={3650} defaultValue={365} className="w-28" />
              <span className="text-xs text-slate-500">jours</span>
              <Button type="submit" disabled={activatePending} size="sm">
                {activatePending && <Spinner />} Activer
              </Button>
            </div>
          </Field>
        </form>
        <Feedback state={activateState} />

        {/* Réinitialisation mot de passe */}
        <div className="border-t border-slate-100 pt-4">
          <p className="mb-2 text-xs font-semibold text-slate-600">Réinitialiser le mot de passe (l'utilisateur devra se reconnecter)</p>
          <form action={resetAction} className="flex items-end gap-2">
            <input type="hidden" name="userId" value={userId} />
            <Field label="Nouveau mot de passe">
              <Input type="password" name="password" minLength={6} placeholder="min. 6 caractères" className="w-56" />
            </Field>
            <Button type="submit" disabled={resetPending} variant="secondary" size="sm">
              {resetPending && <Spinner />} Appliquer
            </Button>
          </form>
          <Feedback state={resetState} />
        </div>
      </div>
    </Card>
  );
}
