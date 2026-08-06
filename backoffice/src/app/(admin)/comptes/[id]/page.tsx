import Link from "next/link";
import { notFound } from "next/navigation";
import { AccountActions } from "@/components/AccountActions";
import { Badge, Card, CardHeader, EmptyState, Table, Td, Th, THead, Tr } from "@/components/ui";
import { LevelBadge, PremiumBadge } from "@/components/badges";
import { IconMail, IconPhone } from "@/components/icons";
import { getUserDetail } from "@/lib/data";
import { formatDate, formatFCFA, formatDateTimeIso } from "@/lib/format";
import type { AppLog, SaleRow } from "@/types";

export const dynamic = "force-dynamic";

export default async function CompteDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const detail = await getUserDetail(id);
  if (!detail.summary) notFound();

  const u = detail.summary;
  const initials = (u.shop_name || u.email || "?").charAt(0).toUpperCase();
  const waPhone = u.shop_phone.replace(/\s+/g, "");

  return (
    <div className="space-y-5">
      <Link href="/comptes" className="text-sm text-slate-500 hover:text-brand-600 hover:underline">← Retour aux comptes</Link>

      {/* Identité */}
      <Card className="p-5">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex items-center gap-4">
            <span className="flex h-14 w-14 items-center justify-center rounded-full bg-brand-50 text-2xl font-bold text-brand-700">
              {initials}
            </span>
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="text-xl font-bold text-slate-900">{u.shop_name || "Boutique sans nom"}</h1>
                <PremiumBadge premium={u.premium} expiry={u.premium_expiry} />
                {u.demo_taken && <Badge color="blue">Démo utilisée</Badge>}
              </div>
              <div className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-slate-500">
                <span className="flex items-center gap-1.5"><IconMail size={14} /> {u.email || "—"}</span>
                <span className="flex items-center gap-1.5"><IconPhone size={14} /> {u.shop_phone || "—"}</span>
              </div>
            </div>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            {waPhone && (
              <a
                href={`https://wa.me/${waPhone}?text=${encodeURIComponent("Bonjour ! Je te contacte au sujet de Lissafi.")}`}
                target="_blank"
                rel="noopener noreferrer"
                className="rounded-lg bg-[#25D366] px-3.5 py-2 text-sm font-medium text-white hover:opacity-90"
              >
                WhatsApp
              </a>
            )}
            <span className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs text-slate-500">
              Inscrit le {formatDateTimeIso(u.created_at)}
            </span>
          </div>
        </div>
      </Card>

      {/* Mini stats */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        <MiniStat label="Produits" value={u.product_count} />
        <MiniStat label="Clients" value={detail.clientCount} />
        <MiniStat label="Ventes" value={u.sale_count} />
        <MiniStat label="Chiffre" value={formatFCFA(u.sales_total)} highlight />
        <MiniStat label="Reçus" value={u.receipt_count} />
        <MiniStat label="Dettes clients" value={formatFCFA(detail.clientDebtTotal)} />
      </div>

      <div className="grid gap-5 lg:grid-cols-3">
        {/* Colonne gauche */}
        <div className="space-y-5">
          <AccountActions userId={id} />

          <Card>
            <CardHeader title="Informations" subtitle="Paramètres stockés dans l'app" />
            <dl className="space-y-2 px-5 pb-5 text-sm">
              <InfoRow label="Nom boutique" value={detail.settings.shop_name || "—"} />
              <InfoRow label="Téléphone boutique" value={detail.settings.shop_phone || "—"} />
              <InfoRow label="Premium jusqu'au" value={u.premium_expiry ? formatDate(u.premium_expiry) : "—"} />
              <InfoRow label="Code d'activation" value={detail.settings.activation_code || "—"} />
              <InfoRow label="Démo utilisée" value={detail.settings.demo_taken === "true" ? "Oui" : "Non"} />
              <InfoRow label="Dernière synchro" value={detail.settings.last_sync_timestamp ? formatDate(Number(detail.settings.last_sync_timestamp)) : "—"} />
              <InfoRow label="PIN admin (local)" value={detail.settings.admin_pin ? "••••" : "—"} />
            </dl>
          </Card>

          {detail.actions.length > 0 && (
            <Card>
              <CardHeader title="Historique admin" subtitle="Actions effectuées sur ce compte" />
              <div className="space-y-2 px-5 pb-5">
                {detail.actions.map((a) => (
                  <div key={a.id} className="flex items-center justify-between gap-2 text-xs">
                    <span className="font-medium text-slate-600">{a.action}</span>
                    <span className="whitespace-nowrap text-slate-400">{formatDateTimeIso(a.created_at)}</span>
                  </div>
                ))}
              </div>
            </Card>
          )}
        </div>

        {/* Colonne droite */}
        <div className="space-y-5 lg:col-span-2">
          <Card>
            <CardHeader title="Ventes récentes" subtitle="100 dernières ventes" />
            {detail.sales.length === 0 ? (
              <div className="px-5 pb-5"><EmptyState title="Aucune vente" /></div>
            ) : (
              <div className="px-5 pb-4">
                <Table>
                  <THead>
                    <Th>Date</Th>
                    <Th className="text-right">Total</Th>
                    <Th className="text-right">Payé</Th>
                    <Th className="text-right">Monnaie</Th>
                    <Th>Type</Th>
                    <Th>Client</Th>
                  </THead>
                  <tbody>
                    {detail.sales.map((s) => <SaleRowView key={s.id} s={s} />)}
                  </tbody>
                </Table>
              </div>
            )}
          </Card>

          <Card>
            <CardHeader title="Activité (logs)" subtitle="Événements remontés par l'app" />
            {detail.logs.length === 0 ? (
              <div className="px-5 pb-5"><EmptyState title="Aucun événement" /></div>
            ) : (
              <div className="divide-y divide-slate-100 px-5 pb-3">
                {detail.logs.map((log) => <LogRow key={log.id} log={log} />)}
              </div>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}

function MiniStat({ label, value, highlight }: { label: string; value: number | string; highlight?: boolean }) {
  return (
    <Card className="p-3">
      <p className="text-[11px] font-medium text-slate-500">{label}</p>
      <p className={`text-lg font-bold leading-tight ${highlight ? "text-brand-600" : "text-slate-900"}`}>{value}</p>
    </Card>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <dt className="text-slate-500">{label}</dt>
      <dd className="truncate font-medium text-slate-800">{value}</dd>
    </div>
  );
}

function SaleRowView({ s }: { s: SaleRow }) {
  return (
    <Tr>
      <Td className="whitespace-nowrap text-xs text-slate-500">{formatDate(s.date)}</Td>
      <Td className="text-right tabular-nums font-medium">{formatFCFA(s.total)}</Td>
      <Td className="text-right tabular-nums text-slate-500">{formatFCFA(s.amount_paid)}</Td>
      <Td className="text-right tabular-nums text-slate-500">{formatFCFA(s.change_given)}</Td>
      <Td>{s.is_credit ? <Badge color="orange">Crédit</Badge> : <Badge color="green">Comptant</Badge>}</Td>
      <Td className="text-xs text-slate-500">{s.client_id ? "client" : "—"}</Td>
    </Tr>
  );
}

function LogRow({ log }: { log: AppLog }) {
  return (
    <div className="flex items-center justify-between gap-3 py-2.5">
      <div className="min-w-0">
        <p className="truncate text-sm text-slate-800">{log.message || log.event_type}</p>
        <p className="text-xs text-slate-400">{formatDate(log.created_at)}</p>
      </div>
      <LevelBadge level={log.level} />
    </div>
  );
}
