# Back-office full data — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Donner à l'admin la consultation de TOUTES les données : onglets Produits/Clients/Dettes/Ventes sur chaque compte, explorateur global table par table (filtre compte + recherche + pagination), et page « Schéma des données ».

**Architecture:** Next.js 15 App Router, rendu serveur `force-dynamic`, lecture via `supabaseAdmin()` (service_role, serveur uniquement). Un registre `lib/explorer.ts` (source unique) décrit chaque table : colonnes, formatage, recherche, ordre — utilisé à la fois par l'explorateur, les onglets et la page de documentation. Les filtres vivent dans l'URL (`searchParams`).

**Tech Stack:** Next.js 15, TypeScript, Tailwind, Supabase JS (`supabase-js`), composants UI existants (`ui.tsx`).

## Global Constraints

- **Build/vérification :** `cd backoffice && npm run build` (typecheck Next.js + rendu). Les pages sont `force-dynamic` → le build passe sans variables d'env.
- **Aucune écriture** : lecture seule. Tout passe par `supabaseAdmin()` (service_role) côté serveur. Les fichiers data portent `import "server-only"`.
- **Sécurité table :** `getExplorerData(table)` ne construit JAMAIS une requête avec un nom de table arbitraire — valider via `getExplorerConfig(table)` (registre), sinon renvoyer `{ rows: [], total: 0, userEmails: {} }`.
- **Langue :** commentaires, libellés et messages de commit en **français**. Jamais de mention IA/Claude.
- **Formatage :** réutiliser `lib/format.ts` (`formatFCFA`, `formatDate`, `formatDateShort`, `toNumber`). Valeur vide/null → « — ».
- **Style :** réutiliser les composants `@/components/ui` (`Card`, `CardHeader`, `Table`, `THead`, `Th`, `Tr`, `Td`, `Badge`, `EmptyState`, `Input`, `Select`, `PageHeader`).

---

### Task 1: Registre des tables (`lib/explorer.ts`)

**Files:**
- Create: `backoffice/src/lib/explorer.ts`

**Interfaces:**
- Produces: `ColumnType`, `ExplorerColumn`, `ExplorerTableConfig`, `EXPLORER_TABLES`, `getExplorerConfig(table: string)` — consommés par Tasks 2, 3, 4, 5.

- [ ] **Step 1: Créer le fichier**

Crée `backoffice/src/lib/explorer.ts` :

