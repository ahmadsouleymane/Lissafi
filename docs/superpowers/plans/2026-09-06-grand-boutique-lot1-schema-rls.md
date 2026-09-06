# Grand boutique — Lot 1 : Schéma & RLS (plan d'implémentation)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Poser le socle de données de l'offre **Grand boutique** (multi-caisses) : notion de
boutique partagée par plusieurs utilisateurs, sans changer aucun comportement de l'app. À la
fin de ce lot, une boutique = un `shop_id`, la RLS Supabase autorise « je vois les données de
MA boutique », et chaque compte existant est automatiquement une boutique solo. **Aucun
changement UI, aucun changement de comportement runtime** — l'app compile et se comporte à
l'identique. Le partage effectif (sync multi-membres), les rôles et l'appairage sont les Lots 2+.

**Décision de conception (affine le design `2026-09-06-grand-boutique-multi-caisses-design.md`) :**
`shop_id` = **l'`user_id` du patron** (le propriétaire de la boutique), pas un UUID indépendant.
Une boutique est identifiée par son propriétaire. Avantages : migration des comptes existants
triviale (`shop_id := user_id`), pas de réconciliation d'UUID entre SQLite local et Supabase,
un vendeur rattaché porte simplement `shop_id = user_id du patron`.

**Périmètre du Lot 1 :** 5 tables métier partagées par boutique — `products`, `sales`,
`sale_items`, `clients`, `debt_transactions`. **`app_settings` reste cloisonné par `user_id`**
dans ce lot (le statut premium/essai est propre au compte ; le partage des réglages boutique
sera traité plus tard, avec précaution vis-à-vis des clés premium posées par le serveur).

**Tech Stack:** Supabase/PostgREST (SQL, RLS, fonctions `SECURITY DEFINER`), SQLite brut
Android (`SQLiteOpenHelper`, pas Room), Kotlin.

## Global Constraints

- Argent en FCFA = `Int` (jamais de flottant). Dates = `Long` epoch millis. Booléens SQLite = 0/1.
- **Miroir obligatoire** : toute table/colonne modifiée dans `LissafiDatabase.onCreate` doit
  l'être en miroir dans `supabase-schema.sql`, avec incrément `DATABASE_VERSION` + migration
  `onUpgrade` (voir Patterns clés de `CLAUDE.md`).
- Le script `supabase-schema.sql` doit **rester idempotent** (re-runnable sans erreur) : usage
  systématique de `ADD COLUMN IF NOT EXISTS`, `CREATE TABLE IF NOT EXISTS`, `DROP POLICY IF EXISTS`,
  `CREATE OR REPLACE FUNCTION`.
- Aucun test unitaire/instrumentation dans le repo → vérification par build :
  `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug`.
  Le SQL se vérifie par relecture + re-run manuel dans le SQL Editor Supabase (non automatisable ici).
- Ce lot est **purement additif** côté SQLite : on ajoute une colonne `shop_id` mais les lectures
  restent filtrées par `user_id` (inchangées). Le basculement des lectures sur `shop_id` est le Lot 3.
- Commentaires de code et messages de commit en **français**, conventional commits, **aucune mention d'IA/Claude**.

---

### Task 1: Supabase — tables `shops` et `shop_members`

**Files:**
- Modify: `supabase-schema.sql` (insérer après la table `device_tokens`, avant le bloc `-- INDEX`, ~ligne 89)

**Interfaces:**
- Produces : `shops(id UUID PK, name TEXT, created_at BIGINT)` et
  `shop_members(shop_id UUID, user_id UUID, role TEXT, caisse_label TEXT, joined_at BIGINT, PK(shop_id,user_id))`.
  `shops.id` = `user_id` du patron. Consommées par les policies (Task 4) et `is_shop_member` (Task 3).

- [ ] **Step 1: Créer les deux tables**

