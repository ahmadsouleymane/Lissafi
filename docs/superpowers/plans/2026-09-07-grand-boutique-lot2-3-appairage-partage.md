# Grand boutique — Lots 2b & 3 : Appairage/rôles + Partage effectif

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development ou superpowers:executing-plans.
> Prérequis : Lot 1 (schéma & RLS) et le socle Lot 2 (amorçage boutique, `shop_id` sur les entités,
> `SupabaseManager.currentShopId/currentShopRole`, RPC `get_or_create_my_shop`) sont livrés.

**Goal:** Rendre le multi-caisses **réellement fonctionnel** : (2b) un patron rattache des caisses
vendeurs par code/QR, avec navigation restreinte pour les vendeurs ; (3) les lectures, la synchro et
le catalogue sont partagés **par boutique** (`shop_id`) au lieu de par compte (`user_id`).

**Constraints:** identiques au Lot 1 (miroir SQLite ↔ `supabase-schema.sql`, FCFA `Int`, français,
pas de test → build `./gradlew assembleDebug`). **Vérifier le build après CHAQUE lot** : ces lots
changent le comportement runtime (contrairement au Lot 1). Toujours conserver l'atomicité de
`finalizeSalePush` (voir `CLAUDE.md`).

---

## LOT 3 — Partage effectif par boutique (à faire AVANT 2b : sans lui, un vendeur rattaché ne verrait rien)

Principe : partout où le code filtre/écrit par `user_id`, filtrer/écrire par `shop_id`. Comme la
migration a posé `shop_id = user_id` pour l'existant, un compte solo est stateless-équivalent
(non-régression). `user_id` reste écrit sur chaque ligne comme **auteur** (nécessaire au CA par
vendeur et au journal, Lot 4).

### Task 1: SQLite — persister et lire `shop_id`
**Files:** `app/src/main/java/com/lissafi/app/data/LissafiDatabase.kt`
- [ ] Dans chaque `Cursor.toProduct()/toSale()/toSaleItem()/toClient()/toDebtTransaction()`, lire la
  colonne `shop_id` (`getString(getColumnIndexOrThrow("shop_id"))`) → champ `shopId`.
- [ ] Dans chaque méthode d'écriture (`upsertProduct`, `insertSale`, `insertSaleItem` interne,
  `upsertClient`, `addDebtTransaction`, et les `insert*IfNotExists`), ajouter
  `put("shop_id", entity.shopId)` dans les `ContentValues`. Si `entity.shopId` est vide, écrire
  `entity.userId` (repli solo) pour ne jamais stocker une chaîne vide.
- [ ] Ajouter un paramètre `shopId` (défaut `""`) aux méthodes de lecture qui filtrent aujourd'hui
  par `userId`, et filtrer par `shop_id` quand il est fourni (garder le filtre `user_id` en repli si
  `shopId` vide, pour rester compatible pendant la transition). Alternative plus simple si tu préfères
  un cut net : remplacer le paramètre `userId` par `shopId` et le filtre `user_id = ?` par
  `shop_id = ?` dans : `getAllProducts`, `getAllProductsIncludingDeleted`, `getProduct`,
  `searchProducts`, `getProductCount`, `getRecentProducts`, `getLowStockProducts`,
  `getSalesBetween`, `getUnsyncedSales`, `getTopProducts`, `countSalesBetween`, `sumTotalBetween`,
  `sumCreditBetween`, `getAllClients`, `getClient`, `searchClients`, `getClientCount`,
  `getDebtTransactions`, `getUnsyncedDebtTransactions`, `getTotalDebt`.
- [ ] **Ne PAS** changer la PK locale `(barcode, user_id)` de `products` dans ce lot : conserver
  l'unicité mais indexer aussi `(barcode, shop_id)`. Le catalogue vraiment partagé (deux vendeurs
  éditant la même fiche) suppose que les écritures produit portent le `shop_id` du patron ; c'est
  couvert par le repli d'écriture ci-dessus.

### Task 2: Repository — basculer sur `currentShopId`
**Files:** `app/src/main/java/com/lissafi/app/data/repository/LissafiRepository.kt`,
`app/src/main/java/com/lissafi/app/ui/navigation/LissafiNavHost.kt`
- [ ] Ajouter `var currentShopIdProvider: () -> String = { currentUserId }` et
  `private val currentShopId get() = currentShopIdProvider()`.
- [ ] Passer `currentShopId` (au lieu de `currentUserId`) à toutes les méthodes DB de lecture listées
  en Task 1. **Continuer** de stamper `userId = currentUserId` sur les écritures (auteur), ET ajouter
  `shopId = currentShopId`.
- [ ] Dans `LissafiNavHost`, là où `repository.withUserId(userId)` est appelé, régler aussi
  `repository.currentShopIdProvider = { SupabaseManager.currentShopId(context) }`.

### Task 3: SyncManager — push/pull par boutique
**Files:** `app/src/main/java/com/lissafi/app/data/sync/SyncManager.kt`
- [ ] Introduire `private val currentShopId get() = SupabaseManager.currentShopId(context)`.
- [ ] `pushProducts/pushClients/pushSales/pushDebtTransactions` : lire depuis la DB par `currentShopId`.
  Stamper `shopId = currentShopId` sur les entités poussées (sinon le trigger serveur retombe sur
  `user_id`, ce qui casserait le partage pour une caisse vendeur).
