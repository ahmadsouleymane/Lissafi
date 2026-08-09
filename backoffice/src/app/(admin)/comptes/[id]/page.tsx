import Link from "next/link";
import { notFound } from "next/navigation";
import { AccountActions } from "@/components/AccountActions";
import { Badge, Card, CardHeader, EmptyState, Table, Td, Th, THead, Tr } from "@/components/ui";
import { LevelBadge, PremiumBadge } from "@/components/badges";
import { IconMail, IconPhone } from "@/components/icons";
import { getAccountClients, getAccountDebts, getAccountProducts, getAccountSales, getUserDetail } from "@/lib/data";
import { formatDate, formatDateShort, formatFCFA, formatDateTimeIso } from "@/lib/format";
import type { AppLog, SaleRow } from "@/types";

export const dynamic = "force-dynamic";

const TABS = [
  { key: "overview", label: "Vue d'ensemble" },
  { key: "produits", label: "Produits" },
  { key: "clients", label: "Clients" },
  { key: "dettes", label: "Dettes" },
  { key: "ventes", label: "Ventes" },
];

const TAB_CLASS =
  "rounded-lg px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900";
const TAB_ACTIVE = "rounded-lg bg-brand-600 px-3 py-1.5 text-sm font-medium text-white";

export default async function CompteDetailPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ tab?: string; page?: string }>;
}) {
  const { id } = await params;
  const sp = await searchParams;
  const tab = TABS.some((t) => t.key === sp.tab) ? (sp.tab as string) : "overview";
  const page = Math.max(1, Number(sp.page ?? 1) || 1);
  const limit = 50;

  const detail = await getUserDetail(id);
  if (!detail.summary) notFound();
  const u = detail.summary;

  const [products, clients, debts, salesPage] =
    tab === "overview"
      ? [null, null, null, null]
      : await Promise.all([
          tab === "produits" ? getAccountProducts(id) : Promise.resolve([]),
          tab === "clients" ? getAccountClients(id) : Promise.resolve([]),
          tab === "dettes" ? getAccountDebts(id) : Promise.resolve([]),
          tab === "ventes" ? getAccountSales(id, page, limit) : Promise.resolve(null),
        ]);

  const waPhone = u.shop_phone.replace(/\s+/g, "");

  return (
    <div className="space-y-5">
      <Link href="/comptes" className="text-sm text-slate-500 hover:text-brand-600 hover:underline">← Retour aux comptes</Link>

      {/* Identité */}
      <Card className="p-5">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex items-center gap-4">
            <span className="flex h-14 w-14 items-center justify-center rounded-full bg-brand-50 text-2xl font-bold text-brand-700">
              {(u.shop_name || u.email || "?").charAt(0).toUpperCase()}
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

      {/* Onglets */}
      <div className="flex flex-wrap gap-1 border-b border-slate-200 pb-3">
        {TABS.map((t) => (
          <Link key={t.key} href={`/comptes/${id}?tab=${t.key}`} className={tab === t.key ? TAB_ACTIVE : TAB_CLASS}>
            {t.label}
          </Link>
        ))}
      </div>

      {tab === "overview" && <OverviewTab detail={detail} />}
      {tab === "produits" && <ProductsTab products={products ?? []} />}
      {tab === "clients" && <ClientsTab clients={clients ?? []} />}
      {tab === "dettes" && <DebtsTab debts={debts ?? []} />}
      {tab === "ventes" && salesPage && (
        <SalesTab
          salesPage={salesPage}
          page={page}
          totalPages={Math.max(1, Math.ceil(salesPage.total / limit))}
          userId={id}
        />
      )}
    </div>
  );
}

// ============================================================
// Vue d'ensemble (contenu existant)
// ============================================================

