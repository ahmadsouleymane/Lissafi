# Refonte UI/UX & Copywriting Lissafi — Design Spec

**Date :** 5 août 2026
**Statut :** Validé — en attente d'implémentation
**Direction visuelle :** Fusion moderne-chaud (palette africaine sophistiquée × rigueur SaaS Vercel-like)

---

## 1. Objectifs

- Transformer Lissafi d'une app Android fonctionnelle en un produit SaaS premium perçu
- Style simple, ultra-efficace, moderne — sans effets superflus
- Rendu « humain », zéro impression d'IA générée
- Icônes harmonieuses, espacements parfaits, beaux graphiques partout
- Navigation mobile optimisée pour un gain de temps maximal
- Copywriting concis dans les UI, chaleureux dans les empty states

---

## 2. Design System

### 2.1 Palette — Fusion moderne-chaud

| Token | Valeur | Rôle |
|-------|--------|------|
| **Primary** | `#1A7F4F` (vert émeraude profond) | Actions, accents, succès |
| **On Primary** | `#FFFFFF` | Texte sur fond primary |
| **Primary Container** | `#E8F5EE` (vert 8%) | Surfaces d'accentuation |
| **On Primary Container** | `#0D3D24` | Texte sur container primary |
| **Secondary** | `#E8913A` (terre cuivrée) | Alertes douces, crédit, highlights |
| **On Secondary** | `#FFFFFF` | Texte sur fond secondary |
| **Secondary Container** | `#FFF3E8` (orange 8%) | Surfaces crédit |
| **On Secondary Container** | `#5C2D0E` | Texte sur container secondary |
| **Background** | `#F9F8F6` | Fond d'écran |
| **On Background** | `#1B1B1B` | Texte principal |
| **Surface** | `#FFFFFF` | Cartes |
| **On Surface** | `#1B1B1B` | Texte sur cartes |
| **Surface Alt** | `#F4F3F0` | Surfaces secondaires |
| **Text Secondary** | `#6B6B6B` | Sous-titres, labels |
| **Text Tertiary** | `#9E9E9E` | Placeholders, hints |
| **Border** | `#EBEBEB` (gris chaud 8%) | Séparations, bordures de cartes |
| **Error** | `#DC5A5A` (rouge adouci) | Erreurs, suppressions |
| **Error Container** | `#FEE2E2` | Fond messages d'erreur |

**Règles :**
- Pas de couleurs inutilisées — le set est volontairement restreint
- Les boutons désactivés : fond `#000000@6%`, texte `#000000@30%`
- Les états de succès/warning utilisent Primary/Secondary plutôt que des couleurs distinctes

### 2.2 Typographie — Inter

Police intégrée via `app/src/main/res/font/inter_*.ttf` (~350 Ko, SIL Open Font License).

| Token | Weight | Size | Line Height | Usage |
|-------|--------|------|-------------|-------|
| `displayLarge` | Light | 36sp | 44sp | Splash, onboarding |
| `headlineLarge` | SemiBold | 24sp | 32sp | Titres d'écran |
| `headlineMedium` | SemiBold | 20sp | 28sp | Titres de section |
| `titleLarge` | Medium | 16sp | 24sp | Titres de carte |
| `titleMedium` | Medium | 14sp | 20sp | Labels |
| `titleSmall` | Medium | 12sp | 16sp | Petits labels |
| `bodyLarge` | Regular | 16sp | 24sp | Corps principal |
| `bodyMedium` | Regular | 14sp | 20sp | Corps secondaire |
| `bodySmall` | Regular | 12sp | 16sp | Légendes, dates |
| `labelLarge` | Medium | 14sp | 20sp | Boutons |
| `labelMedium` | Medium | 12sp | 16sp | Chips, badges |
| `labelSmall` | Medium | 11sp | 16sp | Très petits badges |

**Tracking :** -0.3% sur les titres, +0.1% sur le corps. Inter est déjà bien espacée.

### 2.3 Espacements — Grille 4dp

```
XXS = 2dp    (ultra-fin : icône↔texte dans badge)
XS  = 4dp    (gap minimal)
SM  = 8dp    (gap standard intra-composant)
MD  = 12dp   (padding interne carte)
LG  = 16dp   (padding écran horizontal)
XL  = 20dp   (gap inter-sections)
XXL = 24dp   (padding vertical section)
XXXL= 32dp   (marge haut/bas écran)
```

