# Spec — Back-office full data (Lissafi)

**Date** : 2026-08-09
**Statut** : validé par l'utilisateur (design approuvé)

## Objectif

Permettre à l'admin de consulter **toutes les données** de l'application depuis
le back-office : données brutes de chaque compte (produits, clients, dettes,
ventes avec articles, paramètres), un **explorateur global** table par table
filtrable par utilisateur, et une **documentation** complète des données.
**Pas d'export CSV pour l'instant** (décision utilisateur — à ajouter plus tard).

## Contexte

Le back-office (Next.js 15 App Router + service_role) affiche déjà : stats
globales, liste des comptes avec agrégats, détail compte (identité, réglages,
logs, actions admin, **100 dernières ventes** sans articles, tickets). Il ne
permet **pas** de voir : produits, clients, transactions de dettes, articles
de vente, historique complet, ni de filtrer/explorer les données brutes.

**Lectures** : tout passe par le client `supabaseAdmin()` (clé `service_role`,
serveur uniquement) qui contourne la RLS — les pages sont déjà protégées par
`getAdminSession()` dans le layout `(admin)`. Rien n'est exposé au navigateur.

## Schéma des données (source : supabase-schema.sql + supabase-admin.sql)

Tables métier (chaque ligne porte `user_id`) :
- **products** : barcode(TEXT, PK avec user_id), name, sell_price(INT),
  buy_price(INT), stock(INT), min_stock(INT), category(TEXT),
  has_barcode(BOOL), created_at(BIGINT), updated_at(BIGINT), deleted(BOOL)
- **sales** : id(BIGSERIAL PK), date(BIGINT), total(INT), amount_paid(INT),
  change_given(INT), is_credit(BOOL), client_id(TEXT), synced(BOOL)
- **sale_items** : id(BIGSERIAL PK), sale_id(BIGINT), barcode(TEXT), name,
  price(INT), quantity(DOUBLE), user_id
- **clients** : id(TEXT, PK avec user_id), name, phone(TEXT), total_debt(INT),
  created_at(BIGINT), updated_at(BIGINT)
- **debt_transactions** : id(BIGSERIAL PK), client_id(TEXT), sale_id(BIGINT),
  amount(INT), date(BIGINT), note(TEXT)
- **app_settings** : key(TEXT, PK avec user_id), value(TEXT)

Tables back-office (déjà gérées par leurs pages) : app_logs, support_tickets,
ticket_replies, admin_settings, admin_actions, admins.

## Fonctionnalités (validées)

### 1. Page compte enrichie — onglets (`/comptes/[id]?tab=…`)

L'onglet actif vit dans l'URL (`searchParams.tab`, défaut `overview`). Chaque
onglet ne charge que ses données (rendu serveur `force-dynamic`).

- **Vue d'ensemble** (`overview`) : contenu actuel (identité, mini-stats,
  réglages, historique admin, logs, ventes récentes 100).
- **Produits** (`produits`) : table produits du compte — code-barres, nom,
  prix vente/achat (FCFA), stock, min_stock, catégorie, supprimé (badge).
- **Clients** (`clients`) : nom, téléphone, dette totale (FCFA).
- **Dettes** (`dettes`) : transactions de dette — client (nom résolu), montant
  (FCFA), date, note.
- **Ventes** (`ventes`) : historique complet paginé (50/page) avec **les
  articles** de chaque vente (barcode, nom, prix, quantité, sous-total).

Navigation entre onglets par `<Link>` avec `?tab=…`. Le lien « ← Retour aux
comptes » est conservé.

### 2. Explorateur global (`/donnees`)

Une page pour voir les données de **tous les comptes**, table par table.

- **Tables exposées** : products, clients, sales, sale_items,
  debt_transactions, app_settings.
- **Filtres dans l'URL** (`searchParams`) :
  - `table` (requis, défaut `products`)
  - `user` (user_id UUID, optionnel) — filtre les lignes d'un compte
  - `q` (texte, optionnel) — recherche sur les colonnes cherchables de la table
  - `page` (défaut 1), `limit` (50, figé)
- **Affichage** : sélecteur de table (liens), champ filtre utilisateur (par
  email/boutique, avec liste déroulante des comptes), champ recherche, tableau
  générique, pagination « Précédent / Suivant » + total.
- **Colonne compte** : quand la table a `user_id`, l'explorateur affiche une
  colonne « Compte » avec l'**email résolu** (via `getUserEmails()`, déjà
  existant) au lieu de l'UUID brut.
- **Tableau générique** : colonnes définies une seule fois dans
  `lib/explorer.ts` (registre) ; formatage appliqué selon le type de colonne
  (FCFA, date epoch, date ISO, booléen → badge, nombre, texte).

### 3. Documentation (`/donnees/schema`)