```typescript
// ============================================================
// Registre des tables consultables dans le back-office.
// Source unique de vérité : utilisée par l'explorateur global,
// les onglets des comptes et la page « Schéma des données ».
// ============================================================

export type ColumnType = "money" | "date" | "datetime" | "boolean" | "number" | "text";

export type ExplorerColumn = {
  key: string;
  label: string;
  description: string;
  type: ColumnType;
  hideOnMobile?: boolean;
};

export type ExplorerTableConfig = {
  table: string;          // nom PostgREST
  label: string;          // libellé français
  description: string;    // description (page Schéma)
  orderBy: { column: string; ascending?: boolean };
  searchColumns: string[]; // colonnes cherchables par `q`
  hasUserId: boolean;     // ajoute une colonne « Compte » (email résolu)
  columns: ExplorerColumn[];
};

export const EXPLORER_TABLES: ExplorerTableConfig[] = [
  {
    table: "products",
    label: "Produits",
    description: "Catalogue de produits de chaque boutique : code-barres, prix, stock.",
    orderBy: { column: "updated_at", ascending: false },
    searchColumns: ["name", "barcode", "category"],
    hasUserId: true,
    columns: [
      { key: "name", label: "Nom", description: "Nom du produit affiché dans la caisse.", type: "text" },
      { key: "barcode", label: "Code-barres", description: "Code-barres du produit (clé locale avec l'utilisateur).", type: "text", hideOnMobile: true },
      { key: "sell_price", label: "Prix de vente", description: "Prix de vente en FCFA.", type: "money" },
      { key: "buy_price", label: "Prix d'achat", description: "Prix d'achat en FCFA.", type: "money", hideOnMobile: true },
      { key: "stock", label: "Stock", description: "Quantité en stock.", type: "number" },
      { key: "min_stock", label: "Stock min.", description: "Seuil d'alerte de réapprovisionnement.", type: "number", hideOnMobile: true },
      { key: "category", label: "Catégorie", description: "Catégorie du produit.", type: "text", hideOnMobile: true },
      { key: "deleted", label: "Supprimé", description: "Produit supprimé (soft delete).", type: "boolean", hideOnMobile: true },
    ],
  },
  {
    table: "clients",
    label: "Clients",
    description: "Clients enregistrés de chaque boutique : nom, téléphone, dette.",
    orderBy: { column: "name", ascending: true },
    searchColumns: ["name", "phone"],
    hasUserId: true,
    columns: [
      { key: "name", label: "Nom", description: "Nom du client.", type: "text" },
      { key: "phone", label: "Téléphone", description: "Numéro de téléphone du client.", type: "text", hideOnMobile: true },
      { key: "total_debt", label: "Dette totale", description: "Dette cumulée du client en FCFA.", type: "money" },
      { key: "created_at", label: "Créé le", description: "Date de création du client (epoch ms).", type: "date", hideOnMobile: true },
    ],
  },
  {
    table: "sales",
    label: "Ventes",
    description: "Toutes les ventes : total, paiement, type (comptant/crédit), client.",
    orderBy: { column: "date", ascending: false },
    searchColumns: ["client_id"],
    hasUserId: true,
    columns: [
      { key: "id", label: "N°", description: "Identifiant de la vente (BIGSERIAL).", type: "number" },
      { key: "date", label: "Date", description: "Date de la vente (epoch ms).", type: "datetime" },
      { key: "total", label: "Total", description: "Montant total de la vente en FCFA.", type: "money" },
      { key: "amount_paid", label: "Payé", description: "Montant payé en FCFA.", type: "money", hideOnMobile: true },
      { key: "change_given", label: "Monnaie", description: "Monnaie rendue en FCFA.", type: "money", hideOnMobile: true },
      { key: "is_credit", label: "Crédit", description: "Vente faite à crédit.", type: "boolean" },
      { key: "synced", label: "Synchronisée", description: "Vente poussée vers le cloud.", type: "boolean", hideOnMobile: true },
      { key: "client_id", label: "Client", description: "Identifiant local du client (ou vide).", type: "text", hideOnMobile: true },
    ],
  },
  {
    table: "sale_items",
    label: "Articles vendus",
    description: "Lignes de chaque ticket de vente : article, prix unitaire, quantité.",
    orderBy: { column: "id", ascending: false },
    searchColumns: ["name", "barcode"],
    hasUserId: true,
    columns: [
      { key: "sale_id", label: "Vente N°", description: "Identifiant de la vente parente.", type: "number" },
      { key: "name", label: "Article", description: "Nom de l'article vendu.", type: "text" },
      { key: "barcode", label: "Code-barres", description: "Code-barres de l'article.", type: "text", hideOnMobile: true },
      { key: "price", label: "Prix unitaire", description: "Prix unitaire en FCFA.", type: "money" },
      { key: "quantity", label: "Qté", description: "Quantité vendue.", type: "number" },
    ],
  },
  {
    table: "debt_transactions",
    label: "Dettes",
    description: "Transactions de dette : montant, date, note, client concerné.",
    orderBy: { column: "date", ascending: false },
    searchColumns: ["client_id", "note"],
    hasUserId: true,
    columns: [
      { key: "client_id", label: "Client", description: "Identifiant local du client.", type: "text" },
      { key: "amount", label: "Montant", description: "Montant de la transaction en FCFA.", type: "money" },
      { key: "date", label: "Date", description: "Date de la transaction (epoch ms).", type: "datetime" },
      { key: "note", label: "Note", description: "Note libre sur la transaction.", type: "text" },
      { key: "sale_id", label: "Vente N°", description: "Vente associée (si crédit).", type: "number", hideOnMobile: true },
    ],
  },
  {
    table: "app_settings",
    label: "Paramètres",
    description: "Paramètres applicatifs stockés par compte (clé → valeur).",
    orderBy: { column: "key", ascending: true },
    searchColumns: ["key"],
    hasUserId: true,
    columns: [
      { key: "key", label: "Clé", description: "Nom du paramètre (shop_name, is_premium…).", type: "text" },
      { key: "value", label: "Valeur", description: "Valeur du paramètre.", type: "text" },
    ],
  },
];

/** Renvoie la config d'une table, ou undefined si elle n'est pas dans le registre. */
export function getExplorerConfig(table: string): ExplorerTableConfig | undefined {
  return EXPLORER_TABLES.find((t) => t.table === table);
}
```

