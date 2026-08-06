import Link from "next/link";
import { Badge, Card, EmptyState, PageHeader, Table, Td, Th, THead, Tr } from "@/components/ui";
import { PriorityBadge, TicketStatusBadge } from "@/components/badges";
import { getTickets } from "@/lib/data";
import { formatDate } from "@/lib/format";

export default async function SupportPage({
  searchParams,
}: {
  searchParams: Promise<{ status?: string; priority?: string }>;
}) {
  const params = await searchParams;
  const status = params.status ?? "all";
  const priority = params.priority ?? "all";

  let tickets = await getTickets(status === "all" ? undefined : status);
  if (priority !== "all") tickets = tickets.filter((t) => t.priority === priority);

  const counts = {
    open: tickets.filter((t) => t.status === "open").length,
    in_progress: tickets.filter((t) => t.status === "in_progress").length,
    resolved: tickets.filter((t) => t.status === "resolved").length,
    closed: tickets.filter((t) => t.status === "closed").length,
  };

  return (
    <div className="space-y-5">
      <PageHeader
        title="Support"
        subtitle="Signalements et demandes envoyés depuis l'application"
        action={
          <span className="flex items-center gap-2 rounded-lg bg-red-50 px-3 py-1.5 text-xs font-medium text-red-600">
            {counts.open + counts.in_progress} à traiter
          </span>
        }
      />

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <Stat label="Ouverts" value={counts.open} color="text-sky-600" />
        <Stat label="En cours" value={counts.in_progress} color="text-orange-500" />
        <Stat label="Résolus" value={counts.resolved} color="text-brand-600" />
        <Stat label="Clos" value={counts.closed} color="text-slate-500" />
      </div>

      <Card className="p-4">
        <form method="GET" action="/support" className="flex gap-3">
          <select name="status" defaultValue={status} className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
            <option value="all">Tous les statuts</option>
            <option value="open">Ouverts</option>
            <option value="in_progress">En cours</option>
            <option value="resolved">Résolus</option>
            <option value="closed">Clos</option>
          </select>
          <select name="priority" defaultValue={priority} className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
            <option value="all">Toutes priorités</option>
            <option value="urgent">Urgente</option>
            <option value="high">Haute</option>
            <option value="normal">Normale</option>
            <option value="low">Basse</option>
          </select>
          <button type="submit" className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">
            Filtrer
          </button>
        </form>
      </Card>

      {tickets.length === 0 ? (
        <EmptyState title="Aucun ticket" subtitle="Les signalements apparaîtront ici dès que les utilisateurs les envoient." />
      ) : (
        <Table>
          <THead>
            <Th>#</Th>
            <Th>Sujet</Th>
            <Th>Utilisateur</Th>
            <Th>Priorité</Th>
            <Th>Statut</Th>
            <Th>Reçu le</Th>
            <Th className="text-right">Action</Th>
          </THead>
          <tbody>
            {tickets.map((t) => (
              <Tr key={t.id}>
                <Td className="text-xs text-slate-400">#{t.id}</Td>
                <Td className="max-w-64">
                  <p className="truncate font-medium text-slate-800">{t.subject || "Sans sujet"}</p>
                  <p className="truncate text-xs text-slate-400">{t.message}</p>
                </Td>
                <Td>
                  <Link href={`/comptes/${t.user_id}`} className="text-xs text-brand-600 hover:underline">
                    {t.email || t.user_id.slice(0, 8)}
                  </Link>
                </Td>
                <Td><PriorityBadge priority={t.priority} /></Td>
                <Td><TicketStatusBadge status={t.status} /></Td>
                <Td className="whitespace-nowrap text-xs text-slate-500">{formatDate(t.created_at)}</Td>
                <Td className="text-right">
                  <Link href={`/support/${t.id}`} className="text-sm font-medium text-brand-600 hover:underline">Ouvrir</Link>
                </Td>
              </Tr>
            ))}
          </tbody>
        </Table>
      )}
    </div>
  );
}

function Stat({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <Card className="p-4">
      <p className="text-xs text-slate-500">{label}</p>
      <p className={`text-2xl font-bold ${color}`}>{value}</p>
    </Card>
  );
}
