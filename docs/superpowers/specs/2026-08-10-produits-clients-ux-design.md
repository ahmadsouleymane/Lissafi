# Amélioration UX Produits + Clients — Phase 2

**Date** : 2026-08-10
**Statut** : Approuvé
**Portée** : Écrans Produits, Clients et Détail Client uniquement. Réglages/Auth/Onboarding suivront en Phase 3 (spec séparée).

## Contexte

Suite de la Phase 1 (Caisse + Activité, fusionnée dans `main`). Mêmes objectifs, maintenant appliqués aux écrans de gestion : **insights visuels**, **micro-interactions & feedback**, **guidage contextuel**, **hiérarchie visuelle**. Les écrans Produits/Clients/Détail sont déjà structurés (recherche, filtres, badges de statut, états vides) — on les *enrichit*, on ne les refond pas. Design system intact : palette vert forêt / orange brûlé, Inter, Lucide, `LissafiCard`, `StatusBadge`, etc.

Contrainte forte : **zéro nouvelle requête DB, zéro nouvelle dépendance**. Tous les chiffres sont calculés localement depuis l'état déjà en mémoire (listes `state.products` / `state.clients`, transactions déjà chargées). Patterns réutilisés de la Phase 1 : haptique (`LocalHapticFeedback`), `animateItem()` avec clé stable, `StatusBadge`.

## 1. Écran Produits (`ProductsScreen.kt`)

### Compteurs sur les chips de filtre
Les chips `Tous` / `En stock` / `Alerte stock` affichent désormais un compteur : `Tous · 30`, `En stock · 24`, `Alerte stock · 3`. Le compteur reflète la liste courante (`state.products`, non filtrée par la recherche — la recherche écrase la liste dans le ViewModel, comportement préexistant accepté). `alertCount` existe déjà en local — on ajoute les deux autres comptes (`state.products.count { it.stock > 0 }`, et `state.products.size`).

### Marge unitaire sur la fiche produit
Dans `ProductCard`, quand `product.buyPrice > 0`, afficher sous le prix de vente une ligne `Marge {marge} FCFA` avec `marge = sellPrice - buyPrice`, colorée `Success` si positive, `Error` si négative (vente à perte). Ligne discrète (11-12.sp, `FontWeight.Medium`), dans le style de la ligne "Achat …" existante.

### Animation des cartes
`animateItem()` sur chaque `ProductCard` de la `LazyColumn` (la clé `key = { it.barcode }` existe déjà → passer `modifier = Modifier.animateItem()`).

### Haptique
- Suppression confirmée (dans `ConfirmDialog` → `onConfirm` du `ProductCard`) : `HapticFeedbackType.LongPress`.
- Ajout réussi (`onSave` du `ProductFormDialog`) : `HapticFeedbackType.LongPress`.

## 2. Écran Clients (`ClientsScreen.kt`)

### Compteurs sur les chips de filtre
Chips `Tous` / `Avec dette` / `Récents` avec compteurs : `state.clients.count { it.totalDebt > 0 }`, `state.clients.count { it.updatedAt >= recentCutoff }`, et `state.clients.size`. Même convention de liste courante qu'en Produits.

### Carte "À recouvrer"
Nouvelle carte compacte, insérée entre les chips de filtre et la liste, visible **seulement si** `totalDettes > 0` **et** `state.searchQuery.isBlank()` (pendant une recherche, `state.clients` est écrasé par les résultats — on n'affiche pas un total trompeur).

`totalDettes = state.clients.sumOf { it.totalDebt }` (Int FCFA).

Présentation : `LissafiCard` (cornerRadius 18, elevation 2), fond `SecondaryContainer` (orange doux), icône `LissafiIcons.Encaisser` teintée `Secondary`, texte `Total à recouvrer` (`OnSecondaryContainer`, 13-14.sp) et montant `AmountText` (18-20.sp, `Secondary`). Sans bouton d'action (l'écran Détail Client est à un tap).

### Animation des cartes
`animateItem()` sur chaque `ClientCard` — **nécessite d'ajouter `key = { it.id }`** à l'appel `items(filtered, key = { it.id })` (aujourd'hui `items(filtered)` sans clé ; `Client.id` est un UUID unique).

### Haptique
Ajout client réussi (fermeture du `AddClientDialog` via `onSave`) : `HapticFeedbackType.LongPress`.

## 3. Écran Détail Client (`ClientDetailScreen.kt`)

### Mini-stats enrichies (2 → 3 tuiles)
`MiniStats` passe de 2 à 3 tuiles côte à côte, chacune un `StatMiniCard` existant (`weight(1f)`) :
1. `ventes à crédit` = `transactions.count { it.amount > 0 }` (existant, icône `Panier`, `Secondary`)
2. **`total remboursé`** = `transactions.filter { it.amount < 0 }.sumOf { -it.amount }` (nouveau, icône `Rembourser`, `Success`)
3. `client depuis` = texte "Aujourd'hui"/"Il y a Nj" (existant, icône `Recents`, `Primary`)

Ajustement de la largeur : 3 tuiles sur une ligne restent lisibles (elles sont compactes, ~30-34% chacune).

### Haptique
- Validation de remboursement (`RepayDialog` → `onSave`) : `HapticFeedbackType.LongPress`.
- Ajout de dette (`AddDebtDialog` → `onSave`) : `HapticFeedbackType.LongPress`.

## 4. Composants

Aucun nouveau composant partagé : compteurs inline dans les chips, marge inline dans la fiche produit, carte "À recouvrer" inline dans `ClientsScreen`, 3e tuile via `StatMiniCard` existant. Aucune modification de `LissafiComponents.kt` n'est attendue — si un besoin survient, le limiter au strict nécessaire.

## 5. Hors périmètre (Phase 2)

- Réglages, Auth, Onboarding : Phase 3.
- Pas de dark mode, pas de nouvelle dépendance, pas de requête DB supplémentaire.
- Pas de tri réordonné des listes (garder l'ordre actuel par défaut).
- La carte dette du Détail Client (montant + barre remboursé/crédité) reste inchangée.

## 6. Vérification

- `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug` doit passer.
- Aucun test unitaire dans le repo — vérification manuelle sur device/émulateur :
  - Produits : compteurs sur chips corrects, marge affichée quand `buyPrice > 0` (et rouge si perte), animations d'insertion/retrait, haptique sur ajout/suppression.
  - Clients : compteurs corrects, carte "À recouvrer" visible quand dettes > 0 et recherche vide, cachée pendant une recherche ou sans dette, animations, haptique à l'ajout.
  - Détail Client : 3 tuiles correctes (notamment `total remboursé` = somme des remboursements), haptique sur remboursement/ajout de dette.
  - Limitation connue : l'authentification Supabase backend n'est pas configurée dans cet environnement — la vérification de bout en bout des écrans nécessite un compte (limitation du pilier onboarding, sans rapport avec ce travail).