- [ ] **Step 2: Build**

Run: `cd backoffice && npm run build`
Expected: BUILD SUCCESSFUL (le fichier est compilé via le typecheck).

- [ ] **Step 3: Commit**

```bash
cd /Users/macbookair/Desktop/Lissafi
git add backoffice/src/lib/explorer.ts
git commit -m "feat(backoffice): registre des tables consultables (lib/explorer)"
```

---

### Task 2: Types + fonctions de données (`types.ts`, `lib/data.ts`)

**Files:**
- Modify: `backoffice/src/types.ts`
- Modify: `backoffice/src/lib/data.ts`

**Interfaces:**
- Consumes: `EXPLORER_TABLES`, `getExplorerConfig` (Task 1).
- Produces: types `Product`, `Client`, `DebtTransaction`, `SaleItem`, `ExplorerData`, `AccountSalePage` ; fonctions `getAccountProducts`, `getAccountClients`, `getAccountDebts`, `getAccountSales`, `getExplorerData`, `getAccountOptions` — consommées par Tasks 3, 4, 5.

- [ ] **Step 1: Ajouter les types**

Dans `backoffice/src/types.ts`, à la fin du fichier :

```typescript
export type Product = {
  barcode: string;
  name: string;
  sell_price: number;
  buy_price: number;
  stock: number;
  min_stock: number;
  category: string;
  has_barcode: boolean;
  created_at: number;
  updated_at: number;
  user_id: string;
  deleted: boolean;
};

export type Client = {
  id: string;
  name: string;
  phone: string;
  total_debt: number;
  created_at: number;
  updated_at: number;
  user_id: string;
};

export type DebtTransaction = {
  id: number;
  client_id: string;
  sale_id: number | null;
  amount: number;
  date: number;
  note: string;
  user_id: string;
};

export type SaleItem = {
  id: number;
  sale_id: number;
  barcode: string;
  name: string;
  price: number;
  quantity: number;
  user_id: string;
};

export type ExplorerData = {
  rows: Record<string, unknown>[];
  total: number;
  userEmails: Record<string, string>;
};

export type AccountSalePage = {
  sales: SaleRow[];
  itemsBySale: Record<number, SaleItem[]>;
  total: number;
};
```

- [ ] **Step 2: Ajouter les imports et les fonctions dans `lib/data.ts`**

En tête de `backoffice/src/lib/data.ts`, ajouter à l'import des types :

```typescript
import { getExplorerConfig } from "./explorer";
```
et dans l'import `@/types` ajouter :
```typescript
  AccountSalePage,
  Client,
  DebtTransaction,
  ExplorerData,
  Product,
  SaleItem,
```

À la fin de `backoffice/src/lib/data.ts` (après `countTable`), ajouter :