Toute valeur doit appartenir à cette échelle. Pas de `10.dp` ou `14.dp` arbitraires.

### 2.4 Ombres

- **Carte au repos :** `elevation = 0dp` + `border = 1dp, Border`
- **Carte surélevée :** `shadow(blur = 8dp, offsetY = 2dp, color = #000000@6%)`
- **FAB :** `shadow(blur = 16dp, offsetY = 4dp, color = #000000@10%)`
- **Dialog overlay :** `scrim = #000000@40%`

Pas d'ombres par défaut. Les cartes respirent par leurs bordures.

### 2.5 Formes

```
XS  = 8dp   (chips, badges, petites touches)
SM  = 12dp  (boutons, champs texte, inputs)
MD  = 16dp  (cartes, conteneurs)
LG  = 20dp  (dialogues, bottom sheets)
XL  = 24dp  (grandes modales)
```

### 2.6 Icônes — Lucide (primaire) + Phosphor (sélectif)

**Librairie :** `lucide-compose` — trait 1.5px, tailles standardisées :
- Icône dans un composant : 20dp
- Icône dans un composant compact : 16dp
- Icône empty state : 24dp
- Icône navigation/bottom bar : 22dp

**Mapping des icônes principales :**

| Contexte | Icône Lucide |
|----------|-------------|
| Caisse (bottom bar) | `Store` |
| Produits (bottom bar) | `Package` |
| Clients (bottom bar) | `Users` |
| Activité (bottom bar) | `BarChart3` |
| Scanner | `Scan` |
| Encaisser/paiement | `Banknote` |
| Ajouter | `Plus` |
| Rechercher | `Search` |
| Fermer/annuler | `X` |
| Retour | `ArrowLeft` |
| Réglages | `Settings2` |
| Produit | `Package` |
| Client | `User` |
| Téléphone | `Phone` |
| Email | `Mail` |
| Mot de passe | `Lock` |
| Déconnexion | `LogOut` |
| Version | `Info` |
| Conditions | `FileText` |
| Vérifier/valider | `Check` |
| Alerte/attention | `AlertTriangle` |
| Succès | `CheckCircle2` |
| Erreur | `AlertCircle` |
| Tendance hausse | `TrendingUp` |
| Tendance baisse | `TrendingDown` |
| Imprimer | `Printer` |
| Partager | `Share2` |
| WhatsApp | `MessageCircle` (marque) |
| Crédit/dette | `CreditCard` |
| Remboursement | `Undo2` |
| Comptant | `Banknote` |
| Bénéfice/marge | `DollarSign` |
| Produits récents | `Clock` |
| Panier | `ShoppingCart` |
| Boutique | `Building2` |

**Phosphor (imports vector drawable ponctuels) :**
- `ChartLineUp` — tendances détaillées
- `UsersThree` — groupe de clients
- `CurrencyCircleDollar` — icône argent premium

---

## 3. Navigation

### 3.1 Bottom bar

4 onglets (Paramètres sort de la barre, accessible depuis le header) :

| Icône | Label | Écran |
|:---:|:---:|---|
| `Store` | Caisse | Point de vente principal |
| `Package` | Produits | Catalogue |
| `Users` | Clients | Gestion clients + dettes |
| `BarChart3` | Activité | Rapports + graphiques |

**Indicateur actif :** pill (pastille arrondie) avec fond Primary@10%, icône + texte en Primary. Animation de glissement spring entre les onglets.

**Background :** `Surface` (blanc), border-top 0.5dp `Border`, hauteur 64dp.

### 3.2 Gestes

- **Swipe horizontal** entre les 4 onglets via `HorizontalPager` (lazy loading)
- **Pull-to-refresh** sur les listes pour lancer une synchro manuelle
- **Swipe actions** sur les lignes Produits (gauche = supprimer, droite = éditer)

### 3.3 FAB contextuel

| Écran | Icône | Action |
|-------|-------|--------|
| Caisse | `Scan` | Scanner code-barres |
| Produits | `Plus` | Nouveau produit |
| Clients | `UserPlus` | Nouveau client |
| Activité | — | Pas de FAB (lecture seule) |

Disparaît au scroll vers le bas (`scale` + `fade`), réapparaît au scroll vers le haut.

### 3.4 Hiérarchie de navigation