function OverviewTab({ detail }: { detail: Awaited<ReturnType<typeof getUserDetail>> }) {
  const u = detail.summary!;
  return (
    <div className="space-y-5">
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        <MiniStat label="Produits" value={u.product_count} />
        <MiniStat label="Clients" value={detail.clientCount} />
        <MiniStat label="Ventes" value={u.sale_count} />
        <MiniStat label="Chiffre" value={formatFCFA(u.sales_total)} highlight />
        <MiniStat label="Reçus" value={u.receipt_count} />
        <MiniStat label="Dettes clients" value={formatFCFA(detail.clientDebtTotal)} />
      </div>

      <div className="grid gap-5 lg:grid-cols-3">
        <div className="space-y-5">
          <AccountActions userId={u.user_id} />
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

// ============================================================
// Onglet Produits
// ============================================================

function ProductsTab({ products }: { products: Awaited<ReturnType<typeof getAccountProducts>> }) {
  return (
    <Card>
      <CardHeader title="Produits" subtitle={`${products.length} produit${products.length > 1 ? "s" : ""}`} />
      {products.length === 0 ? (
        <div className="px-5 pb-5"><EmptyState title="Aucun produit" /></div>
      ) : (
        <div className="px-5 pb-4">
          <Table>
            <THead>
              <Th>Nom</Th>
              <Th>Code-barres</Th>
              <Th className="text-right">Prix vente</Th>
              <Th className="text-right">Prix achat</Th>
              <Th className="text-right">Stock</Th>
              <Th>Catégorie</Th>
              <Th>Statut</Th>
            </THead>
            <tbody>
              {products.map((p) => (
                <Tr key={`${p.barcode}-${p.user_id}`}>
                  <Td className="font-medium text-slate-900">{p.name}</Td>
                  <Td className="font-mono text-xs text-slate-500">{p.barcode}</Td>
                  <Td className="text-right tabular-nums">{formatFCFA(p.sell_price)}</Td>
                  <Td className="text-right tabular-nums text-slate-500">{formatFCFA(p.buy_price)}</Td>
                  <Td className="text-right tabular-nums">{p.stock}</Td>
                  <Td className="text-xs text-slate-500">{p.category || "—"}</Td>
                  <Td>{p.deleted ? <Badge color="red">Supprimé</Badge> : <Badge color="green">Actif</Badge>}</Td>
                </Tr>
              ))}
            </tbody>
          </Table>
        </div>
      )}
    </Card>
  );
}

// ============================================================
// Onglet Clients
// ============================================================

function ClientsTab({ clients }: { clients: Awaited<ReturnType<typeof getAccountClients>> }) {
  return (
    <Card>
      <CardHeader title="Clients" subtitle={`${clients.length} client${clients.length > 1 ? "s" : ""}`} />
      {clients.length === 0 ? (
        <div className="px-5 pb-5"><EmptyState title="Aucun client" /></div>
      ) : (
        <div className="px-5 pb-4">
          <Table>
            <THead>
              <Th>Nom</Th>
              <Th>Téléphone</Th>
              <Th className="text-right">Dette totale</Th>
              <Th>Créé le</Th>
            </THead>
            <tbody>
              {clients.map((c) => (
                <Tr key={`${c.id}-${c.user_id}`}>
                  <Td className="font-medium text-slate-900">{c.name}</Td>
                  <Td className="text-slate-500">{c.phone || "—"}</Td>
                  <Td className="text-right tabular-nums">{formatFCFA(c.total_debt)}</Td>
                  <Td className="whitespace-nowrap text-xs text-slate-500">{formatDateShort(c.created_at)}</Td>
                </Tr>
              ))}
            </tbody>
          </Table>
        </div>
      )}
    </Card>
  );
}

// ============================================================
// Onglet Dettes
// ============================================================

function DebtsTab({ debts }: { debts: Awaited<ReturnType<typeof getAccountDebts>> }) {
  return (
    <Card>
      <CardHeader title="Transactions de dette" subtitle={`${debts.length} transaction${debts.length > 1 ? "s" : ""}`} />
      {debts.length === 0 ? (
        <div className="px-5 pb-5"><EmptyState title="Aucune transaction" /></div>
      ) : (
        <div className="px-5 pb-4">
          <Table>
            <THead>
              <Th>Client</Th>
              <Th className="text-right">Montant</Th>
              <Th>Date</Th>
              <Th>Note</Th>
            </THead>
            <tbody>
              {debts.map((d) => (
                <Tr key={d.id}>
                  <Td className="font-medium text-slate-900">{d.client_name}</Td>
                  <Td className="text-right tabular-nums">{formatFCFA(d.amount)}</Td>
                  <Td className="whitespace-nowrap text-xs text-slate-500">{formatDateShort(d.date)}</Td>
                  <Td className="text-xs text-slate-500">{d.note || "—"}</Td>
                </Tr>
              ))}
            </tbody>
          </Table>
        </div>
      )}
    </Card>
  );
}

// ============================================================
// Onglet Ventes (avec articles)
// ============================================================

function SalesTab({
  salesPage,
  page,
  totalPages,
  userId,
}: {
  salesPage: Awaited<ReturnType<typeof getAccountSales>>;
  page: number;
  totalPages: number;
  userId: string;
}) {
  const { sales, itemsBySale, total } = salesPage;
  return (
    <div className="space-y-4">
      <Card>
        <CardHeader title="Historique des ventes" subtitle={`${total} vente${total > 1 ? "s" : ""} au total (50 affichées par page)`} />
        {sales.length === 0 ? (
          <div className="px-5 pb-5"><EmptyState title="Aucune vente" /></div>
        ) : (
          <div className="divide-y divide-slate-100 px-5 pb-3">
            {sales.map((s) => <SaleCard key={s.id} s={s} items={itemsBySale[s.id] ?? []} />)}
          </div>
        )}
      </Card>
      {totalPages > 1 && (
        <div className="flex items-center justify-between px-5">
          <p className="text-sm text-slate-500">{total} vente{total > 1 ? "s" : ""}</p>
          <div className="flex items-center gap-2">
            {page > 1 && (
              <Link href={`/comptes/${userId}?tab=ventes&page=${page - 1}`} className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-50">
                ← Précédent
              </Link>
            )}
            <span className="text-sm text-slate-500">Page {page} / {totalPages}</span>
            {page < totalPages && (
              <Link href={`/comptes/${userId}?tab=ventes&page=${page + 1}`} className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-50">
                Suivant →
              </Link>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function SaleCard({ s, items }: { s: SaleRow; items: Awaited<ReturnType<typeof getAccountSales>>["itemsBySale"][number] }) {
  return (
    <div className="py-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-sm font-semibold text-slate-900">Vente #{s.id}</span>
          <span className="text-xs text-slate-500">{formatDate(s.date)}</span>
          {s.is_credit ? <Badge color="orange">Crédit</Badge> : <Badge color="green">Comptant</Badge>}
        </div>
        <div className="flex flex-wrap items-center gap-3 text-sm">
          <span className="font-semibold text-slate-900">{formatFCFA(s.total)}</span>
          <span className="text-xs text-slate-500">Payé {formatFCFA(s.amount_paid)}</span>
          {s.client_id && <span className="text-xs text-slate-500">Client {s.client_id}</span>}
        </div>
      </div>
      {items.length > 0 && (
        <div className="mt-2 overflow-hidden rounded-lg border border-slate-100">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/80 text-left text-[11px] font-semibold text-slate-500">
                <th className="px-3 py-1.5">Article</th>
                <th className="px-3 py-1.5 text-right">Prix</th>
                <th className="px-3 py-1.5 text-right">Qté</th>
                <th className="px-3 py-1.5 text-right">Sous-total</th>
              </tr>
            </thead>
            <tbody>
              {items.map((it) => (
                <tr key={it.id} className="border-b border-slate-50 last:border-0">
                  <td className="px-3 py-1.5 text-slate-700">{it.name}</td>
                  <td className="px-3 py-1.5 text-right tabular-nums text-slate-500">{formatFCFA(it.price)}</td>
                  <td className="px-3 py-1.5 text-right tabular-nums">{it.quantity}</td>
                  <td className="px-3 py-1.5 text-right tabular-nums font-medium">{formatFCFA(it.price * it.quantity)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

// ============================================================
// Petits composants
// ============================================================

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
