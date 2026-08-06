import Link from "next/link";
import { BarChart, LineChart, ChartPoint } from "@/components/charts";
import { Badge, Card, CardHeader, EmptyState, PageHeader, StatCard } from "@/components/ui";
import { LevelBadge, TicketStatusBadge } from "@/components/badges";
import {
  IconActivity,
  IconAlert,
  IconBox,
  IconChat,
  IconCrown,
  IconReceipt,
  IconTrend,
  IconUsers,
} from "@/components/icons";
import { getLogs, getSalesSeries, getSignupsSeries, getStats, getTickets } from "@/lib/data";
import { formatFCFA, formatDate, toNumber } from "@/lib/format";
import { isConfigured } from "@/lib/supabase";

function dayLabel(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleDateString("fr-FR", { day: "2-digit", month: "2-digit" });
}

export default async function DashboardPage() {
  const configured = isConfigured();
  const [stats, salesSeries, signupsSeries, logs, tickets] = await Promise.all([
    getStats(),
    getSalesSeries(30),
    getSignupsSeries(30),
    getLogs({ limit: 8 }),
    getTickets(),
  ]);

  const salesPoints: ChartPoint[] = salesSeries.map((s) => ({
    label: dayLabel(s.day),
    value: toNumber(s.total),
  }));

  const signupPoints: ChartPoint[] = signupsSeries.map((s) => ({
    label: dayLabel(s.day),
    value: toNumber(s.signups),
  }));

  if (!configured) {
    return (
      <div>
        <PageHeader title="Tableau de bord" subtitle="Vue d'ensemble de Lissafi" />
        <Card className="mt-6 p-6">
          <h2 className="text-base font-bold text-slate-900">Configuration requise</h2>
          <p className="mt-2 text-sm text-slate-600">
            Le back-office n'est pas encore relié à Supabase. Copie <code className="rounded bg-slate-100 px-1">.env.example</code> vers{" "}
            <code className="rounded bg-slate-100 px-1">.env</code> et renseigne les clés{" "}
            <code className="rounded bg-slate-100 px-1">SUPABASE_ANON_KEY</code> et{" "}
            <code className="rounded bg-slate-100 px-1">SUPABASE_SERVICE_ROLE_KEY</code> (Supabase → Settings → API). Puis exécute{" "}
            <code className="rounded bg-slate-100 px-1">supabase-admin.sql</code> dans le SQL Editor et relance <code className="rounded bg-slate-100 px-1">npm run dev</code>.
          </p>
        </Card>
      </div>
    );
  }

  const noMigration = stats.users === undefined;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Tableau de bord"
        subtitle={new Intl.DateTimeFormat("fr-FR", { weekday: "long", day: "numeric", month: "long", year: "numeric" }).format(new Date())}
      />

      {noMigration && (
        <Card className="border-amber-200 bg-amber-50 p-4">
          <p className="text-sm text-amber-800">
            <strong>Migration non appliquée.</strong> Exécute le script <code className="rounded bg-amber-100 px-1">supabase-admin.sql</code> dans le SQL
            Editor Supabase pour activer les statistiques et le suivi.
          </p>
        </Card>
      )}

      {/* Cartes statistiques */}
      <div className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-4">
        <StatCard label="Comptes" value={formatNumber(stats.users)} sub={`${formatNumber(stats.users_today)} aujourd'hui · ${formatNumber(stats.active_today)} actifs`} icon={<IconUsers size={18} />} />
        <StatCard label="Premium" value={formatNumber(stats.premium)} sub={`${formatNumber(stats.demo)} démos · ${formatNumber(stats.premium_expiring_30d)} expirent ≤ 30 j`} icon={<IconCrown size={18} />} accent="orange" />
        <StatCard label="Ventes" value={formatNumber(stats.sales)} sub={`${formatNumber(stats.sales_today)} aujourd'hui`} icon={<IconTrend size={18} />} accent="blue" />
        <StatCard label="Reçus" value={formatNumber(stats.receipts)} sub="tickets émis (WhatsApp/impression)" icon={<IconReceipt size={18} />} />
        <StatCard label="Chiffre cumulé" value={formatFCFA(toNumber(stats.sales_total_fcfa))} sub={`Aujourd'hui : ${formatFCFA(toNumber(stats.sales_today_fcfa))}`} icon={<IconActivity size={18} />} />
        <StatCard label="Produits" value={formatNumber(stats.products)} sub={`${formatNumber(stats.clients)} clients suivis`} icon={<IconBox size={18} />} accent="gray" />
        <StatCard label="Erreurs (24 h)" value={formatNumber(stats.errors_24h)} sub="app_logs niveau erreur" icon={<IconAlert size={18} />} accent="red" />
        <StatCard label="Tickets ouverts" value={formatNumber(stats.tickets_open)} sub="demandes de support en cours" icon={<IconChat size={18} />} accent="blue" />
      </div>

      {/* Graphiques */}
      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader title="Ventes — 30 derniers jours" subtitle="Total par jour (FCFA)" />
          <div className="px-5 pb-5">
            {salesPoints.some((p) => p.value > 0) ? (
              <BarChart data={salesPoints} formatValue={formatFCFA} />
            ) : (
              <EmptyState title="Aucune vente sur la période" />
            )}
          </div>
        </Card>
        <Card>
          <CardHeader title="Inscriptions — 30 derniers jours" subtitle="Nouveaux comptes par jour" />
          <div className="px-5 pb-5">
            {signupPoints.some((p) => p.value > 0) ? (
              <LineChart data={signupPoints} />
            ) : (
              <EmptyState title="Aucune inscription sur la période" />
            )}
          </div>
        </Card>
      </div>

      {/* Activité récente */}
      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader
            title="Activité récente"
            subtitle="Derniers événements remontés par l'app"
            action={<Link href="/logs" className="text-xs font-medium text-brand-600 hover:underline">Tout voir</Link>}
          />
          <div className="divide-y divide-slate-100 px-5 pb-3">
            {logs.length === 0 && <EmptyState title="Aucun événement" subtitle="Les logs arriveront dès que l'app sera en ligne" />}
            {logs.slice(0, 6).map((log) => (
              <div key={log.id} className="flex items-center justify-between gap-3 py-2.5">
                <div className="min-w-0">
                  <p className="truncate text-sm text-slate-800">{log.message || log.event_type}</p>
                  <p className="truncate text-xs text-slate-400">
                    {log.email || "—"} · {formatDate(log.created_at)}
                  </p>
                </div>
                <LevelBadge level={log.level} />
              </div>
            ))}
          </div>
        </Card>

        <Card>
          <CardHeader
            title="Tickets de support"
            subtitle="Derniers signalements"
            action={<Link href="/support" className="text-xs font-medium text-brand-600 hover:underline">Tout voir</Link>}
          />
          <div className="divide-y divide-slate-100 px-5 pb-3">
            {tickets.length === 0 && <EmptyState title="Aucun ticket" subtitle="Les signalements envoyés depuis l'app apparaîtront ici" />}
            {tickets.slice(0, 5).map((t) => (
              <div key={t.id} className="flex items-center justify-between gap-3 py-2.5">
                <div className="min-w-0">
                  <p className="truncate text-sm text-slate-800">{t.subject || "Signalement"}</p>
                  <p className="truncate text-xs text-slate-400">{t.email || "—"} · {formatDate(t.created_at)}</p>
                </div>
                <TicketStatusBadge status={t.status} />
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}

function formatNumber(n: number | undefined): string {
  return new Intl.NumberFormat("fr-FR").format(toNumber(n));
}