```sql
-- 8. Boutique (unité de partage). id = user_id du patron/propriétaire.
CREATE TABLE IF NOT EXISTS shops (
    id         UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name       TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL
);

-- 9. Appartenance + rôle. Pilote la RLS.
CREATE TABLE IF NOT EXISTS shop_members (
    shop_id      UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role         TEXT NOT NULL DEFAULT 'vendeur' CHECK (role IN ('patron', 'vendeur')),
    caisse_label TEXT NOT NULL DEFAULT '',
    joined_at    BIGINT NOT NULL,
    PRIMARY KEY (shop_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_shop_members_user ON shop_members(user_id);
```

- [ ] **Step 2: Activer la RLS sur ces deux tables** (policies posées en Task 3, après le helper).

```sql
ALTER TABLE shops ENABLE ROW LEVEL SECURITY;
ALTER TABLE shop_members ENABLE ROW LEVEL SECURITY;
```

---

### Task 2: Supabase — colonne `shop_id` + backfill sur les 5 tables métier

**Files:**
- Modify: `supabase-schema.sql` (juste après les `ALTER TABLE ... ADD COLUMN` existants / la définition de chaque table)

**Interfaces:**
- Produces : colonne `shop_id UUID` sur `products`, `sales`, `sale_items`, `clients`,
  `debt_transactions`, remplie (`= user_id`) pour toutes les lignes existantes.

- [ ] **Step 1: Ajouter la colonne (idempotent) sur les 5 tables**

```sql
ALTER TABLE products           ADD COLUMN IF NOT EXISTS shop_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE sales              ADD COLUMN IF NOT EXISTS shop_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE sale_items         ADD COLUMN IF NOT EXISTS shop_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE clients            ADD COLUMN IF NOT EXISTS shop_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE debt_transactions  ADD COLUMN IF NOT EXISTS shop_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
```

- [ ] **Step 2: Backfill `shop_id = user_id` pour l'existant** (chaque compte devient sa boutique solo)

```sql
UPDATE products          SET shop_id = user_id WHERE shop_id IS NULL;
UPDATE sales             SET shop_id = user_id WHERE shop_id IS NULL;
UPDATE sale_items        SET shop_id = user_id WHERE shop_id IS NULL;
UPDATE clients           SET shop_id = user_id WHERE shop_id IS NULL;
UPDATE debt_transactions SET shop_id = user_id WHERE shop_id IS NULL;
```

- [ ] **Step 3: Index sur `shop_id`** (les requêtes de sync filtreront par boutique au Lot 3)

```sql
CREATE INDEX IF NOT EXISTS idx_products_shop          ON products(shop_id);
CREATE INDEX IF NOT EXISTS idx_sales_shop             ON sales(shop_id);
CREATE INDEX IF NOT EXISTS idx_clients_shop           ON clients(shop_id);
CREATE INDEX IF NOT EXISTS idx_debt_transactions_shop ON debt_transactions(shop_id);
```

- [ ] **Step 4: Catalogue unique par boutique** — `products` a une PK `(barcode, user_id)`.
  Ajouter une contrainte d'unicité par boutique sans casser l'existant (pour solo, `shop_id = user_id`
  → équivalent). On garde la PK actuelle dans ce lot et on ajoute un index unique cible :

```sql
CREATE UNIQUE INDEX IF NOT EXISTS uq_products_barcode_shop ON products(barcode, shop_id);
```

  *(La bascule complète de la PK vers `(barcode, shop_id)` — quand le catalogue devient réellement
  partagé et écrit par le patron — est repoussée au Lot 3 pour éviter tout risque sur les données en prod.)*

---

### Task 3: Supabase — helper `is_shop_member()` + boot `get_or_create_my_shop()` + backfill boutiques

**Files:**
- Modify: `supabase-schema.sql` (après les tables, avant/à côté des policies)

