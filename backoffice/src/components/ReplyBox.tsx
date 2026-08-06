"use client";

import { useActionState } from "react";
import { replyToTicketForm, ActionResult } from "@/actions/admin";
import { Button, Spinner, Textarea } from "./ui";

export function ReplyBox({ ticketId }: { ticketId: number }) {
  const [state, action, pending] = useActionState<ActionResult | undefined, FormData>(replyToTicketForm, undefined);

  return (
    <form action={action} className="space-y-2">
      <input type="hidden" name="ticketId" value={ticketId} />
      <Textarea name="message" required placeholder="Rédiger une réponse… (le ticket passe en « résolu »)" />
      {state?.error && <p className="text-xs font-medium text-red-600">{state.error}</p>}
      {state?.ok && <p className="text-xs font-medium text-brand-600">Réponse envoyée.</p>}
      <Button type="submit" disabled={pending}>
        {pending && <Spinner />} Envoyer la réponse
      </Button>
    </form>
  );
}