```
BottomBar (4 onglets, HorizontalPager)
├── Caisse
│   ├── → Scanner code-barres (fullscreen modal)
│   ├── → Encaissement (bottom sheet)
│   ├── → Reçu après vente (bottom sheet)
│   └── → Sélection client crédit (bottom sheet)
├── Produits
│   ├── → Ajouter/Modifier produit (push)
│   └── → Scanner code-barres (modal)
├── Clients
│   ├── → Ajouter/Modifier client (push)
│   ├── → Détail client (push)
│   │   └── → Remboursement (bottom sheet)
│   └── → Historique transactions (push)
├── Activité
│   └── → Détail période (push, zoom jour/semaine)
└── Réglages (icône ⚙ dans le header)
    ├── → Admin (codes premium)
    ├── → Mon compte
    └── → Déconnexion (dialog)
```

### 3.5 Transitions

- **Entre onglets :** fondu croisé 200ms (pas de slide, HorizontalPager gère le swipe)
- **Push écran :** slide horizontal droite→gauche, 250ms
- **Bottom sheet :** slide vertical bas→haut, spring damping
- **Dialog :** scale 95%→100% + fade, 200ms

---

## 4. Écrans

### 4.1 Caisse (écran principal)

**Header :**
- Titre « LISSAFI » en Inter SemiBold 20sp
- Sous-titre dynamique : date + résumé (« Mer 5 août · 3 ventes aujourd'hui » ou « Prêt »)
- Actions : indicateur synchro + ⚙ réglages

**Barre de recherche persistante :**
- Fond `Surface Alt`, coins 12dp, icône `Search` à gauche
- Au focus : dropdown inline avec résultats (pas de dialog)
- Placeholder : « Chercher un produit... »

**Produits récents :**
- Pills horizontaux scrollables : icône + nom + prix
- Hauteur 44dp, fond `Surface`, border 1dp `Border`
- Apparition fade-in

**Carte Total :**
- Bordée (`1.5dp Primary@20%`), fond `Surface`, coins 16dp
- Montant en Inter SemiBold 36sp, aligné à droite
- Compteur d'articles en dessous

**Lignes panier :**
- Stepper horizontal compact : `[−] 3 [+]` en style bordered, 32dp hauteur
- Prix unitaire en `bodySmall`, total ligne en `titleMedium`
- Séparateur 0.5dp `Border` entre chaque ligne

**Toggle Comptant/Crédit :**
- Segmented control 50/50, hauteur 44dp
- Fond `Surface Alt`, sélecteur `Surface` avec ombre fine

**Bouton Encaisser :**
- Primary, pleine largeur, 56dp, coins 14dp
- Texte : « Encaisser · 12 500 FCFA »
- Effet pression : scale 0.97 spring

**FAB Scanner :**
- Cercle Primary 56dp, icône `Scan`, ombre douce
- Position bottom-end, au-dessus de la bottom bar

**Dialogues :**
- Encaissement : bottom sheet avec champ montant + chips rapides
- Reçu : bottom sheet avec résumé + actions (WhatsApp, Partager, Imprimer)
- Sélection client : bottom sheet avec recherche + création rapide

### 4.2 Activité (ex-Rapports)

**Segmented control période :** « Aujourd'hui » / « Cette semaine » / « Ce mois »

**Carte chiffre d'affaires :**
- Sparkline (courbe de Bézier lissée, Vico `LineSpec`)
- Dégradé Primary → Primary@5% en fill
- Variation vs période précédente (↑ 12% / ↓ 5%)
- Montant en Inter SemiBold 30sp

**Grille KPIs 2×2 :**
- Comptant, Crédit, Marge estimée, Panier moyen
- Cartes compactes 14dp, `Surface`, border 1px
- Icône + label + valeur + pourcentage

**Top produits (bar chart horizontal) :**
- Barres proportionnelles, Primary
- Rang en cercle coloré, nom + compteur
- Tap → détail par produit

**Répartition horaire (bar chart vertical) :**
- Colonnes Vico : fines (12dp), coins arrondis, espacement 4dp
- Labels heures en abscisse, pas d'axes lourds
- Tap → tooltip « 14h · 12 ventes · 45 000 FCFA »

**Ventes vs Crédits (stacked bar) :**
- Barres empilées par jour de semaine
- Comptant = Primary, Crédit = Secondary

**Crédits en cours :**
- Liste compacte avec barre de progression 4dp par ligne

**Animations dashboard :**
- Courbes : draw gauche→droite 600ms
- KPIs : `animateIntAsState` (compteur 0→valeur)
- Barres : stagger 50ms depuis le bas

### 4.3 Produits

- Liste épurée avec icône `Package`, nom, prix, stock
- Filter chips : « Tous » / « En stock » / « Alerte stock »
- Stock bas → badge Secondary discret, rupture → badge Error
- Swipe actions (éditer, supprimer)
- Formulaire ajout/modification : sections claires (Info, Prix, Stock, Code-barres)
- Scanner code-barres intégré au formulaire (pas d'écran séparé)

### 4.4 Clients

- Liste avec avatar cercle, nom, nombre d'achats, dette
- Filter chips : « Tous » / « Avec dette » / « Récents »
- Détail client :
  - Carte dette proéminente avec barre de progression
  - Mini-stats (nb achats, ancienneté)
  - Timeline des transactions (crédits, remboursements)
  - FAB « Rembourser »

### 4.5 Réglages

Sections groupées avec titres en minuscules :
- **Boutique :** nom, téléphone
- **Compte :** email, mot de passe, déconnexion
- **Lissafi Premium :** statut, barres progression, upgrade
- **À propos :** version, conditions

Chaque ligne = `InfoRow` : icône | label + valeur → flèche.

### 4.6 Admin

- Statut premium avec barres de progression par limite
- Saisie code premium : 5 inputs individuels (style code PIN)
- Carte essai gratuit distincte (icône 🎁)
- Section synchronisation manuelle

### 4.7 Auth (Login / Signup)

- Logo « ✦ LISSAFI ✦ » + tagline « Ta caisse, simplement »
- Champs plus grands, espacés
- Bouton démo distinct : « 🎬 Tester avec la démo »
- Lien « Mot de passe oublié ? » → bottom sheet email
- Formulaire inscription : email + mot de passe + confirmation
- Messages d'erreur en rouge sous les champs (pas de Toast)
- Validation en temps réel

---

## 5. Copywriting

### 5.1 Principes

- **Concis dans l'UI** : boutons, labels, titres — jamais un mot de trop. Style Linear.app
- **Chaleureux dans les vides** : empty states, messages d'erreur, onboarding. Style Notion
- **Tutoiement préservé** mais élégant : « ta caisse » → « ta caisse » (reste), « ton panier est vide » → « Panier vide »
- **Pas d'exclamation superflue** : jamais « !! »
- **Mots simples** : « activité » plutôt que « rapports », « dette » plutôt que « crédit impayé »

### 5.2 Mapping complet

#### Caisse

| Clé | Avant | Après |
|-----|-------|-------|
| header.subtitle | « Ta caisse » | « {date} · {n} ventes aujourd'hui » |
| search.placeholder | « Chercher un produit et le toucher… » | « Chercher un produit... » |
| section.recent | « PRODUITS VITESSE » | « Récents » |
| cart.empty.title | « Ton panier est vide » | « Panier vide » |
| cart.empty.subtitle | « Touche le scanner pour lire un code-barres... » | « Scanne un code-barres ou cherche un produit » |
| total.label | « TOTAL À PAYER » | « Total à encaisser » |
| total.items | « {n} article(s) » | « {n} articles » |
| mode.cash | « Comptant » | « Comptant » |
| mode.credit | « Crédit » | « Crédit » |
| button.encaisser | « ENCAISSER · {montant} » | « Encaisser · {montant} » |
| button.credit | « ENREGISTRER LE CRÉDIT · {montant} » | « Enregistrer · {montant} » |
| help.title | « Comment vendre en 3 étapes » | « Première vente ? » |
| help.step1 | « Ajoute un produit : scanne son code ou tape son nom » | « 1. Ajoute un produit » |
| help.step2 | « Vérifie le panier et choisis Comptant ou Crédit » | « 2. Vérifie et choisis le mode » |
| help.step3 | « Touche ENCAISSER — c'est tout ! » | « 3. Encaisser, c'est tout » |
| scan.ok | « ✓ {produit} ajouté au panier » | « {produit} ajouté » |
| scan.notfound | « Produit non trouvé : {code} » | « Code-barres inconnu » |
| credit.label | « Crédit pour : {client} » | « Crédit · {client} » |
| credit.noClient | « Pour vendre à crédit, choisis d'abord un client. » | « Choisis un client pour le crédit » |
| receipt.title | « Vente enregistrée ✓ » / « Crédit enregistré ✓ » | « Vente confirmée » / « Crédit enregistré » |
| receipt.change | « Monnaie à rendre : {montant} » | « À rendre : {montant} » |
| empty.cart.action | (n'existe pas) | « Voir les produits » |

#### Activité

| Clé | Avant | Après |
|-----|-------|-------|
| screen.title | « Rapports » | « Activité » |
| screen.subtitle | « Ton activité en un coup d'œil » | (date dynamique) |
| period.today | « Aujourd'hui » | « Aujourd'hui » |
| period.week | « Semaine » | « Cette semaine » |
| period.month | « Mois » | « Ce mois » |
| card.revenue | « Ventes totales » | « Chiffre d'affaires » |
| kpi.cash | « Comptant » | « Comptant » |
| kpi.credit | « Crédit » | « À crédit » |
| kpi.profit | « Bénéfice estimé » | « Marge estimée » |
| kpi.basket | — | « Panier moyen » |
| section.topProducts | « TOP PRODUITS » | « Produits les plus vendus » |
| section.hours | — | « Heures d'activité » |
| section.debts | « Crédits à recevoir » | « Crédits en attente » |
| product.sales | « Vendu {n} fois » | « {n} ventes » |
| badge.top | « N°1 » | « #1 » |
| empty.title | « Aucune vente sur cette période » | « Aucune vente » |
| empty.message | « Les chiffres de ta caisse apparaîtront ici... » | « Lance ta première vente depuis la caisse ! » |

#### Produits

| Clé | Avant | Après |
|-----|-------|-------|
| screen.title | « Produits » | « Produits » |
| filter.all | — | « Tous » |
| filter.inStock | — | « En stock » |
| filter.lowStock | — | « Alerte stock » |
| stock.low | — | « Stock bas » |
| stock.out | — | « Rupture » |
| empty.title | — | « Aucun produit » |
| empty.message | — | « Ajoute ton premier article ! » |
| add.title | « Ajouter un produit » | « Nouveau produit » |
| edit.title | « Modifier le produit » | « Modifier » |
| delete.confirm | — | « Supprimer ce produit ? Les ventes passées ne seront pas affectées. » |
| field.name | — | « Nom du produit » |
| field.price | — | « Prix de vente (FCFA) » |
| field.cost | — | « Prix d'achat (FCFA) » |
| field.stock | — | « Quantité en stock » |
| field.barcode | — | « Code-barres » |
| scan.button | — | « Scanner le code-barres » |

#### Clients

| Clé | Avant | Après |
|-----|-------|-------|
| screen.title | « Clients » | « Clients » |
| filter.all | — | « Tous » |
| filter.withDebt | — | « Avec dette » |
| filter.recent | — | « Récents » |
| empty.title | — | « Aucun client » |
| empty.message | — | « Ajoute ton premier client pour vendre à crédit ! » |
| add.title | « Ajouter un client » | « Nouveau client » |
| detail.debt | « Crédits à recevoir » | « Dette actuelle » |
| detail.purchases | — | « Achats » |
| detail.since | — | « Client depuis le {date} » |
| detail.history | — | « Historique » |
| detail.empty | — | « Aucune transaction avec ce client » |
| repay.button | — | « Rembourser » |
| field.name | « Nom du client * » | « Nom » |
| field.phone | « Téléphone (optionnel) » | « Téléphone » |

#### Réglages

| Clé | Avant | Après |
|-----|-------|-------|
| screen.title | « Paramètres » | « Réglages » |
| section.shop | — | « Boutique » |
| section.account | — | « Compte » |
| section.premium | — | « Lissafi Premium » |
| section.about | — | « À propos » |
| shop.name | — | « Nom de la boutique » |
| shop.phone | — | « Téléphone » |
| account.email | — | « Email » |
| account.password | — | « Mot de passe » |
| account.logout | — | « Déconnexion » |
| premium.status | — | « Statut » |
| premium.upgrade | — | « Passer à Premium → » |
| about.version | « Version de l'application » | « Version » |
| about.terms | — | « Conditions d'utilisation » |

#### Admin

| Clé | Avant | Après |
|-----|-------|-------|
| screen.title | — | « Administration » |
| code.title | — | « Code d'activation » |
| code.activate | — | « Activer » |
| demo.title | — | « Essai gratuit 7 jours » |
| demo.message | — | « Profite de toutes les fonctionnalités Premium » |
| sync.button | — | « Forcer la synchronisation » |

#### Auth

| Clé | Avant | Après |
|-----|-------|-------|
| brand.title | — | « ✦ LISSAFI ✦ » |
| brand.tagline | — | « Ta caisse, simplement » |
| login.button | « Connexion » | « Se connecter » |
| signup.link | « Créer un compte » | « S'inscrire » |
| demo.button | — | « 🎬 Tester avec la démo » |
| forgot.link | — | « Mot de passe oublié ? » |
| field.email | « Email » | « Email » |
| field.password | « Mot de passe » | « Mot de passe » |
| field.confirm | — | « Confirmer le mot de passe » |
| error.invalidEmail | — | « Adresse email invalide » |
| error.shortPassword | — | « 6 caractères minimum » |
| error.mismatch | — | « Les mots de passe ne correspondent pas » |
| signup.button | — | « Créer mon compte » |

---

## 6. Graphiques — Vico

Librairie : `com.patrykandpatrick.vico:compose-m3:2.x`

Types implémentés :
- **Sparkline (carte CA) :** `LineSpec` avec dégradé fill, points de données cerclés, tooltips au tap
- **Bar chart horizontal (top produits) :** barres proportionnelles Primary, coins arrondis à droite
- **Column chart vertical (répartition horaire) :** colonnes fines 12dp, stagger animation
- **Stacked bar chart (ventes vs crédits) :** Primary + Secondary empilés par jour

Animations globales : draw 600ms avec easing `FastOutSlowInEasing`.

---

## 7. Dépendances à ajouter

```kotlin
// build.gradle.kts (app)
implementation("io.github.nicholasgasior:lucide-compose:1.0.0")  // Icônes Lucide
implementation("com.patrykandpatrick.vico:compose-m3:2.1.0")      // Graphiques
// Inter font → app/src/main/res/font/inter_*.ttf (4 fichiers : Regular, Medium, SemiBold, Light)
```

**Phosphor :** import vector drawable manuel via Android Studio → Vector Asset → icône SVG.

---

## 8. Fichiers concernés

### À créer
- `ui/theme/Type.kt` — `LissafiTypography` avec Inter
- `ui/theme/Spacing.kt` — grille d'espacement
- `ui/theme/Icons.kt` — mapping des icônes Lucide
- `res/font/inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter_light.ttf`

### À modifier en profondeur
- `ui/theme/Color.kt` — nouvelle palette
- `ui/theme/Theme.kt` — nouveau `LissafiTheme` avec Inter, nouvelles couleurs
- `ui/navigation/LissafiNavHost.kt` — 4 onglets, HorizontalPager, FAB contextuel
- `ui/components/LissafiComponents.kt` — refonte de tous les composants
- `ui/screen/CaisseScreen.kt` — redesign complet
- `ui/screen/ReportsScreen.kt` → renommé `ActivityScreen.kt`, redesign complet avec Vico
- `ui/screen/ProductsScreen.kt` — redesign
- `ui/screen/ClientsScreen.kt` — redesign
- `ui/screen/ClientDetailScreen.kt` — redesign avec timeline
- `ui/screen/SettingsScreen.kt` — redesign sections
- `ui/screen/AdminScreen.kt` — redesign
- `ui/screen/AuthScreen.kt` — redesign branding
- `build.gradle.kts` — dépendances Lucide + Vico

### À ne pas toucher (logique métier)
- `ui/viewmodel/*` — tous les ViewModels (logique métier inchangée)
- `data/*` — repository, database, API, auth, sync
- `service/*` — ReceiptService, PremiumManager, FormatUtils
- `MainActivity.kt`, `LissafiApp.kt`

---

## 9. Ordre d'implémentation

1. **Design System** — Couleurs, Typographie (Inter), Espacements, Icônes (Lucide), Thème
2. **Composants partagés** — `LissafiComponents.kt` (refonte de tous les composants)
3. **Navigation** — `LissafiNavHost.kt` (HorizontalPager, 4 onglets, FAB)
4. **Écran Caisse** — redesign complet (écran principal, le plus visible)
5. **Écran Activité** — redesign + intégration Vico (le plus transformé)
6. **Écrans Produits + Clients + Détail client**
7. **Écrans Réglages + Admin**
8. **Écran Auth**
9. **Copywriting** — revue complète de tous les textes
10. **Build, test, ajustements visuels**
