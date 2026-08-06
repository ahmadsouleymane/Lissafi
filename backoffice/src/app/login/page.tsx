"use client";

import { useActionState } from "react";
import { login, LoginState } from "@/actions/auth";
import { Button, Field, Input, Spinner } from "@/components/ui";
import { Logo } from "@/components/Logo";

export default function LoginPage() {
  const [state, formAction, pending] = useActionState<LoginState, FormData>(login, undefined);

  return (
    <div className="flex min-h-screen items-center justify-center bg-brand-700 px-4">
      <div className="w-full max-w-sm">
        <div className="mb-6 flex justify-center">
          <span className="flex items-center gap-3">
            <svg width="44" height="44" viewBox="0 0 48 48" fill="none" aria-hidden>
              <rect width="48" height="48" rx="10" fill="white" />
              <path
                d="M12 14h3l2.5 14a3 3 0 0 0 3 2.5h12.2a3 3 0 0 0 3-2.4l2.3-11.1H15.6"
                stroke="#2e8b57"
                strokeWidth="2.6"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              <circle cx="20.5" cy="35" r="2.1" fill="#2e8b57" />
              <circle cx="31.5" cy="35" r="2.1" fill="#2e8b57" />
              <path d="M30 18.5l2.5 2.5L38 15" stroke="#2e8b57" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            <span className="text-2xl font-bold text-white">
              Lissafi<span className="text-white/80"> Admin</span>
            </span>
          </span>
        </div>

        <div className="rounded-2xl bg-white p-6 shadow-xl">
          <h1 className="text-lg font-bold text-slate-900">Connexion</h1>
          <p className="mt-0.5 text-sm text-slate-500">Espace réservé à l'administration.</p>

          <form action={formAction} className="mt-5 space-y-4">
            <Field label="Email">
              <Input type="email" name="email" placeholder="admin@lissafi.app" autoComplete="email" required />
            </Field>
            <Field label="Mot de passe">
              <Input type="password" name="password" placeholder="••••••••" autoComplete="current-password" required />
            </Field>

            {state?.error && (
              <p className="rounded-lg bg-red-50 px-3 py-2 text-xs font-medium text-red-600 ring-1 ring-red-200">{state.error}</p>
            )}

            <Button type="submit" disabled={pending} className="w-full" size="lg">
              {pending ? (
                <>
                  <Spinner /> Connexion…
                </>
              ) : (
                "Se connecter"
              )}
            </Button>
          </form>
        </div>

        <p className="mt-4 text-center text-xs text-white/70">
          Back-office Lissafi · activation premium, statistiques, support
        </p>
      </div>
    </div>
  );
}