Page statique « Schéma des données » listant **toutes les tables** et **tous
les champs** avec : nom du champ, type, description (en français).
La description de chaque table/colonne est écrite à la main dans le registre
`lib/explorer.ts` (source unique : utilisée pour l'affichage ET la doc).

## Registre des tables (`lib/explorer.ts`)

Source unique de vérité. Structure par table :

```ts
type ExplorerTable = {
  table: string;                    // nom PostgREST
  label: string;                    // libellé français
  description: string;              // description pour la doc
  orderBy: { column: string; ascending?: boolean };
  searchColumns: string[];          // colonnes cherchables par `q`
  columns: ExplorerColumn[];        // colonnes à afficher
};

type ExplorerColumn = {
  key: string;
  label: string;                    // en-tête français
  description: string;              // description pour la doc
  type: "money" | "date" | "datetime" | "boolean" | "number" | "text";
  hideOnMobile?: boolean;
};
```

### products
- orderBy `updated_at` desc ; search : `name`, `barcode`, `category`
- colonnes : barcode(text), name(text), sell_price(money, « Prix de vente »),
  buy_price(money, « Prix d'achat »), stock(number), min_stock(number),
  category(text), deleted(boolean, « Supprimé »)

### clients
- orderBy `name` asc ; search : `name`, `phone`
- colonnes : name(text, « Nom »), phone(text, « Téléphone »),
  total_debt(money, « Dette totale »), created_at(date, « Créé le »)

### sales
- orderBy `date` desc ; search : `client_id`
- colonnes : id(number), date(datetime), total(money), amount_paid(money,
  « Payé »), change_given(money, « Monnaie »), is_credit(boolean, « Crédit »),
  synced(boolean, « Synchronisée »), client_id(text, « Client »)

### sale_items
- orderBy `id` desc ; search : `name`, `barcode`
- colonnes : id(number), sale_id(number, « Vente # »), name(text, « Article »),
  barcode(text), price(money, « Prix unitaire »), quantity(number, « Qté »)

### debt_transactions
- orderBy `date` desc ; search : `client_id`, `note`
- colonnes : id(number), client_id(text, « Client »), amount(money),
  date(datetime), note(text)

### app_settings
- orderBy `key` asc ; search : `key`
- colonnes : key(text, « Clé »), value(text, « Valeur »)

## Fichiers

**Back-office (`backoffice/`)**

- `src/lib/explorer.ts` *(nouveau)* — registre des tables (config + descriptions).
- `src/lib/data.ts` *(modifié)* — nouvelles fonctions :
  - `getAccountProducts(userId)`
  - `getAccountClients(userId)`
  - `getAccountDebts(userId)` (résout le nom du client via les clients du compte)
  - `getAccountSales(userId, page, limit)` → ventes + `sale_items` pour la page
  - `getExplorerData(table, { user, q, page, limit })` → `{ rows, total }`
  - `getAccountOptions()` → `{ id, label }[]` pour le filtre utilisateur
  - `getTableLabel(table)`, helpers pour construire la requête PostgREST
- `src/types.ts` *(modifié)* — types `Product`, `Client`, `DebtTransaction`,
  `SaleItem`, `ExplorerData`.
- `src/components/ExplorerTable.tsx` *(nouveau)* — tableau générique (colonnes
  via registre, formatage, valeurs manquantes → « — »).
- `src/app/(admin)/comptes/[id]/page.tsx` *(modifié)* — restructuration en
  onglets `?tab=` ; les onglets Produits/Clients/Dettes/Ventes sont des
  composants dédiés (nouvelles fonctions locales dans le même dossier).
- `src/app/(admin)/donnees/page.tsx` *(nouveau)* — explorateur global.
- `src/app/(admin)/donnees/schema/page.tsx` *(nouveau)* — documentation.
- `src/app/(admin)/donnees/layout.tsx` *(nouveau)* — navigation interne
  (Explorateur / Schéma) + header.

## Décisions techniques

- **Pagination** : basée sur `page`/`limit`, `.range((page-1)*limit, page*limit-1)`,
  `.count("exact")` pour le total.
- **Recherche** : `.or()` PostgREST sur `searchColumns` avec `ilike.*{q}*`.
- **Filtre utilisateur** : `.eq("user_id", user)` quand `user` fourni.
- **Sécurité (validation de table)** : `getExplorerData(table)` doit valider
  `table` contre la liste du registre `lib/explorer.ts` (tableaux autorisés
  uniquement) et renvoyer `{ rows: [], total: 0 }` pour toute table inconnue —
  jamais construire une requête PostgREST avec un nom de table arbitraire venu
  de l'URL.
- **Ventes du compte** : paginées (50/page) ; articles récupérés en une 2e
  requête pour les `sale_id` de la page (`.in("sale_id", ids)`), groupés en JS.
- **Dettes du compte** : `getAccountDebts` joint les clients pour résoudre
  `client_id → name` en JS (pas de FK PostgREST exploitable).
- **Formatage** : réutilise `lib/format.ts` (`formatFCFA`, `formatDate`,
  `formatDateTimeIso`).
- **Aucune écriture** ajoutée : lecture seule, cohérent avec l'usage actuel.

## Hors périmètre

- Export CSV (à ajouter plus tard si demandé).
- Modification/suppression de données depuis le back-office.
- Nouveau SQL : aucune table ni fonction RPC à créer — les lectures se font
  directement via le client service_role sur les tables existantes.

## Vérification

- `cd backoffice && npm run build` (Next.js build — typecheck + rendu).
- `npm run dev` + test manuel navigateur : onglets du compte, explorateur
  (sélecteur de table, filtre utilisateur, recherche, pagination), page
  Schéma.
