import { Badge, Card, EmptyState, PageHeader, Table, Td, Th, THead, Tr } from "@/components/ui";
import { getAuditLogs, getUserEmails } from "@/lib/data";
import { formatDateTimeIso } from "@/lib/format";

const EVENT_COLORS: Record<string, "green" | "blue" | "gray" | "red" | "orange"> = {
  login: "green",
  user_signed_up: "blue",
  signup: "blue",
  token_refreshed: "gray",
  logout: "gray",
  user_deleted: "red",
  login_failed: "red",
  user_recovery_requested: "orange",
};

const EVENT_LABELS: Record<string, string> = {
  login: "Connexion",
  user_signed_up: "Inscription",
  signup: "Inscription",
  token_refreshed: "Token renouvelé",
  logout: "Déconnexion",
  user_deleted: "Compte supprimé",
  login_failed: "Échec connexion",
  user_recovery_requested: "Mot de passe oublié",
};

export default async function ConnexionsPage({
  searchParams,
}: {
  searchParams: Promise<{ range?: string }>;
}) {
  const params = await searchParams;
  const range = params.range ?? "7d";
  const ranges: Record<string, number> = { "24h": 86400000, "7d": 7 * 86400000, "30d": 30 * 86400000, all: 0 };
  const fromTs = Date.now() - (ranges[range] ?? ranges["7d"]);

  const [audit, emails] = await Promise.all([getAuditLogs({ fromTs, limit: 400 }), getUserEmails()]);

  const summaries = audit.reduce<Record<string, { events: number; last: string }>>((acc, a) => {
    const key = a.user_id ?? "";
    if (!key) return acc;
    acc[key] = acc[key] ?? { events: 0, last: "" };
    acc[key].events++;
    if (!acc[key].last || a.created_at > acc[key].last) acc[key].last = a.created_at;
    return acc;
  }, {});

  return (
    <div className="space-y-5">
      <PageHeader
        title="Connexions"
        subtitle="Journal d'authentification Supabase (GoTrue)"
        action={
          <span className="rounded-lg bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-600">
            {audit.length} événements
          </span>
        }
      />

      <div className="grid gap-4 lg:grid-cols-3">
        {/* Colonne droite : tableau */}
        <div className="lg:col-span-2">
          <Card className="p-4">
            <form method="GET" action="/connexions" className="mb-4 flex items-center gap-2">
              <select name="range" defaultValue={range} className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
                <option value="24h">24 dernières heures</option>
                <option value="7d">7 derniers jours</option>
                <option value="30d">30 derniers jours</option>
                <option value="all">Tout</option>
              </select>
              <button type="submit" className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">
                Appliquer
              </button>
            </form>

            {audit.length === 0 ? (
              <EmptyState title="Aucun événement d'authentification" />
            ) : (
              <div className="max-h-[640px] overflow-auto">
                <Table>
                  <THead>
                    <Th>Date</Th>
                    <Th>Événement</Th>
                    <Th>Utilisateur</Th>
                    <Th>IP</Th>
                  </THead>
                  <tbody>
                    {audit.slice(0, 200).map((a, i) => (
                      <Tr key={i}>
                        <Td className="whitespace-nowrap text-xs text-slate-500">{formatDateTimeIso(a.created_at)}</Td>
                        <Td><Badge color={EVENT_COLORS[a.auth_event] ?? "gray"}>{EVENT_LABELS[a.auth_event] ?? a.auth_event}</Badge></Td>
                        <Td className="text-xs text-slate-600">{a.user_id ? emails[a.user_id] ?? a.user_id.slice(0, 8) : "—"}</Td>
                        <Td className="font-mono text-xs text-slate-500">{a.ip ?? "—"}</Td>
                      </Tr>
                    ))}
                  </tbody>
                </Table>
              </div>
            )}
          </Card>
        </div>

        {/* Colonne gauche : synthèse par compte */}
        <div>
          <Card>
            <div className="px-5 pt-4 pb-3">
              <h2 className="text-sm font-semibold text-slate-900">Activité par compte</h2>
              <p className="text-xs text-slate-500">Connexions sur la période</p>
            </div>
            <div className="max-h-[640px] space-y-1 overflow-auto px-5 pb-4">
              {Object.entries(summaries)
                .sort((a, b) => b[1].events - a[1].events)
                .slice(0, 30)
                .map(([uid, info]) => (
                  <div key={uid} className="flex items-center justify-between gap-2 rounded-lg px-2 py-1.5 hover:bg-slate-50">
                    <div className="min-w-0">
                      <p className="truncate text-xs font-medium text-slate-700">{emails[uid] || uid.slice(0, 8)}</p>
                      <p className="text-[11px] text-slate-400">dernier : {formatDateTimeIso(info.last)}</p>
                    </div>
                    <Badge color="blue">{info.events}</Badge>
                  </div>
                ))}
              {Object.keys(summaries).length === 0 && <EmptyState title="Aucune donnée" />}
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
