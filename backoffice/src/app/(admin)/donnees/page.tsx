import Link from "next/link";
import { Card, EmptyState, PageHeader } from "@/components/ui";
import { ExplorerTable } from "@/components/ExplorerTable";
import { EXPLORER_TABLES, getExplorerConfig } from "@/lib/explorer";
import { getAccountOptions, getExplorerData } from "@/lib/data";

export const dynamic = "force-dynamic";

const ACTIVE =
  "rounded-lg bg-brand-600 px-3 py-1.5 text-sm font-medium text-white";
const INACTIVE =
  "rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-50";
const NAV_LINK =
  "rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-50 disabled:opacity-40";

export default async function DonneesPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | undefined>>;
}) {
  const params = await searchParams;
  const table = params.table ?? "products";
  const user = params.user ?? "";
  const q = params.q ?? "";
  const page = Math.max(1, Number(params.page ?? 1) || 1);
  const limit = 50;

  const config = getExplorerConfig(table);
  const [accounts, explorer] = await Promise.all([
    getAccountOptions(),
    getExplorerData(table, { user, q, page, limit }),
  ]);

  const totalPages = Math.max(1, Math.ceil(explorer.total / limit));
  const buildHref = (overrides: Record<string, string | number>) => {
    const sp = new URLSearchParams({ table });
    if (user) sp.set("user", user);
    if (q) sp.set("q", q);
    for (const [k, v] of Object.entries(overrides)) sp.set(k, String(v));
    return `/donnees?${sp.toString()}`;
  };

  return (
    <div className="space-y-5">
      <PageHeader title="Explorateur de données" subtitle="Consulte les données brutes de tous les comptes, table par table." />

      {/* Sélecteur de table */}
      <div className="flex flex-wrap gap-2">
        {EXPLORER_TABLES.map((t) => (
          <Link key={t.table} href={`/donnees?table=${t.table}`} className={table === t.table ? ACTIVE : INACTIVE}>
            {t.label}
          </Link>
        ))}
      </div>

      {/* Filtres */}
      <Card className="p-4">
        <form method="GET" action="/donnees" className="flex flex-col gap-3 sm:flex-row">
          <input type="hidden" name="table" value={table} />
          <select
            name="user"
            defaultValue={user}
            className="sm:w-64 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/40"
          >
            <option value="">Tous les comptes</option>
            {accounts.map((a) => (
              <option key={a.id} value={a.id}>
                {a.label}
              </option>
            ))}
          </select>
          <input
            name="q"
            type="search"
            defaultValue={q}
            placeholder={config ? `Rechercher (${config.searchColumns.join(", ")})…` : "Rechercher…"}
            className="flex-1 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/40"
          />
          <button type="submit" className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">
            Filtrer
          </button>
          {q && (
            <a href={buildHref({ page: 1 })} className="rounded-lg px-3 py-2 text-sm text-brand-600 hover:underline">
              Effacer
            </a>
          )}
        </form>
      </Card>

      {!config ? (
        <EmptyState title="Table inconnue" subtitle="Sélectionne une table dans la liste ci-dessus." />
      ) : explorer.rows.length === 0 ? (
        <EmptyState title="Aucune donnée" subtitle="Modifie les filtres ou choisis un autre compte." />
      ) : (
        <>
          <ExplorerTable config={config} rows={explorer.rows} userEmails={explorer.userEmails} />
          <div className="flex items-center justify-between gap-3">
            <p className="text-sm text-slate-500">
              {explorer.total} ligne{explorer.total > 1 ? "s" : ""}
            </p>
            <div className="flex items-center gap-2">
              {page > 1 && (
                <Link href={buildHref({ page: page - 1 })} className={NAV_LINK}>
                  ← Précédent
                </Link>
              )}
              <span className="text-sm text-slate-500">
                Page {page} / {totalPages}
              </span>
              {page < totalPages && (
                <Link href={buildHref({ page: page + 1 })} className={NAV_LINK}>
                  Suivant →
                </Link>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
