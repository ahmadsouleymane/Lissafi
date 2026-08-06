import Link from "next/link";
import { activatePremiumQuick, addPremiumDaysQuick, deactivatePremiumQuick } from "@/actions/admin";
import { ConfirmForm } from "@/components/ConfirmForm";
import { Badge, Button, Card, EmptyState, PageHeader, Table, Td, Th, THead, Tr } from "@/components/ui";
import { IconCrown } from "@/components/icons";
import { getUserSummaries } from "@/lib/data";
import { daysUntil, formatDate, formatDateTimeIso, toNumber } from "@/lib/format";
import type { UserSummary } from "@/types";

export default async function PremiumPage({ searchParams }: { searchParams: Promise<{ status?: string; q?: string }> }) {
  const params = await searchParams;
  const status = params.status ?? "all";
  const q = (params.q ?? "").toLowerCase().trim();
  const users = await getUserSummaries();
  const now = Date.now();

  const sorted = users
    .map((u) => ({ ...u, _priority: priorityOf(u, now) }))
    .filter((u) => {
      if (q && !`${u.email} ${u.shop_name} ${u.shop_phone}`.toLowerCase().includes(q)) return false;
      switch (status) {
        case "premium": return u.premium && u.premium_expiry > now;
        case "expiring": return u.premium && u.premium_expiry > now && daysUntil(u.premium_expiry) <= 30;
        case "expired": return u.premium && u.premium_expiry <= now;
        case "demo": return u.demo_taken;
        case "free": return !u.premium && !u.demo_taken;
        default: return true;
      }
    })
    .sort((a, b) => a._priority - b._priority || (b.created_at ?? "").localeCompare(a.created_at ?? ""));

  const premiumActive = users.filter((u) => u.premium && u.premium_expiry > now).length;
  const expiringSoon = users.filter((u) => u.premium && u.premium_expiry > now && daysUntil(u.premium_expiry) <= 30).length;
  const expired = users.filter((u) => u.premium && u.premium_expiry <= now).length;

  return (
    <div className="space-y-5">
      <PageHeader
        title="Premium"
        subtitle="Activation manuelle des comptes — le commerçant paie, tu actives."
        action={
          <span className="flex items-center gap-2 rounded-lg bg-brand-50 px-3 py-1.5 text-xs font-medium text-brand-700">
            <IconCrown size={15} /> {premiumActive} actifs
          </span>
        }
      />

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        <Card className="p-4">
          <p className="text-xs text-slate-500">Premium actifs</p>
          <p className="text-2xl font-bold text-brand-600">{premiumActive}</p>
        </Card>
        <Card className="p-4">
          <p className="text-xs text-slate-500">Expirent ≤ 30 jours</p>
          <p className="text-2xl font-bold text-orange-500">{expiringSoon}</p>
        </Card>
        <Card className="p-4">
          <p className="text-xs text-slate-500">Premium expirés</p>
          <p className="text-2xl font-bold text-red-500">{expired}</p>
        </Card>
      </div>

      <Card className="p-4">
        <form method="GET" action="/premium" className="flex flex-col gap-3 sm:flex-row">
          <input
            name="q"
            type="search"
            defaultValue={q}
            placeholder="Rechercher par boutique, email ou téléphone…"
            className="flex-1 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/40"
          />
          <select
            name="status"
            defaultValue={status}
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/40"
          >
            <option value="all">Tous</option>
            <option value="premium">Premium actif</option>
            <option value="expiring">Expirent ≤ 30 j</option>
            <option value="expired">Expirés</option>
            <option value="demo">Démo</option>
            <option value="free">Gratuit</option>
          </select>
          <button type="submit" className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">
            Filtrer
          </button>
        </form>
      </Card>

      {sorted.length === 0 ? (
        <EmptyState title="Aucun compte ne correspond" />
      ) : (
        <Table>
          <THead>
            <Th>Compte</Th>
            <Th>Statut</Th>
            <Th className="text-right">Expiration</Th>
            <Th className="text-right">Ventes</Th>
            <Th className="text-right">Chiffre</Th>
            <Th>Actions</Th>
          </THead>
          <tbody>
            {sorted.map((u) => (
              <PremiumRow key={u.user_id} u={u} now={now} />
            ))}
          </tbody>
        </Table>
      )}
    </div>
  );
}

function priorityOf(u: UserSummary, now: number): number {
  if (u.premium && u.premium_expiry > now) {
    const d = daysUntil(u.premium_expiry);
    return d <= 30 ? 0 : 1;
  }
  if (u.premium) return 2;
  if (u.demo_taken) return 3;
  return 4;
}

function PremiumRow({ u, now }: { u: UserSummary & { _priority: number }; now: number }) {
  const active = u.premium && u.premium_expiry > now;
  const days = daysUntil(u.premium_expiry);

  return (
    <Tr>
      <Td>
        <Link href={`/comptes/${u.user_id}`} className="font-medium text-brand-600 hover:underline">
          {u.shop_name || u.email || "Compte"}
        </Link>
        <p className="text-xs text-slate-400">{u.email || "—"}</p>
      </Td>
      <Td>
        {active ? (
          <Badge color={days <= 30 ? "orange" : "green"}>{days <= 30 ? `Expire dans ${days} j` : "Premium"}</Badge>
        ) : u.premium ? (
          <Badge color="red">Expiré</Badge>
        ) : u.demo_taken ? (
          <Badge color="blue">Démo</Badge>
        ) : (
          <Badge color="gray">Gratuit</Badge>
        )}
      </Td>
      <Td className="whitespace-nowrap text-right text-xs text-slate-500">
        {u.premium_expiry ? formatDate(u.premium_expiry) : "—"}
      </Td>
      <Td className="text-right tabular-nums text-slate-600">{u.sale_count}</Td>
      <Td className="text-right tabular-nums text-slate-700">{formatNumber(u.sales_total)}</Td>
      <Td>
        <div className="flex flex-wrap items-center gap-1.5">
          <form action={activatePremiumQuick.bind(null, u.user_id, 365)}>
            <Button type="submit" size="sm">1 an</Button>
          </form>
          <form action={activatePremiumQuick.bind(null, u.user_id, 30)}>
            <Button type="submit" size="sm" variant="secondary">30 j</Button>
          </form>
          <form action={addPremiumDaysQuick.bind(null, u.user_id, 30)}>
            <Button type="submit" size="sm" variant="secondary">+30 j</Button>
          </form>
          {active && (
            <ConfirmForm action={deactivatePremiumQuick.bind(null, u.user_id)} confirmText={`Désactiver le premium de ${u.shop_name || u.email} ?`}>
              <Button type="submit" size="sm" variant="dangerOutline">Stop</Button>
            </ConfirmForm>
          )}
        </div>
      </Td>
    </Tr>
  );
}

function formatNumber(n: number | undefined): string {
  return new Intl.NumberFormat("fr-FR").format(toNumber(n)) + " F";
}
