import Link from "next/link";
import { notFound } from "next/navigation";
import { deleteTicketQuick, updateTicketStatusQuick } from "@/actions/admin";
import { ConfirmForm } from "@/components/ConfirmForm";
import { ReplyBox } from "@/components/ReplyBox";
import { Badge, Button, Card, CardHeader } from "@/components/ui";
import { PriorityBadge, TicketStatusBadge } from "@/components/badges";
import { getTicket, getTicketReplies } from "@/lib/data";
import { formatDate } from "@/lib/format";

export const dynamic = "force-dynamic";

const STATUSES = ["open", "in_progress", "resolved", "closed"] as const;

export default async function TicketDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const ticketId = Number(id);
  if (!Number.isFinite(ticketId)) notFound();

  const [ticket, replies] = await Promise.all([getTicket(ticketId), getTicketReplies(ticketId)]);
  if (!ticket) notFound();

  return (
    <div className="mx-auto max-w-3xl space-y-5">
      <Link href="/support" className="text-sm text-slate-500 hover:text-brand-600 hover:underline">← Retour au support</Link>

      <Card className="p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-lg font-bold text-slate-900">{ticket.subject || "Sans sujet"}</h1>
              <TicketStatusBadge status={ticket.status} />
              <PriorityBadge priority={ticket.priority} />
            </div>
            <p className="mt-1 text-sm text-slate-500">
              De{" "}
              <Link href={`/comptes/${ticket.user_id}`} className="font-medium text-brand-600 hover:underline">
                {ticket.email || ticket.user_id.slice(0, 8)}
              </Link>{" "}
              · reçu le {formatDate(ticket.created_at)}
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-1.5">
            {STATUSES.filter((s) => s !== ticket.status).map((s) => (
              <form key={s} action={updateTicketStatusQuick.bind(null, ticketId, s)}>
                <Button type="submit" size="sm" variant="secondary">→ {s.replace("_", " ")}</Button>
              </form>
            ))}
            <ConfirmForm action={deleteTicketQuick.bind(null, ticketId)} confirmText="Supprimer définitivement ce ticket et ses réponses ?">
              <Button type="submit" size="sm" variant="dangerOutline">Supprimer</Button>
            </ConfirmForm>
          </div>
        </div>
      </Card>

      {/* Message initial */}
      <Card className="p-5">
        <p className="whitespace-pre-wrap text-sm text-slate-800">{ticket.message || "Aucun message."}</p>
        <p className="mt-3 text-xs text-slate-400">{formatDate(ticket.created_at)}</p>
      </Card>

      {/* Fil de discussion */}
      {replies.length > 0 && (
        <div className="space-y-3">
          {replies.map((r) => (
            <Card key={r.id} className={r.is_admin ? "border-brand-200 bg-brand-50/40" : ""}>
              <div className="p-4">
                <div className="mb-1.5 flex items-center justify-between">
                  <Badge color={r.is_admin ? "green" : "gray"}>{r.is_admin ? "Admin" : "Utilisateur"}</Badge>
                  <span className="text-xs text-slate-400">{formatDate(r.created_at)}</span>
                </div>
                <p className="whitespace-pre-wrap text-sm text-slate-800">{r.message}</p>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Boîte de réponse */}
      <Card>
        <CardHeader title="Répondre" subtitle="La réponse partira en message interne (pas encore de notification WhatsApp auto)" />
        <div className="px-5 pb-5">
          <ReplyBox ticketId={ticketId} />
        </div>
      </Card>
    </div>
  );
}