**Interfaces:**
- Produces : `is_shop_member(uuid) → boolean` (consommée par toutes les policies métier, Task 4) ;
  `get_or_create_my_shop() → uuid` (consommée par l'app au Lot 2 ; définie ici).

- [ ] **Step 1: Helper d'appartenance (`SECURITY DEFINER` pour éviter la récursion RLS sur `shop_members`)**

```sql
CREATE OR REPLACE FUNCTION public.is_shop_member(p_shop uuid)
RETURNS boolean
LANGUAGE sql SECURITY DEFINER SET search_path = public STABLE
AS $func$
    SELECT EXISTS (
        SELECT 1 FROM shop_members
        WHERE shop_id = p_shop AND user_id = auth.uid()
    );
$func$;
REVOKE EXECUTE ON FUNCTION public.is_shop_member(uuid) FROM PUBLIC, anon;
GRANT  EXECUTE ON FUNCTION public.is_shop_member(uuid) TO authenticated;
```

- [ ] **Step 2: Backfill des boutiques solo pour tous les comptes existants**
  (chaque propriétaire de données devient patron d'une boutique à un membre) :

```sql
INSERT INTO shops (id, name, created_at)
SELECT DISTINCT user_id, '', (extract(epoch FROM now()) * 1000)::bigint
FROM (
    SELECT user_id FROM products
    UNION SELECT user_id FROM sales
    UNION SELECT user_id FROM clients
) u
ON CONFLICT (id) DO NOTHING;

INSERT INTO shop_members (shop_id, user_id, role, caisse_label, joined_at)
SELECT id, id, 'patron', 'Caisse 1', created_at FROM shops
ON CONFLICT (shop_id, user_id) DO NOTHING;
```

- [ ] **Step 3: Fonction d'amorçage appelée par l'app** — renvoie le `shop_id` de l'utilisateur
  connecté, en créant une boutique solo si le compte n'en a pas encore (nouveau compte) :

```sql
CREATE OR REPLACE FUNCTION public.get_or_create_my_shop()
RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $func$
DECLARE
    v_shop uuid;
    v_now  bigint := (extract(epoch FROM now()) * 1000)::bigint;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'not_authenticated';
    END IF;

    -- Boutique déjà membre ? (patron OU vendeur rattaché)
    SELECT shop_id INTO v_shop FROM shop_members WHERE user_id = auth.uid() LIMIT 1;
    IF v_shop IS NOT NULL THEN
        RETURN v_shop;
    END IF;

    -- Sinon : créer la boutique solo (id = mon user_id) + m'y inscrire patron.
    INSERT INTO shops (id, name, created_at) VALUES (auth.uid(), '', v_now)
    ON CONFLICT (id) DO NOTHING;
    INSERT INTO shop_members (shop_id, user_id, role, caisse_label, joined_at)
    VALUES (auth.uid(), auth.uid(), 'patron', 'Caisse 1', v_now)
    ON CONFLICT (shop_id, user_id) DO NOTHING;

    RETURN auth.uid();
END;
$func$;
REVOKE EXECUTE ON FUNCTION public.get_or_create_my_shop() FROM PUBLIC, anon;
GRANT  EXECUTE ON FUNCTION public.get_or_create_my_shop() TO authenticated;
```

- [ ] **Step 4: Policies sur `shops` / `shop_members`** — un membre voit sa boutique et la liste
  de ses membres ; l'écriture reste au serveur (fonctions `SECURITY DEFINER`) pour ce lot :

```sql
DROP POLICY IF EXISTS "Member sees own shop" ON shops;
CREATE POLICY "Member sees own shop" ON shops
    FOR SELECT USING (is_shop_member(id));

DROP POLICY IF EXISTS "Member sees shop roster" ON shop_members;
CREATE POLICY "Member sees shop roster" ON shop_members
    FOR SELECT USING (is_shop_member(shop_id));
```

---

### Task 4: Supabase — bascule des policies RLS métier de `user_id` vers `shop_id`

**Files:**
- Modify: `supabase-schema.sql:118-136` (les 5 policies `products`/`sales`/`sale_items`/`clients`/`debt_transactions`)

**Interfaces:**
- Consumes : `is_shop_member()` (Task 3), colonne `shop_id` (Task 2).
- Effet : un utilisateur voit/écrit les lignes **de sa boutique** au lieu de seulement les siennes.
  Pour un compte solo (`shop_id = user_id`), le comportement est identique à avant → non-régression.

- [ ] **Step 1: Remplacer les 5 policies** (garder `app_settings` et `device_tokens` inchangées) :

```sql
DROP POLICY IF EXISTS "User sees own products" ON products;
CREATE POLICY "Shop sees products" ON products
    FOR ALL USING (is_shop_member(shop_id)) WITH CHECK (is_shop_member(shop_id));

DROP POLICY IF EXISTS "User sees own sales" ON sales;
CREATE POLICY "Shop sees sales" ON sales
    FOR ALL USING (is_shop_member(shop_id)) WITH CHECK (is_shop_member(shop_id));

DROP POLICY IF EXISTS "User sees own sale_items" ON sale_items;
CREATE POLICY "Shop sees sale_items" ON sale_items
    FOR ALL USING (is_shop_member(shop_id)) WITH CHECK (is_shop_member(shop_id));

DROP POLICY IF EXISTS "User sees own clients" ON clients;
CREATE POLICY "Shop sees clients" ON clients
    FOR ALL USING (is_shop_member(shop_id)) WITH CHECK (is_shop_member(shop_id));

DROP POLICY IF EXISTS "User sees own debt_transactions" ON debt_transactions;
CREATE POLICY "Shop sees debt_transactions" ON debt_transactions
    FOR ALL USING (is_shop_member(shop_id)) WITH CHECK (is_shop_member(shop_id));
```

- [ ] **Step 2: Filet de sécurité `default_shop_id()`** — tant que l'app ne renseigne pas
  encore `shop_id` (avant le Lot 3), une ligne insérée sans `shop_id` serait rejetée par
  `WITH CHECK (is_shop_member(NULL))`. Un trigger `BEFORE INSERT` sur les 5 tables pose
  `shop_id := user_id` quand il est NULL (le BEFORE trigger s'exécute avant la vérification RLS) :

```sql
CREATE OR REPLACE FUNCTION public.default_shop_id()
RETURNS trigger LANGUAGE plpgsql AS $func$
BEGIN
    IF NEW.shop_id IS NULL THEN NEW.shop_id := NEW.user_id; END IF;
    RETURN NEW;
END;
$func$;
-- puis un trigger `trg_default_shop_id BEFORE INSERT` sur chacune des 5 tables.
```

  C'est ce qui garantit la **non-régression** des comptes solo sans toucher à l'app dans ce lot.

- [ ] **Step 3: Garde anti-lockout** — vérifier qu'aucune ligne métier n'a `shop_id IS NULL`
  après backfill (sinon elle deviendrait invisible). Requête de contrôle à exécuter manuellement :

```sql
-- Doit renvoyer 0 partout :
SELECT 'products' t, count(*) FROM products WHERE shop_id IS NULL
UNION ALL SELECT 'sales', count(*) FROM sales WHERE shop_id IS NULL
UNION ALL SELECT 'sale_items', count(*) FROM sale_items WHERE shop_id IS NULL
UNION ALL SELECT 'clients', count(*) FROM clients WHERE shop_id IS NULL
UNION ALL SELECT 'debt_transactions', count(*) FROM debt_transactions WHERE shop_id IS NULL;
```

---

### Task 5: SQLite Android — `DATABASE_VERSION` 5→6 + colonne `shop_id` (miroir, additif)

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/data/LissafiDatabase.kt:26` (version),
  `:41-104` (onCreate des 5 tables), `:113-168` (ajout d'un bloc `onUpgrade` v5→v6)

**Interfaces:**
- Produces : colonne `shop_id TEXT NOT NULL DEFAULT ''` sur les 5 tables locales, remplie
  `= user_id` à la migration. **Aucune lecture n'utilise encore `shop_id`** (Lot 3) → additif pur.

- [ ] **Step 1: Incrémenter la version**

`const val DATABASE_VERSION = 5` → `const val DATABASE_VERSION = 6` (mettre à jour le commentaire
de version qui décrit l'historique v2..v5, y ajouter « v6 = colonne `shop_id` (boutique partagée) »).

- [ ] **Step 2: Ajouter `shop_id` dans les `CREATE TABLE` de `onCreate`** pour les 5 tables
  (`products`, `sales`, `sale_items`, `clients`, `debt_transactions`) — colonne
  `shop_id TEXT NOT NULL DEFAULT ''` à côté de `user_id`. Sur `products`, la garder hors PK dans ce lot
  (la PK reste `(barcode, user_id)`, cf. Task 2 Step 4).

- [ ] **Step 3: Bloc de migration `onUpgrade` v5→v6** (à la suite du bloc `if (oldVersion < 5)`) :

```kotlin
// V5 → V6 : colonne shop_id (boutique partagée, offre Grand boutique).
// Additif : chaque compte devient sa boutique solo → shop_id = user_id pour l'existant.
// Les lectures restent filtrées par user_id dans ce lot (bascule sur shop_id au lot suivant).
if (oldVersion < 6) {
    for (t in listOf("products", "sales", "sale_items", "clients", "debt_transactions")) {
        try { db.execSQL("ALTER TABLE $t ADD COLUMN shop_id TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
        try { db.execSQL("UPDATE $t SET shop_id = user_id WHERE shop_id = ''") } catch (_: Exception) {}
    }
}
```

- [ ] **Step 4: Vérifier le build** :
  `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug`.
  Attendu : compile sans erreur. Aucune entité `data/entity/*.kt` ni requête n'ayant besoin de
  `shop_id` à ce stade, rien d'autre ne change.

---

### Task 6: Relecture croisée schéma ↔ SQLite + note de migration

**Files:**
- Read: `supabase-schema.sql`, `LissafiDatabase.kt`
- Modify: `CLAUDE.md` (ligne décrivant `DATABASE_VERSION`, y consigner v6)

- [ ] **Step 1:** Vérifier que chaque colonne/table ajoutée côté SQLite a son miroir côté
  `supabase-schema.sql` et inversement (5 colonnes `shop_id`, tables `shops`/`shop_members` côté serveur
  uniquement — normal, elles n'existent pas en local dans ce lot).
- [ ] **Step 2:** Mettre à jour la parenthèse d'historique de `DATABASE_VERSION` dans `CLAUDE.md`
  (`… ; v6 = colonne shop_id (boutique partagée, offre Grand boutique)`).
- [ ] **Step 3:** Confirmer l'idempotence de `supabase-schema.sql` (relecture : tous les ajouts en
  `IF NOT EXISTS` / `CREATE OR REPLACE` / `DROP POLICY IF EXISTS`).

---

## Definition of Done (Lot 1)

- `supabase-schema.sql` re-runnable : crée `shops` + `shop_members`, ajoute `shop_id` aux 5 tables,
  backfill `shop_id = user_id`, RLS métier basculée sur `is_shop_member(shop_id)`, `app_settings`/`device_tokens` inchangées.
- Comptes existants : chacun est une boutique solo (patron) → **aucune régression** (solo ⇒ `shop_id = user_id`).
- `./gradlew assembleDebug` passe ; SQLite migre v5→v6 en additif, comportement de l'app identique.
- `get_or_create_my_shop()` disponible pour le Lot 2 (rôles & appairage).

## Ce que ce lot ne fait PAS (lots suivants)

- Lot 2 : `currentShopIdProvider` dans le repository, appel `get_or_create_my_shop()` au login,
  écran d'appairage patron/vendeur (code/QR), `join_shop_with_code()`, navigation restreinte vendeur.
- Lot 3 : bascule des lectures/sync sur `shop_id`, catalogue réellement partagé (PK `(barcode, shop_id)`),
  gestion de la survente offline.
- Lot 4 : consolidation (CA par caisse/vendeur, clôture Z, journal, permissions).
- Lot 5 : palier tarifaire Grand boutique côté serveur + back-office + carte paywall + `max_caisses` + export CSV.
