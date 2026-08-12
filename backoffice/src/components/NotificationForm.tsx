"use client";

import { useActionState, useMemo, useState } from "react";
import { sendManualNotificationForm } from "@/actions/notifications";
import type { ActionResult } from "@/actions/admin";
import type { NotificationAccount } from "@/types";
import { Button, Card, CardHeader, Field, Input, Select, Spinner, Textarea } from "./ui";

function Feedback({ state }: { state: ActionResult | undefined }) {
  if (!state) return null;
  if (state.ok) return <p className="rounded-lg bg-brand-50 px-3 py-2 text-xs font-medium text-brand-700 ring-1 ring-brand-200">Notification envoyée.</p>;
  if (state.error) return <p className="rounded-lg bg-red-50 px-3 py-2 text-xs font-medium text-red-600 ring-1 ring-red-200">{state.error}</p>;
  return null;
}

export function NotificationForm({ accounts }: { accounts: NotificationAccount[] }) {
  const [state, action, pending] = useActionState<ActionResult | undefined, FormData>(sendManualNotificationForm, undefined);
  const [mode, setMode] = useState<"status" | "accounts">("status");
  const [query, setQuery] = useState("");

  const filtered = useMemo(() => {
    const q = query.toLowerCase().trim();
    if (!q) return accounts;
    return accounts.filter((a) => `${a.email ?? ""} ${a.shop_name} ${a.shop_phone}`.toLowerCase().includes(q));
  }, [accounts, query]);

  return (
    <Card>
      <CardHeader title="Envoi manuel" subtitle="Titre + message envoyés en push à l'app Android" />
      <form action={action} className="space-y-4 px-5 pb-5">
        <Field label="Titre">
          <Input type="text" name="title" required maxLength={80} placeholder="Ex: Nouvelle fonctionnalité disponible" />
        </Field>
        <Field label="Message">
          <Textarea name="body" required maxLength={500} placeholder="Le contenu de la notification…" />
        </Field>

        <div>
          <span className="mb-1 block text-xs font-medium text-slate-600">Destinataires</span>
          <div className="flex gap-4 text-sm text-slate-700">
            <label className="flex items-center gap-1.5">
              <input type="radio" name="mode" value="status" checked={mode === "status"} onChange={() => setMode("status")} />
              Par statut
            </label>
            <label className="flex items-center gap-1.5">
              <input type="radio" name="mode" value="accounts" checked={mode === "accounts"} onChange={() => setMode("accounts")} />
              Comptes précis
            </label>
          </div>
        </div>

        {mode === "status" ? (
          <Field label="Statut ciblé">
            <Select name="status" defaultValue="all">
              <option value="all">Tous les comptes</option>
              <option value="premium">Premium actif</option>
              <option value="free">Gratuit</option>
              <option value="expired">Premium expiré</option>
            </Select>
          </Field>
        ) : (
          <div>
            <Input
              type="search"
              placeholder="Rechercher par boutique, email ou téléphone…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="mb-2"
            />
            <div className="max-h-56 overflow-y-auto rounded-lg border border-slate-200">
              {filtered.length === 0 && <p className="px-3 py-4 text-center text-xs text-slate-400">Aucun compte trouvé.</p>}
              {filtered.map((a) => (
                <label key={a.user_id} className="flex items-center gap-2 border-b border-slate-100 px-3 py-2 text-sm last:border-0 hover:bg-slate-50">
                  <input type="checkbox" name="userIds" value={a.user_id} />
                  <span className="truncate">
                    {a.shop_name || "Boutique sans nom"} — {a.email || "—"}
                  </span>
                </label>
              ))}
            </div>
          </div>
        )}

        <Feedback state={state} />
        <Button type="submit" disabled={pending}>
          {pending && <Spinner />} Envoyer la notification
        </Button>
      </form>
    </Card>
  );
}
