# Grand boutique — Multi-caisses (design de l'offre)

> Document de cadrage produit + technique de la nouvelle offre **Grand boutique**.
> Décisions actées : architecture **B (boutique partagée complète)** + **nouvelle offre
> commerciale au-dessus de Business**. Ce document précède le plan d'implémentation
> détaillé (à ranger ensuite dans `docs/superpowers/plans/`).

## 1. Problème & opportunité

Aujourd'hui, un compte Lissafi = **une boutique isolée**. Tout est cloisonné par
`user_id`, et la RLS Supabase est verrouillée sur `auth.uid() = user_id`
(`supabase-schema.sql`). Conséquence : deux téléphones = deux comptes = **deux stocks
séparés qui ne se voient pas**. Il est impossible pour un grand boutique / supérette de
faire tourner **plusieurs caisses sur le même stock**.

Le plan **Business** promet pourtant déjà, sur la carte du paywall, « Multi-caisses
(CA global) » et « Plusieurs utilisateurs » (`PaywallScreen.kt:203-208`). **Ces promesses
ne sont pas tenues** : `Plan.BUSINESS` débloque exactement la même chose que l'essai —
produits illimités (`PremiumManager.kt:142-146`). Il n'existe aucune notion de boutique à
plusieurs caisses dans le code.

**Cible.** Grand boutique / supérette de Niamey = **le patron + 2 à 4 vendeurs**, chacun
sur son téléphone ou une tablette, vendant le même stock, servant les mêmes clients à
crédit, pendant que le patron voit un chiffre consolidé sans tenir la caisse lui-même.

## 2. Packaging commercial — nouvelle offre « Grand boutique »

Nouveau palier **au-dessus de Business**. Business reste **mono-caisse illimité**.

| | Petite boutique (PLUS) | Commerce (BUSINESS) | **Grand boutique (nouveau)** |
|---|---|---|---|
| Produits | ≤ 200 | illimité | illimité |
| Caisses (appareils actifs) | 1 | 1 | **2 à 4+** |
| Utilisateurs / rôles | 1 | 1 | **patron + vendeurs** |
| Stock / clients partagés | — | — | **✅** |
| CA consolidé par caisse/vendeur | — | — | **✅** |
| Clôture de caisse (Z) + journal vendeur | — | — | **✅** |
| Rapports avancés + export CSV | — | ✅ | ✅ |

