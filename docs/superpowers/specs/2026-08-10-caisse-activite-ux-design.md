# Amélioration UX Caisse + Activité — Phase 1

**Date** : 2026-08-10
**Statut** : Approuvé
**Portée** : Écrans Caisse (POS) et Activité uniquement. Produits/Clients et Réglages/Auth/Onboarding suivront dans des phases séparées, chacune avec son propre spec.

## Contexte

Lissafi a un système de design déjà cohérent et moderne (palette vert forêt / orange brûlé, typographie Inter, coins arrondis, cartes avec ombres douces, icônes Lucide — voir `ui/theme/` et `ui/components/LissafiComponents.kt`). L'objectif n'est pas de le remplacer mais de l'enrichir pour :

- donner plus d'**insights visuels** (tendances, alertes stock, heures de pointe),
- renforcer les **micro-interactions et le feedback** (haptique, animations de succès),
- améliorer le **guidage contextuel** et la **hiérarchie visuelle** des chiffres clés.

Cible : commerçants informels à Niamey qui utilisent l'app au quotidien pour encaisser et suivre leur activité — le design doit rester lisible et rapide à utiliser, pas surchargé.

## 1. Nouveaux composants partagés (`ui/components/LissafiComponents.kt`)

### `TrendBadge`
Pastille compacte : flèche haut/bas + pourcentage. Vert (`Success`) si évolution positive, orange/rouge (`Secondary`/`Error`) si négative. Réutilisable sur n'importe quel KPI. Pas de valeur affichée si la période précédente n'a aucune donnée (évite un `%` absurde du type +∞%).

### `LowStockBanner`
Carte compacte listant les produits dont `stock <= minStock`, triée par urgence (stock le plus bas en premier), limitée à 3 items visibles + compteur "et X autres". Bouton d'action vers l'écran Produits (filtré si possible, sinon écran Produits standard).

### `HourlyActivityChart`
Bar chart Vico (dépendance déjà présente dans le projet pour la sparkline) montrant le nombre de ventes par heure. Axe X = heures, pas de légende superflue, couleur `Primary`.

### `SuccessPulse`
Remplace l'icône statique de succès dans `ReceiptSheet`. Animation : cercle qui pulse (scale 0.8→1.1→1.0) + check qui apparaît en scale-in, via `animateFloatAsState`/`spring()` — aucune nouvelle dépendance.

### Haptique
Ajout de `view.performHapticFeedback(...)` (via `LocalView.current`) à trois endroits : ajout au panier (scan ou recherche), stepper +/-, validation de vente. Effort minimal, gain de perception de réactivité important.

## 2. Données / ViewModel

### `LissafiRepository` + `LissafiDatabase`
Nouvelle méthode `getLowStockProducts(userId: String): List<Product>` — requête `SELECT * FROM products WHERE user_id = ? AND stock <= min_stock ORDER BY stock ASC`.

### `ReportViewModel` / `ReportState`
Nouveaux champs :
- `previousTotalVentes: Int`, `previousNbTransactions: Int` — calculés en interrogeant la période équivalente immédiatement précédente (ex : période = Semaine → semaine précédente). Réutilise `repository.sumTotalBetween`/`countSalesBetween` avec une plage décalée.
- `hourlyBreakdown: List<Pair<Int, Int>>` (heure 0-23 → nb ventes) — calculé **en mémoire** à partir de la liste `sales` déjà chargée pour le calcul de marge (pas de requête DB supplémentaire). N'est peuplé/affiché que pour les périodes `TODAY` et `WEEK` (peu lisible sur un mois).
- `lowStockProducts: List<Product>` — chargé via la nouvelle méthode repository, indépendamment de la période sélectionnée (c'est un état courant du stock, pas une donnée historique).
- `bestDay: RevenuePoint?` — le point avec le montant le plus élevé dans `revenueSeries`, pour mise en avant textuelle.

## 3. Écran Activité (`ReportsScreen.kt`)

- Chaque `KpiTile` (Comptant, Crédit, Marge) affiche un `TrendBadge` basé sur la comparaison avec la période précédente. Le KPI "Panier moyen" n'a pas de comparaison (moins pertinent).
- Nouvelle section **"Stock bas"** (`LowStockBanner`) affichée en tête de liste, avant la carte CA — c'est l'information la plus actionnable.
- Nouvelle section **"Heures de pointe"** avec `HourlyActivityChart`, affichée seulement si `period != MONTH` et qu'il y a des données.
- La carte CA (`RevenueCard`) affiche en plus, sous la sparkline, le texte "Meilleur jour : {jour} · {montant}" si `bestDay` existe et qu'il y a plusieurs jours dans la série.

## 4. Écran Caisse (`CaisseScreen.kt`)

- Le header (`LissafiHeader`) gagne une pastille discrète à droite du logo (avant l'icône réglages) : `"Aujourd'hui · {FormatUtils.formatFCFA(total)}"`. Nécessite de charger le total du jour au montage de l'écran (nouvelle requête légère `repository.sumTotalBetween` bornée à aujourd'hui, indépendante du `ReportViewModel`). Tap → `onNavigateToReports()`.
- `ReceiptSheet` utilise `SuccessPulse` à la place de l'icône statique actuelle.
- `CartItemRow` : ajout d'une animation d'entrée (`AnimatedVisibility` avec `slideInVertically` + `fadeIn`) quand un item est ajouté au panier.
- Haptique sur ajout panier / stepper / validation (cf. section 1).
- Pas de refonte des états vides existants (déjà bien traités) — alignement visuel mineur seulement si besoin en cours d'implémentation.

## 5. Hors périmètre (Phase 1)

- Produits, Clients, Réglages, Auth, Onboarding : phases suivantes.
- Pas de dark mode (le thème reste "Light uniquement" comme aujourd'hui).
- Pas de nouvelle dépendance externe (on reste sur Vico, déjà présent, et les primitives Compose standard pour les animations).

## 6. Vérification

- `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` puis `./gradlew assembleDebug` doit passer.
- Aucun test unitaire dans le repo — vérification manuelle sur device/émulateur :
  - Flux caisse complet (scan/recherche → panier → encaissement comptant → reçu avec `SuccessPulse`).
  - Flux crédit (vérifier que le header pastille se met à jour après une vente).
  - Écran Activité sur les 3 périodes (Aujourd'hui/Semaine/Mois), avec et sans données.
  - Cas produits sans stock bas (banner absent) et avec stock bas (banner visible, limité à 3 + compteur).
  - Cas période précédente sans données (`TrendBadge` absent, pas de crash division par zéro).
