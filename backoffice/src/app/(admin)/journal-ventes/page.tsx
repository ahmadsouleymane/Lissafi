import { Badge, Card, EmptyState, PageHeader, Table, Td, Th, THead, Tr } from "@/components/ui";
import { getSaleAuditLog, type SaleAuditRow } from "@/lib/data";
import { formatDate, timeAgo } from "@/lib/format";

export const dynamic = "force-dynamic";

const ACTION_META: Record<string, { label: string; color: "green" | "amber" | "red" | "gray" }> = {
  created: { label: "Créée", color: "green" },
  modified: { label: "Modifiée", color: "amber" },
  cancelled: { label: "Annulée", color: "red" },
};

export default async function JournalVentesPage({
  searchParams,
}: {
  searchParams: Promise<{ action?: string; range?: string; q?: string }>;
}) {
  const params = await searchParams;
  const action = params.action ?? "changes";
  const range = params.range ?? "30d";
  const q = (params.q ?? "").toLowerCase().trim();

  const ranges: Record<string, number> = { "7d": 7 * 86400000, "30d": 30 * 86400000, "90d": 90 * 86400000, all: 0 };
  const fromTs = ranges[range] ?? ranges["30d"];

  let rows = await getSaleAuditLog({ action, fromTs, limit: 500 });
  if (q) rows = rows.filter((r) => `${r.email ?? ""} ${r.details} ${r.sale_id}`.toLowerCase().includes(q));

  const nbModif = rows.filter((r) => r.action === "modified").length;
  const nbAnnul = rows.filter((r) => r.action === "cancelled").length;

  return (
    <div className="space-y-5">
      <PageHeader
        title="Journal des ventes"
        subtitle={`${rows.length} opération${rows.length > 1 ? "s" : ""} · ${nbModif} modification${nbModif > 1 ? "s" : ""}, ${nbAnnul} annulation${nbAnnul > 1 ? "s" : ""} — trace inaltérable des retouches de vente`}
      />

      <Card className="p-4">
        <form method="GET" action="/journal-ventes" className="flex flex-col gap-3 sm:flex-row sm:flex-wrap">
          <input
            name="q"
            type="search"
            defaultValue={params.q ?? ""}
            placeholder="Filtrer par email, motif, N° de vente…"
            className="min-w-40 flex-1 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/40"
          />
          <select name="action" defaultValue={action} className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
            <option value="changes">Modifs &amp; annulations</option>
            <option value="modified">Modifications</option>
            <option value="cancelled">Annulations</option>
            <option value="created">Créations</option>
            <option value="all">Tout</option>
          </select>
          <select name="range" defaultValue={range} className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
            <option value="7d">7 derniers jours</option>
            <option value="30d">30 derniers jours</option>
            <option value="90d">90 derniers jours</option>
            <option value="all">Tout</option>
          </select>
          <button type="submit" className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">
            Filtrer
          </button>
        </form>
      </Card>

      {rows.length === 0 ? (
        <EmptyState
          title="Aucune opération"
          subtitle="Les modifications et annulations de ventes apparaîtront ici dès qu'elles se produiront dans l'app."
        />
      ) : (
        <Table>
          <THead>
            <Th>Quand</Th>
            <Th>Compte</Th>
            <Th>Action</Th>
            <Th>Vente</Th>
            <Th>Détail</Th>
          </THead>
          <tbody>
            {rows.map((r) => (
              <AuditRowView key={r.id} row={r} />
            ))}
          </tbody>
        </Table>
      )}
    </div>
  );
}

function AuditRowView({ row }: { row: SaleAuditRow }) {
  const meta = ACTION_META[row.action] ?? { label: row.action, color: "gray" as const };
  return (
    <Tr>
      <Td className="whitespace-nowrap text-xs text-slate-500">
        <p>{timeAgo(row.date)}</p>
        <p className="text-slate-400">{formatDate(row.date)}</p>
      </Td>
      <Td className="text-xs text-slate-600">{row.email || (row.user_id ? row.user_id.slice(0, 8) : "—")}</Td>
      <Td><Badge color={meta.color}>{meta.label}</Badge></Td>
      <Td className="text-xs font-mono text-slate-500">N°{row.sale_id}</Td>
      <Td className="max-w-80"><p className="truncate text-slate-800" title={row.details}>{row.details || "—"}</p></Td>
    </Tr>
  );
}