**Prix (à valider).** Facturation par **nombre de caisses** pour capter la valeur :
piste de départ **Grand boutique = 90 000 F/an pour 2 caisses**, puis **+30 000 F/an par
caisse supplémentaire**. À arbitrer avec `docs/superpowers/specs/2026-08-12-pricing-v2-design.md`
et l'étude `docs/etude-marche-et-prix.md`. Le nombre de caisses autorisé (`max_caisses`)
est porté côté serveur, comme le reste du statut premium (jamais posé par l'app).

## 3. Fonctionnalités de l'offre

**Cœur (justifie le prix — indispensable v1)**
- Plusieurs caisses rattachées à **une même boutique**, appairage par **code / QR**.
- **Stock partagé** quasi temps réel (vente sur caisse A → stock décrémenté vu par B).
- **Clients & crédits partagés** (remboursement possible depuis n'importe quelle caisse).
- **Rôles** : *Patron* (voit tout, gère, voit les marges/rapports) vs *Vendeur*
  (encaisse seulement — ni marges, ni rapports, ni réglages).
- **Tableau de bord consolidé** : CA global + CA **par caisse** et **par vendeur**, par jour.

**Différenciateurs (forte valeur perçue côté patron)**
- **Clôture de caisse / « Z » par vendeur** en fin de journée : attendu vs compté, écart mis en évidence.
- **Journal des actions par vendeur** : qui a vendu quoi, annulations, remises →
  argument **anti-triche / anti-vol** pour un patron absent du comptoir.
- **Permissions par vendeur** : autoriser/interdire remises, annulations, vente à crédit.
- **Rapports avancés + export CSV** (déjà promis sur la carte Business).
- Alertes rupture de stock, ajustement/transfert de stock.

**Plus tard (v2+)**
- Espace **patron à distance** (consulter les stats sans être au magasin — s'appuyer sur
  l'infra back-office existante, mais côté patron, pas admin).
- **Multi-boutiques** (2 magasins d'un même patron).

## 4. Architecture technique — B (boutique partagée)

Passage de **« 1 utilisateur = 1 jeu de données »** à **« 1 boutique = N utilisateurs
partagent 1 jeu de données »**. Trois chantiers.

### 4.1 Modèle de données

Deux nouvelles tables au-dessus des tables métier :

```sql
-- La boutique = l'unité de partage. Créée automatiquement pour chaque compte
-- existant (le propriétaire devient patron d'une boutique à 1 membre).
CREATE TABLE shops (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id    UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name        TEXT,
    created_at  BIGINT NOT NULL
);

-- Appartenance + rôle. C'est la table qui pilote la RLS.
CREATE TABLE shop_members (
    shop_id   UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    user_id   UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role      TEXT NOT NULL CHECK (role IN ('patron', 'vendeur')),
    caisse_label TEXT,          -- « Caisse 1 », « Caisse entrée »…
    joined_at BIGINT NOT NULL,
    PRIMARY KEY (shop_id, user_id)
);
```

Les tables métier (`products`, `sales`, `sale_items`, `clients`, `debt_transactions`,
`app_settings`) gagnent une colonne **`shop_id`**. On **garde `user_id`** en plus : il
devient « l'auteur de la ligne » (utile pour le CA par vendeur et le journal), plus la clé
d'isolation. La PK composite de `products` passe de `(barcode, user_id)` à
`(barcode, shop_id)` — un même code-barres = un seul produit pour toute la boutique.

### 4.2 Sécurité (RLS)

La règle « je vois ma boutique » remplace `auth.uid() = user_id`. Fonction helper
`SECURITY DEFINER` pour éviter la récursion RLS sur `shop_members` :

```sql
CREATE OR REPLACE FUNCTION public.is_shop_member(p_shop UUID)
RETURNS boolean LANGUAGE sql SECURITY DEFINER STABLE AS $$
    SELECT EXISTS (
        SELECT 1 FROM shop_members
        WHERE shop_id = p_shop AND user_id = auth.uid()
    );
$$;
```

Chaque policy métier devient `USING (is_shop_member(shop_id)) WITH CHECK (is_shop_member(shop_id))`.
Les écritures réservées au patron (réglages sensibles, gestion des membres) ajoutent un
contrôle de rôle. **Miroir obligatoire** : `LissafiDatabase.onCreate` + `supabase-schema.sql`
+ incrément `DATABASE_VERSION` (→ v6) avec migration `onUpgrade` qui backfill un `shop_id`
par compte existant.

### 4.3 Rôles côté app

- `LissafiRepository.currentUserIdProvider` gagne un **`currentShopIdProvider`** ; les
  requêtes filtrent par `shop_id`, `user_id` ne sert plus qu'à taguer l'auteur.
- Le rôle (`patron`/`vendeur`) est chargé au login et gouverne la navigation
  (`LissafiNavHost`) : un vendeur ne voit pas Rapports, Réglages sensibles, marges.
- **Appairage d'une caisse** : le patron génère un code/QR depuis Réglages ; le vendeur le
  saisit → insertion `shop_members(shop_id, user_id, 'vendeur')` côté serveur via une
  fonction `SECURITY DEFINER` `join_shop_with_code(code)`, avec garde `max_caisses`.

### 4.4 Synchronisation & offline

Le point le plus délicat, à cause du **local-first** et de la fusion des ids de vente.

- **Fusion des ids** (`finalizeSalePush`) : reste atomique, mais plusieurs appareils
  poussent des ventes concurrentes. Garder l'`id` distant `BIGSERIAL` généré par Supabase
  comme source de vérité ; l'id local n'est jamais partagé. Vérifier qu'aucune référence
  (`sale_items`, `debt_transactions`) ne fuit entre caisses avant réalignement.
- **Pull enrichi** : `SyncManager` tire désormais les ventes/stocks des **autres** membres
  de la boutique, pas seulement les siens (filtre `shop_id`).
- **Survente hors-ligne** (décision produit) : si deux caisses vendent le dernier article
  sans réseau, on **tolère le stock négatif** (« best-effort ») plutôt qu'un verrou strict
  — cohérent avec le contexte informel et le principe « ne jamais bloquer l'UI sur le
  réseau ». Le stock se réconcilie au retour réseau ; une alerte signale les écarts.

## 5. Impacts sur l'existant

- `PremiumManager` : ajouter `Plan.GRAND_BOUTIQUE` (ou un champ `max_caisses` porté par le
  statut premium) ; `getPlan()`/`activateWithCode()` répercutent le palier posé par le serveur.
- `redeem_premium_code()` / back-office : émettre le palier Grand boutique + `max_caisses`,
  et créer/rattacher la `shop`.
- `PaywallScreen` : nouvelle carte « Grand boutique » ; ajuster la carte Business
  (retirer les promesses multi-caisses non tenues, ou les renvoyer vers Grand boutique).
- `supabase-admin.sql` : le back-office gère les membres/caisses d'une boutique.
- Aucun test dans le repo → vérification par build (`./gradlew assembleDebug`,
  `npm run build` back-office).

## 6. Découpage proposé (lots)

1. **Schéma & RLS** : tables `shops`/`shop_members`, colonne `shop_id`, fonction
   `is_shop_member`, migration DB v6, miroir `supabase-schema.sql`. (Aucun changement UI.)
2. **Rôles & appairage** : `currentShopIdProvider`, `join_shop_with_code`, écran d'appairage
   patron (code/QR) + vendeur, navigation restreinte pour le vendeur.
3. **Partage effectif** : sync multi-membres (stock/clients/crédits partagés), gestion offline.
4. **Consolidation** : tableau de bord CA par caisse/vendeur, clôture Z, journal des actions,
   permissions par vendeur.
5. **Monétisation** : palier Grand boutique côté serveur + back-office, carte paywall,
   `max_caisses`, export CSV.

## 7. Questions ouvertes à trancher avant le plan d'implémentation

- Prix exact et paliers de caisses (2 / 4 / illimité ?).
- Un vendeur peut-il appartenir à **plusieurs boutiques** (probablement non en v1 → 1 compte = 1 boutique active).
- Migration des comptes existants : création automatique d'une `shop` par compte, patron = propriétaire.
- Le compte démo (`demo@lissafi.app`) doit-il illustrer une boutique multi-caisses ?