```typescript
// ============================================================
// Données brutes — consultation complète (lecture seule)
// ============================================================

/** Produits d'un compte (classe par ordre de mise à jour). */
export async function getAccountProducts(userId: string): Promise<Product[]> {
  const { data } = await supabaseAdmin()
    .from("products")
    .select("*")
    .eq("user_id", userId)
    .order("updated_at", { ascending: false });
  return (data ?? []) as Product[];
}

/** Clients d'un compte (classe par nom). */
export async function getAccountClients(userId: string): Promise<Client[]> {
  const { data } = await supabaseAdmin()
    .from("clients")
    .select("*")
    .eq("user_id", userId)
    .order("name", { ascending: true });
  return (data ?? []) as Client[];
}

/** Transactions de dette d'un compte, avec le nom du client résolu. */
export async function getAccountDebts(userId: string): Promise<(DebtTransaction & { client_name: string })[]> {
  const [txns, clients] = await Promise.all([
    supabaseAdmin()
      .from("debt_transactions")
      .select("*")
      .eq("user_id", userId)
      .order("date", { ascending: false }),
    getAccountClients(userId),
  ]);
  const names = new Map(clients.map((c) => [c.id, c.name]));
  return ((txns.data ?? []) as DebtTransaction[]).map((t) => ({
    ...t,
    client_name: names.get(t.client_id) ?? t.client_id,
  }));
}

/** Ventes d'un compte, paginées, avec les articles de la page. */
export async function getAccountSales(userId: string, page = 1, limit = 50): Promise<AccountSalePage> {
  const from = (page - 1) * limit;
  const to = from + limit - 1;
  const { data, count } = await supabaseAdmin()
    .from("sales")
    .select("*", { count: "exact" })
    .eq("user_id", userId)
    .order("date", { ascending: false })
    .range(from, to);
  const sales = (data ?? []) as SaleRow[];
  const ids = sales.map((s) => s.id);
  const itemsBySale: Record<number, SaleItem[]> = {};
  if (ids.length > 0) {
    const { data: items } = await supabaseAdmin()
      .from("sale_items")
      .select("*")
      .in("sale_id", ids)
      .order("id", { ascending: true });
    for (const it of (items ?? []) as SaleItem[]) {
      (itemsBySale[it.sale_id] ??= []).push(it);
    }
  }
  return { sales, itemsBySale, total: toNumber(count) };
}

/** Options {id, label} pour le filtre utilisateur de l'explorateur. */
export async function getAccountOptions(): Promise<{ id: string; label: string }[]> {
  const users = await getUserSummaries();
  return users
    .filter((u) => u.user_id)
    .map((u) => ({ id: u.user_id, label: `${u.shop_name || "Boutique sans nom"} — ${u.email || "—"}` }))
    .sort((a, b) => a.label.localeCompare(b.label, "fr"));
}

/**
 * Données d'une table du registre pour l'explorateur global.
 * Valide `table` contre le registre — jamais de requête sur un nom arbitraire.
 */
export async function getExplorerData(
  table: string,
  opts: { user?: string; q?: string; page?: number; limit?: number } = {}
): Promise<ExplorerData> {
  const config = getExplorerConfig(table);
  if (!config) return { rows: [], total: 0, userEmails: {} };

  const page = Math.max(1, opts.page ?? 1);
  const limit = Math.min(100, Math.max(1, opts.limit ?? 50));
  const from = (page - 1) * limit;
  const to = from + limit - 1;

  let query = supabaseAdmin()
    .from(config.table)
    .select("*", { count: "exact" })
    .order(config.orderBy.column, { ascending: config.orderBy.ascending ?? false })
    .range(from, to);

  if (opts.user) query = query.eq("user_id", opts.user);
  if (opts.q) {
    const like = `ilike.*${opts.q.replace(/\*/g, "")}*`;
    query = query.or(config.searchColumns.map((c) => `${c}.${like}`).join(","));
  }

  const { data, count } = await query;
  const userEmails = await getUserEmails();
  return {
    rows: (data ?? []) as Record<string, unknown>[],
    total: toNumber(count),
    userEmails,
  };
}
```

- [ ] **Step 3: Build**

Run: `cd backoffice && npm run build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
cd /Users/macbookair/Desktop/Lissafi
git add backoffice/src/types.ts backoffice/src/lib/data.ts
git commit -m "feat(backoffice): lectures complètes produits/clients/dettes/ventes + explorateur (data)"
```

---

### Task 3: Tableau générique (`components/ExplorerTable.tsx`)

**Files:**
- Create: `backoffice/src/components/ExplorerTable.tsx`

**Interfaces:**
- Consumes: `ExplorerTableConfig`, `ExplorerColumn` (Task 1) ; `Badge`, `Table`, `Td`, `Th`, `THead`, `Tr` (ui) ; `formatFCFA`, `formatDate`, `formatDateShort`, `toNumber` (format).
- Produces: `<ExplorerTable config rows userEmails />` — utilisé par Task 4.

- [ ] **Step 1: Créer le composant**

Crée `backoffice/src/components/ExplorerTable.tsx` :