- [ ] `pullProducts/pullClients/pullSales/pullDebtTransactions` : remplacer la validation
  `x.userId != uid` par `x.shopId != currentShopId` (garder les autres gardes d'intégrité :
  montants ≥ 0, nom non vide…). `pullDebtTransactions` itère déjà sur les clients de la boutique.
- [ ] **Fusion des ids de vente** : `finalizeSalePush` reste atomique et inchangée. Vérifier qu'un
  pull sur une caisse B n'insère pas en double une vente poussée par la caisse A : la garde
  `db.saleExists(remoteId)` couvre le cas (l'id distant est global).

### Task 4: Survente hors-ligne (décision produit actée : best-effort)
- [ ] Autoriser le stock négatif : ne pas bloquer une vente si le stock local est insuffisant
  (cohérent avec « ne jamais bloquer l'UI »). Au pull, le stock du serveur fait foi ; si le produit
  repasse négatif après réconciliation, laisser tel quel et signaler via `getLowStockProducts`.
- [ ] (Optionnel) Journaliser `api.logEvent("survente", "warn", ...)` quand une vente rend un stock négatif.

### Task 5: Build
- [ ] `export JAVA_HOME=... && ./gradlew assembleDebug`. Vérifier qu'un compte solo se comporte à
  l'identique (shop_id = user_id) : produits, ventes, clients, dettes, rapports inchangés.

---

## LOT 2b — Appairage des caisses + rôles

### Task 6: Supabase — appairage sécurisé
**Files:** `supabase-schema.sql`
- [ ] Table `shop_pairing_codes(code TEXT PK, shop_id UUID, created_at BIGINT, expires_at BIGINT,
  used_by UUID NULL)` — RLS : lecture/écriture réservées au patron de la boutique (`is_shop_member(shop_id)`
  + rôle patron), ou entièrement via fonctions SECURITY DEFINER.
- [ ] `create_pairing_code()` (SECURITY DEFINER) : réservé au patron ; génère un code court (ex.
  `LSF-XXXXXX`), expire 15 min ; **refuse** si le nombre de membres ≥ `max_caisses` (lu depuis
  `app_settings` du patron, clé posée par le serveur au Lot 5). Renvoie le code.
- [ ] `join_shop_with_code(p_code text)` (SECURITY DEFINER) : valide le code non expiré/non utilisé,
  insère `shop_members(shop_id, auth.uid(), 'vendeur', caisse_label, now)`, marque le code utilisé,
  renvoie `{shop_id, role}`. Garde `max_caisses` de nouveau (course).
- [ ] `leave_shop()` / `remove_member(p_user uuid)` (patron) pour détacher une caisse.
- [ ] `my_shop_members()` : renvoie la liste des membres de ma boutique (patron uniquement) pour l'écran de gestion.

### Task 7: SupabaseApi + Manager — méthodes d'appairage
**Files:** `SupabaseApi.kt`, `SupabaseManager.kt`
- [ ] `createPairingCode(): String?`, `joinShopWithCode(code): Pair<shopId, role>?`,
  `myShopMembers(): List<ShopMember>`, `removeMember(userId)`, `leaveShop()` — sur le modèle de
  `getOrCreateMyShop` (RPC, best-effort, lèvent `SupabaseException` seulement pour l'appairage où
  l'UI a besoin du message d'erreur).
- [ ] Après un `joinShopWithCode` réussi : `SupabaseManager.setShop(context, shopId, "vendeur")` puis
  forcer une synchro complète (pull de tout le dataset de la boutique).

### Task 8: UI — écran « Mes caisses » (patron) + onboarding vendeur
**Files:** `ui/screen/SettingsScreen.kt` (+ nouvel écran), `ui/navigation/LissafiNavHost.kt`,
`ui/viewmodel/…`
- [ ] Patron : dans Réglages, entrée « Mes caisses » → écran listant les membres (nom/rôle/label),
  bouton « Ajouter une caisse » → génère un code + **QR** (réutiliser la lib QR déjà présente pour le
  portail partenaire si dispo, sinon afficher le code en grand). Retrait d'une caisse.
- [ ] Vendeur : à l'onboarding/Réglages, « Rejoindre une boutique » → saisie/scan du code →
  `joinShopWithCode`. Le `BarcodeScanner` existant peut scanner le QR.
- [ ] Feedback clair (code expiré, quota `max_caisses` atteint → renvoyer vers l'offre).

### Task 9: Navigation restreinte selon le rôle
**Files:** `ui/navigation/LissafiNavHost.kt`, écrans concernés
- [ ] Lire `SupabaseManager.currentShopRole(context)`. Si `vendeur` : masquer Rapports, masquer les
  marges/`buy_price` (Produits, tickets), masquer la gestion premium et « Mes caisses », et
  éventuellement restreindre la suppression de produits. Le patron garde tout.
- [ ] Garde-fou : un vendeur ne doit pas pouvoir activer un code premium (l'abonnement est celui du patron).

### Task 10: Build + parcours manuel à 2 comptes
- [ ] Build OK. Test manuel : compte A (patron) génère un code ; compte B (vendeur) rejoint ;
  une vente sur B apparaît dans les données de A ; un produit créé par A est visible sur B ;
  un remboursement de dette sur B est visible sur A.

## Definition of Done (2b & 3)
- Deux appareils connectés à des comptes différents mais à la **même boutique** partagent stock,
  clients, crédits et ventes. Le CA remonte au patron. Un compte solo reste identique à avant.
- Vendeur : UI restreinte (pas de marges, pas de rapports, pas de premium).
