import { Badge, Card, EmptyState, PageHeader, Table, Td, Th, THead, Tr } from "@/components/ui";
import { EventTypeLabel, LevelBadge } from "@/components/badges";
import { getLogs } from "@/lib/data";
import { formatDate, timeAgo } from "@/lib/format";
import type { AppLog } from "@/types";

export default async function LogsPage({
  searchParams,
}: {
  searchParams: Promise<{ level?: string; event_type?: string; range?: string; q?: string }>;
}) {
  const params = await searchParams;
  const level = params.level ?? "";
  const eventType = params.event_type ?? "";
  const range = params.range ?? "7d";
  const q = (params.q ?? "").toLowerCase().trim();

  const now = Date.now();
  const ranges: Record<string, number> = { "24h": 86400000, "7d": 7 * 86400000, "30d": 30 * 86400000, all: 0 };
  const fromTs = ranges[range] ?? ranges["7d"];

  let logs = await getLogs({ fromTs, level, eventType, limit: 300 });
  if (q) logs = logs.filter((l) => `${l.email ?? ""} ${l.message} ${l.event_type}`.toLowerCase().includes(q));

  const eventTypes = [...new Set(logs.map((l) => l.event_type))].sort();

  return (
    <div className="space-y-5">
      <PageHeader title="Erreurs & activité" subtitle={`${logs.length} événement${logs.length > 1 ? "s" : ""} · journal de l'application`} />

      <Card className="p-4">
        <form method="GET" action="/logs" className="flex flex-col gap-3 sm:flex-row sm:flex-wrap">
          <input
            name="q"
            type="search"
            defaultValue={q}
            placeholder="Filtrer par email, message…"
            className="min-w-40 flex-1 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/40"
          />
          <select name="level" defaultValue={level} className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
            <option value="">Tous niveaux</option>
            <option value="error">Erreurs</option>
            <option value="warn">Avertissements</option>
            <option value="info">Info</option>
            <option value="debug">Debug</option>
          </select>
          <select name="event_type" defaultValue={eventType} className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
            <option value="">Tous les types</option>
            {eventTypes.map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
          <select name="range" defaultValue={range} className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
            <option value="24h">24 dernières heures</option>
            <option value="7d">7 derniers jours</option>
            <option value="30d">30 derniers jours</option>
            <option value="all">Tout</option>
          </select>
          <button type="submit" className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">
            Filtrer
          </button>
        </form>
      </Card>

      {logs.length === 0 ? (
        <EmptyState title="Aucun événement" subtitle="Les logs arriveront quand l'app sera en ligne et utilisée." />
      ) : (
        <Table>
          <THead>
            <Th>Quand</Th>
            <Th>Utilisateur</Th>
            <Th>Type</Th>
            <Th>Niveau</Th>
            <Th>Message</Th>
            <Th>Détails</Th>
          </THead>
          <tbody>
            {logs.map((log) => (
              <LogRowView key={log.id} log={log} />
            ))}
          </tbody>
        </Table>
      )}
    </div>
  );
}

function LogRowView({ log }: { log: AppLog }) {
  let metaShort = "";
  if (log.meta && log.meta !== "{}") {
    try {
      const parsed = JSON.parse(log.meta) as Record<string, unknown>;
      const line = Object.entries(parsed)
        .map(([k, v]) => `${k}=${typeof v === "object" ? JSON.stringify(v) : v}`)
        .join(" · ");
      metaShort = line.slice(0, 120);
    } catch {
      metaShort = log.meta.slice(0, 120);
    }
  }

  return (
    <Tr>
      <Td className="whitespace-nowrap text-xs text-slate-500">
        <p>{timeAgo(log.created_at)}</p>
        <p className="text-slate-400">{formatDate(log.created_at)}</p>
      </Td>
      <Td className="text-xs text-slate-600">{log.email || log.user_id.slice(0, 8) || "—"}</Td>
      <Td><span className="text-xs text-slate-600"><EventTypeLabel type={log.event_type} /></span></Td>
      <Td><LevelBadge level={log.level} /></Td>
      <Td className="max-w-64"><p className="truncate text-slate-800" title={log.message}>{log.message || "—"}</p></Td>
      <Td className="max-w-64">{metaShort ? <p className="truncate font-mono text-[11px] text-slate-400" title={metaShort}>{metaShort}</p> : <Badge color="gray">—</Badge>}</Td>
    </Tr>
  );
}
