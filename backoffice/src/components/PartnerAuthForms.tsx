"use client";

import { useActionState } from "react";
import Link from "next/link";
import { partnerSignUp, partnerSignIn } from "@/actions/partners";
import type { PartnerActionResult } from "@/actions/partners";
import { Button, Field, Input, Spinner } from "./ui";

function ErrorNote({ state }: { state: PartnerActionResult | undefined }) {
  if (!state?.error) return null;
  return (
    <p className="rounded-lg bg-red-50 px-3 py-2 text-xs font-medium text-red-600 ring-1 ring-red-200">
      {state.error}
    </p>
  );
}

export function PartnerSignUpForm() {
  const [state, action, pending] = useActionState<PartnerActionResult | undefined, FormData>(partnerSignUp, undefined);
  return (
    <form action={action} className="space-y-4">
      <Field label="Ton nom complet">
        <Input type="text" name="name" required placeholder="Ex: Amadou Boubacar" autoComplete="name" />
      </Field>
      <Field label="Téléphone (Orange Money / Moov)" hint="Pour te payer tes commissions.">
        <Input type="tel" name="phone" placeholder="Ex: 90 11 22 33" autoComplete="tel" />
      </Field>
      <Field label="Email">
        <Input type="email" name="email" required placeholder="ton@email.com" autoComplete="email" />
      </Field>
      <Field label="Mot de passe" hint="8 caractères minimum.">
        <Input type="password" name="password" required placeholder="••••••••" autoComplete="new-password" minLength={8} />
      </Field>
      <ErrorNote state={state} />
      <Button type="submit" size="lg" disabled={pending} className="w-full">
        {pending ? <><Spinner /> Création…</> : "Créer mon compte"}
      </Button>
      <p className="text-center text-xs text-slate-500">
        Déjà inscrit ? <Link href="/partenaire/connexion" className="font-medium text-brand-600 hover:underline">Se connecter</Link>
      </p>
    </form>
  );
}

export function PartnerSignInForm() {
  const [state, action, pending] = useActionState<PartnerActionResult | undefined, FormData>(partnerSignIn, undefined);
  return (
    <form action={action} className="space-y-4">
      <Field label="Email">
        <Input type="email" name="email" required placeholder="ton@email.com" autoComplete="email" />
      </Field>
      <Field label="Mot de passe">
        <Input type="password" name="password" required placeholder="••••••••" autoComplete="current-password" />
      </Field>
      <ErrorNote state={state} />
      <Button type="submit" size="lg" disabled={pending} className="w-full">
        {pending ? <><Spinner /> Connexion…</> : "Se connecter"}
      </Button>
      <p className="text-center text-xs text-slate-500">
        Pas encore de compte ? <Link href="/partenaire/inscription" className="font-medium text-brand-600 hover:underline">Créer un compte</Link>
      </p>
    </form>
  );
}