```typescript
import type { ReactNode } from "react";
import { Badge, Table, Td, Th, THead, Tr } from "@/components/ui";
import { formatDate, formatDateShort, formatFCFA, toNumber } from "@/lib/format";
import type { ExplorerColumn, ExplorerTableConfig } from "@/lib/explorer";

function formatValue(value: unknown, type: ExplorerColumn["type"]): ReactNode {
  if (value === null || value === undefined || value === "") return "—";
  switch (type) {
    case "money":
      return <span className="tabular-nums">{formatFCFA(toNumber(value))}</span>;
    case "date":
      return <span className="whitespace-nowrap text-xs">{formatDateShort(toNumber(value))}</span>;
    case "datetime":
      return <span className="whitespace-nowrap text-xs">{formatDate(toNumber(value))}</span>;
    case "boolean":
      return value ? <Badge color="green">Oui</Badge> : <Badge color="gray">Non</Badge>;
    case "number":
      return <span className="tabular-nums">{toNumber(value).toLocaleString("fr-FR")}</span>;
    default:
      return String(value);
  }
}

export function ExplorerTable({
  config,
  rows,
  userEmails,
}: {
  config: ExplorerTableConfig;
  rows: Record<string, unknown>[];
  userEmails: Record<string, string>;
}) {
  const cols: ExplorerColumn[] = config.hasUserId
    ? [{ key: "__account__", label: "Compte", description: "Compte auquel la ligne appartient.", type: "text" }, ...config.columns]
    : config.columns;

  return (
    <Table>
      <THead>
        {cols.map((c) => (
          <Th key={c.key}>{c.label}</Th>
        ))}
      </THead>
      <tbody>
        {rows.map((row, i) => (
          <Tr key={i}>
            {cols.map((c) => (
              <Td key={c.key} className="align-middle">
                {c.key === "__account__"
                  ? userEmails[row.user_id as string] || String(row.user_id ?? "").slice(0, 8)
                  : formatValue(row[c.key], c.type)}
              </Td>
            ))}
          </Tr>
        ))}
      </tbody>
    </Table>
  );
}
```

- [ ] **Step 2: Build**

Run: `cd backoffice && npm run build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
cd /Users/macbookair/Desktop/Lissafi
git add backoffice/src/components/ExplorerTable.tsx
git commit -m "feat(backoffice): tableau générique de consultation (ExplorerTable)"
```

---

### Task 4: Explorateur global + documentation + navigation

**Files:**
- Create: `backoffice/src/app/(admin)/donnees/layout.tsx`
- Create: `backoffice/src/app/(admin)/donnees/page.tsx`
- Create: `backoffice/src/app/(admin)/donnees/schema/page.tsx`
- Modify: `backoffice/src/components/AppShell.tsx` (lien de navigation)
- Modify: `backoffice/src/components/icons.tsx` (icône IconDatabase)

**Interfaces:**
- Consumes: `EXPLORER_TABLES`, `getExplorerConfig` (Task 1) ; `getExplorerData`, `getAccountOptions` (Task 2) ; `<ExplorerTable>` (Task 3).
- Produces: page `/donnees` (explorateur), `/donnees/schema` (doc), entrée de nav.

- [ ] **Step 1: Ajouter l'icône IconDatabase**

Dans `backoffice/src/components/icons.tsx`, après `IconAlert`, ajouter :

```tsx
export const IconDatabase = (p: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...p}>
    <ellipse cx="12" cy="5" rx="9" ry="3" />
    <path d="M3 5v14a9 3 0 0 0 18 0V5" />
    <path d="M3 12a9 3 0 0 0 18 0" />
  </svg>
);
```

- [ ] **Step 2: Ajouter le lien dans AppShell**

Dans `backoffice/src/components/AppShell.tsx`, dans le tableau `navItems` (après la ligne « Réglages »), ajouter :

```tsx
  { href: "/donnees", label: "Données", icon: IconDatabase },
```
et ajouter `IconDatabase` à l'import des icônes.

- [ ] **Step 3: Créer le layout interne**

Crée `backoffice/src/app/(admin)/donnees/layout.tsx` :

```tsx
import Link from "next/link";

const tabs = [
  { href: "/donnees", label: "Explorateur", exact: true },
  { href: "/donnees/schema", label: "Schéma des données", exact: false },
];

export default function DonneesLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="space-y-5">
      <div className="flex gap-1 border-b border-slate-200 pb-3">
        {tabs.map((t) => (
          <Link
            key={t.href}
            href={t.href}
            className="rounded-lg px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900"
          >
            {t.label}
          </Link>
        ))}
      </div>
      {children}
    </div>
  );
}
```

- [ ] **Step 4: Créer la page explorateur**

Crée `backoffice/src/app/(admin)/donnees/page.tsx` :

```tsx
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
```

- [ ] **Step 5: Créer la page Schéma**

Crée `backoffice/src/app/(admin)/donnees/schema/page.tsx` :

