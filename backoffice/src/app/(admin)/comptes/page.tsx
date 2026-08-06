import Link from "next/link";
import { Badge, Card, EmptyState, PageHeader, Select, Table, Td, Th, THead, Tr } from "@/components/ui";
import { getUserSummaries } from "@/lib/data";
import { formatDate, formatFCFA, formatDateTimeIso } from "@/lib/format";
import { IconUsers } from "@/components/icons";
import type { UserSummary } from "@/types";

export default async function ComptesPage({ searchParams }: { searchParams: Promise<{ q?: string; status?: string }> }) {
  const params = await searchParams;
  const q = (params.q ?? "").toLowerCase().trim();
  const status = params.status ?? "all";

  const users = await getUserSummaries();
  const now = Date.now();

  const filtered = users.filter((u) => {
    if (q && !`${u.email} ${u.shop_name} ${u.shop_phone}`.toLowerCase().includes(q)) return false;
    switch (status) {
      case "premium":
        return u.premium && u.premium_expiry > now;
      case "expired":
        return u.premium && u.premium_expiry <= now;
      case "demo":
        return u.demo_taken;
      case "free":
        return !u.premium && !u.demo_taken;
      default:
        return true;
    }
  });

  return (
    <div className="space-y-5">
      <PageHeader
        title="Comptes"
        subtitle={`${users.length} compte${users.length > 1 ? "s" : ""} enregistré${users.length > 1 ? "s" : ""} · ${filtered.length} affiché${filtered.length > 1 ? "s" : ""}`}
        action={
          <span className="flex items-center gap-2 rounded-lg bg-brand-50 px-3 py-1.5 text-xs font-medium text-brand-700">
            <IconUsers size={15} /> {users.filter((u) => u.premium).length} premium
          </span>
        }
      />

      <Card className="p-4">
        <form method="GET" action="/comptes" className="flex flex-col gap-3 sm:flex-row">
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
            <option value="all">Tous les statuts</option>
            <option value="premium">Premium actif</option>
            <option value="expired">Premium expiré</option>
            <option value="demo">Démo utilisée</option>
            <option value="free">Gratuit</option>
          </select>
          <button type="submit" className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">
            Filtrer
          </button>
        </form>
      </Card>

      {filtered.length === 0 ? (
        <EmptyState title="Aucun compte trouvé" subtitle="Modifie tes critères de recherche." />
      ) : (
        <Table>
          <THead>
            <Th>Boutique</Th>
            <Th>Contact</Th>
            <Th className="text-center">Statut</Th>
            <Th className="text-right">Produits</Th>
            <Th className="text-right">Ventes</Th>
            <Th className="text-right">Chiffre</Th>
            <Th>Inscrit</Th>
            <Th>Dernière connexion</Th>
            <Th className="text-right">Action</Th>
          </THead>
          <tbody>
            {filtered.map((u) => (
              <UserRow key={u.user_id} u={u} />
            ))}
          </tbody>
        </Table>
      )}
    </div>
  );
}

function UserRow({ u }: { u: UserSummary }) {
  const now = Date.now();
  return (
    <Tr>
      <Td>
        <div className="flex items-center gap-2.5">
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-50 text-sm font-bold text-brand-700">
            {(u.shop_name || u.email || "?").charAt(0).toUpperCase()}
          </span>
          <div className="min-w-0">
            <p className="truncate font-medium text-slate-900">{u.shop_name || "—"}</p>
            {u.activation_code && <p className="text-[11px] text-slate-400">Code {u.activation_code}</p>}
          </div>
        </div>
      </Td>
      <Td>
        <p className="text-slate-700">{u.email || "—"}</p>
        {u.shop_phone && <p className="text-xs text-slate-400">{u.shop_phone}</p>}
      </Td>
      <Td className="text-center">
        <UserStatus u={u} now={now} />
      </Td>
      <Td className="text-right tabular-nums text-slate-600">{u.product_count}</Td>
      <Td className="text-right tabular-nums text-slate-600">{u.sale_count}</Td>
      <Td className="text-right tabular-nums font-medium text-slate-800">{formatFCFA(u.sales_total)}</Td>
      <Td className="whitespace-nowrap text-xs text-slate-500">{formatDateTimeIso(u.created_at)}</Td>
      <Td className="whitespace-nowrap text-xs text-slate-500">{formatDateTimeIso(u.last_sign_in_at)}</Td>
      <Td className="text-right">
        <Link href={`/comptes/${u.user_id}`} className="text-sm font-medium text-brand-600 hover:underline">
          Voir
        </Link>
      </Td>
    </Tr>
  );
}

function UserStatus({ u, now }: { u: UserSummary; now: number }) {
  if (u.premium && u.premium_expiry > now) {
    return <Badge color="green">Premium</Badge>;
  }
  if (u.premium) return <Badge color="red">Expiré</Badge>;
  if (u.demo_taken) return <Badge color="blue">Démo</Badge>;
  return <Badge color="gray">Gratuit</Badge>;
}
