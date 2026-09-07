# Grand boutique — Lots 4 & 5 : Consolidation + Tarification

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development ou superpowers:executing-plans.
> Prérequis : Lots 1, 2, 2b, 3 livrés (boutique partagée fonctionnelle, rôles patron/vendeur).

**Goal:** (4) donner au patron la valeur qui justifie le prix — chiffre par caisse/vendeur, clôture
de caisse, journal des actions, permissions ; (5) packager et monétiser l'offre **Grand boutique**
comme palier au-dessus de Business, facturé par nombre de caisses.

**Constraints:** identiques (miroir SQLite ↔ SQL, FCFA `Int`, français, build `./gradlew assembleDebug`,
back-office `npm run build`). L'argent reste `Int` FCFA ; l'export CSV utilise `;` (séparateur FR).

---

## LOT 4 — Consolidation & contrôle

### Task 1: CA par caisse / par vendeur
**Files:** `LissafiDatabase.kt`, `LissafiRepository.kt`, `ui/screen/ReportsScreen.kt`, `ui/viewmodel/ReportsViewModel.kt`
- [ ] Les ventes portent déjà `user_id` (auteur) et `shop_id`. Ajouter des agrégats DB groupés par
  `user_id` sur la boutique : `sumTotalBySeller(start, end, shopId)`, `countSalesBySeller(...)`.
- [ ] Rapports (patron) : bloc « Par caisse » listant chaque vendeur (résoudre le nom via
  `my_shop_members`) avec CA, nb de ventes, panier moyen. Filtre période existant réutilisé.

### Task 2: Clôture de caisse (« Z » par vendeur / par jour)
**Files:** SQL (`supabase-schema.sql` : table `cash_closures`), `LissafiDatabase.kt` (miroir + v7),
`ui/screen/…` (écran Clôture)
- [ ] Table `cash_closures(id, shop_id, user_id, opened_at, closed_at, expected_total INT,
  counted_total INT, diff INT, note)`. RLS `is_shop_member(shop_id)`.
- [ ] Écran fin de journée : montant attendu (somme espèces des ventes non-crédit du vendeur depuis
  la dernière clôture) vs montant compté (saisi) → écart affiché. Enregistré + poussé.
- [ ] Incrémenter `DATABASE_VERSION` (v7) + migration `onUpgrade` (créer la table locale).

### Task 3: Journal des actions (anti-triche)
**Files:** SQL (`shop_activity_log`), `LissafiDatabase.kt` (miroir + même v7), repository, écran Journal (patron)
- [ ] Table `shop_activity_log(id, shop_id, user_id, action TEXT, detail TEXT, created_at)`.
  Actions : `sale`, `sale_void`, `discount`, `price_change`, `stock_adjust`, `debt_payment`.
- [ ] Émettre une ligne (local + push) aux points clés du repository. Écran patron « Journal » :
  liste filtrable par vendeur/date. Fire-and-forget local-first (ne bloque pas l'UI).

### Task 4: Permissions par vendeur
**Files:** `shop_members` (colonnes `can_discount`, `can_void`, `can_credit` BOOL — migration SQL),
UI « Mes caisses », points d'application dans les écrans Caisse/Produits
- [ ] Ajouter les 3 permissions (défaut : `can_credit=true`, `can_discount=false`, `can_void=false`).
- [ ] Le patron les règle depuis « Mes caisses ». L'app lit le rôle+permissions au login (étendre
  `join_shop_with_code`/`my_shop_members` pour les renvoyer, stocker dans `SupabaseManager`).
- [ ] Appliquer : masquer/désactiver remise, annulation, vente à crédit selon les permissions du vendeur.

### Task 5: Export CSV (déjà promis sur la carte payante)
**Files:** `service/` (nouveau `ExportService.kt`), `ui/screen/ReportsScreen.kt`
- [ ] Générer un CSV des ventes de la période (date, caisse/vendeur, total, payé, crédit) séparé par
  `;`, partagé via le même mécanisme d'`Intent`/FileProvider que le ticket WhatsApp existant.

### Task 6: Build + vérif rôles (patron voit tout, vendeur restreint).

---

## LOT 5 — Tarification & activation

**Décision actée :** nouvelle offre **Grand boutique** au-dessus de Business, facturée par nombre de
caisses. Piste de départ : **90 000 F/an pour 2 caisses, +30 000 F/an par caisse** (à valider avec
`docs/etude-marche-et-prix.md` et `docs/superpowers/specs/2026-08-12-pricing-v2-design.md`).

### Task 7: Statut serveur — palier + quota de caisses
**Files:** `supabase-schema.sql` (`premium_codes.plan` + `redeem_premium_code`), `supabase-admin.sql`
- [ ] Étendre le `CHECK (plan IN ('plus','business'))` → ajouter `'grand_boutique'`.
- [ ] `redeem_premium_code` pose aussi `app_settings('max_caisses', N)` selon le code (ex. 2, 4, …).
  Ces clés restent **server-managed** (exclues de `pushSettings`, déjà le cas via la liste
  `serverManaged` dans `SyncManager`). Ajouter `max_caisses` à cette liste d'exclusion.
- [ ] Back-office (`supabase-admin.sql` + `backoffice/`) : générer des codes `grand_boutique` avec un
  `max_caisses`, et l'afficher dans les stats/activation.

### Task 8: App — plan `GRAND_BOUTIQUE`
**Files:** `service/PremiumManager.kt`, `ui/viewmodel/SettingsViewModel.kt`, `ui/screen/SettingsScreen.kt`
- [ ] `enum Plan { TRIAL, PLUS, BUSINESS, GRAND_BOUTIQUE, LOCKED }`. `getPlan()` : lit `plan == "grand_boutique"`.
  Tarifs `GRAND_BOUTIQUE_YEARLY = 90_000` (+ constante par caisse).
- [ ] `maxCaisses()` : lit `app_settings('max_caisses')` (défaut 1). Utilisé par `create_pairing_code`
  côté serveur (source de vérité) et pour l'affichage app (« 2/4 caisses utilisées »).
- [ ] Réglages : afficher le plan « Grand boutique · N caisses ».

### Task 9: Paywall — carte Grand boutique
**Files:** `ui/screen/PaywallScreen.kt`
- [ ] Ajouter une carte **Grand boutique** au-dessus de Business. Déplacer les promesses multi-caisses
  (« Multi-caisses », « Plusieurs utilisateurs ») de Business → Grand boutique (Business redevient
  mono-caisse illimité honnête). Liens paiement/WhatsApp sur le modèle existant
  (`buildActivationPaymentLink("grand_boutique", …)`).
- [ ] Landing (`landing/`) : ajouter la colonne Grand boutique au tableau des offres.

### Task 10: Build app + `npm run build` back-office/landing.

## Definition of Done (4 & 5)
- Patron : CA par caisse/vendeur, clôture Z, journal, permissions, export CSV.
- Offre Grand boutique activable par code (palier + quota de caisses), carte paywall + landing à jour,
  Business remis au niveau réel (mono-caisse). `max_caisses` appliqué à l'appairage (Lot 2b).