```tsx
import { Card, CardHeader, PageHeader, Table, Td, Th, THead, Tr } from "@/components/ui";
import { EXPLORER_TABLES } from "@/lib/explorer";

export const dynamic = "force-dynamic";

export default function SchemaPage() {
  return (
    <div className="space-y-5">
      <PageHeader title="Schéma des données" subtitle="Toutes les tables de l'application et la description de chaque champ." />
      {EXPLORER_TABLES.map((t) => (
        <Card key={t.table}>
          <CardHeader title={t.label} subtitle={<span><code className="rounded bg-slate-100 px-1.5 py-0.5 text-xs">{t.table}</code> — {t.description}</span>} />
          <Table className="mb-4">
            <THead>
              <Th>Champ</Th>
              <Th>Type</Th>
              <Th>Description</Th>
            </THead>
            <tbody>
              {t.columns.map((c) => (
                <Tr key={c.key}>
                  <Td className="font-mono text-xs text-slate-700">{c.key}</Td>
                  <Td>
                    <code className="rounded bg-slate-100 px-1.5 py-0.5 text-[11px] text-slate-600">{c.type}</code>
                  </Td>
                  <Td className="text-slate-600">{c.description}</Td>
                </Tr>
              ))}
            </tbody>
          </Table>
        </Card>
      ))}
    </div>
  );
}
```

- [ ] **Step 6: Build**

Run: `cd backoffice && npm run build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
cd /Users/macbookair/Desktop/Lissafi
git add backoffice/src/app/'(admin)'/donnees backoffice/src/components/AppShell.tsx backoffice/src/components/icons.tsx
git commit -m "feat(backoffice): explorateur global de données + page Schéma + navigation"
```

---

### Task 5: Onglets dans le détail du compte (`comptes/[id]`)

**Files:**
- Modify: `backoffice/src/app/(admin)/comptes/[id]/page.tsx`

**Interfaces:**
- Consumes: `getAccountProducts`, `getAccountClients`, `getAccountDebts`, `getAccountSales` (Task 2).
- Produces: page `/comptes/[id]?tab=overview|produits|clients|dettes|ventes` avec navigation par onglets.

- [ ] **Step 1: Restructurer la page en onglets**

Remplace **tout** le contenu de `backoffice/src/app/(admin)/comptes/[id]/page.tsx` par :

```tsx
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
  searchParams: Promise<{ tab?: string }>;
}) {
  const { id } = await params;
  const sp = await searchParams;
  const tab = TABS.some((t) => t.key === sp.tab) ? (sp.tab as string) : "overview";

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
          tab === "ventes" ? getAccountSales(id, 1, 50) : Promise.resolve(null),
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
      {tab === "ventes" && salesPage && <SalesTab salesPage={salesPage} />}
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

function SalesTab({ salesPage }: { salesPage: Awaited<ReturnType<typeof getAccountSales>> }) {
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
```

- [ ] **Step 2: Build**

Run: `cd backoffice && npm run build`
Expected: BUILD SUCCESSFUL. Si une erreur de type survient sur `SaleRowView` (props inutilisées `s.change_given` etc. — non, elles sont utilisées), corriger au minimum.

- [ ] **Step 3: Commit**

```bash
cd /Users/macbookair/Desktop/Lissafi
git add 'backoffice/src/app/(admin)/comptes/[id]/page.tsx'
git commit -m "feat(backoffice): onglets Produits/Clients/Dettes/Ventes dans le détail compte"
```

---

### Task 6: Vérification manuelle

**Files:** aucun (vérification).

- [ ] **Step 1: Lancer le back-office en dev**

Run: `cd backoffice && npm run dev` (ou déploiement Vercel).

- [ ] **Step 2: Scénarios**

1. `/comptes` → ouvrir un compte → les onglets Vue d'ensemble / Produits / Clients / Dettes / Ventes s'affichent ; chaque onglet montre ses données (ou l'empty state).
2. `/donnees` → basculer entre les tables, filtrer par compte, rechercher, paginer ; les liens sont partageables.
3. `/donnees/schema` → toutes les tables + champs décrits.

---

## Self-Review (à exécuter après rédaction)

- [ ] Spec couverte : onglets compte (Produits/Clients/Dettes/Ventes), explorateur global (6 tables, filtre compte, recherche, pagination, email résolu), page Schéma, registre unique, validation de table, pas d'export.
- [ ] Aucun placeholder : chaque étape contient le code complet.
- [ ] Types cohérents : `getExplorerConfig`, `ExplorerTable`, `getExplorerData`, `getAccountSales` (retour `AccountSalePage`), signatures identiques entre tâches.
