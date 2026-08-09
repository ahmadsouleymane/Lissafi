# Plan Stratégique Complet — LISSAFI

> Application mobile Android de caisse enregistreuse pour petits commerces informels. Niamey, Niger.

---

## DÉCISIONS ACTÉES (non négociables pour cette version)

| Décision | Choix |
|---|---|
| Nom | **Lissafi** (« calcul, compte » en haoussa) |
| Plateforme | **Android natif (Kotlin)**, APK distribué hors Play Store |
| iOS | Plus tard, quand l'argent rentre |
| Modèle | **Abonnement annuel**, pas de licence à vie, pas de mensuel |
| Offres payantes | **Essentiel 25 000 F/an** (logiciel seul) + **Pack Boutique 60 000 F** (imprimante + logiciel) |
| Version gratuite | 10 produits, 10 crédits clients, historique 30 jours |
| Activation Premium | **Manuelle** — le commerçant paie en espèces/mobile money, toi tu actives via code admin |
| Imprimante | **Pack Boutique à la demande** (dropshipping local, zéro stock) — imprimante 58 mm Bluetooth + 2 rouleaux + installation + 1 an Essentiel inclus |
| Rouleaux papier | Offerts dans le Pack, vendus séparément à la demande |

---

## TABLE DES MATIÈRES

1. Résumé exécutif
2. Marque & identité
3. Business model & pricing
4. Spécification technique (pour développement)
5. Architecture base de données locale
6. Écrans & parcours utilisateur
7. Copywriting — tous les textes
8. Plan marketing 12 mois
9. Plan de lancement
10. Roadmap 90 jours
11. Projections financières
12. Risques & mitigations
13. Actions prioritaires

---

## 1. RÉSUMÉ EXÉCUTIF

### Le projet
**Lissafi** — une appli Android de caisse qui remplace le cahier des petits commerçants de Niamey. Scan, panier, crédits clients, stock. Fonctionne sans Internet.

### Modèle
- **Gratuit** : scan, panier, 10 produits, 10 crédits, historique 30j, ticket WhatsApp
- **Essentiel (25 000 FCFA/an)** : tout illimité, rapports, export CSV, support prioritaire
- **Pack Boutique (60 000 FCFA)** : imprimante 58 mm Bluetooth + 2 rouleaux + installation + 1 an Essentiel inclus — à la demande, zéro stock (dropshipping local)

### Objectif 12 mois
- 50 clients payants = 1 382 000 FCFA/an net (dont ~140 000 F de marge matérielle)
- 100 utilisateurs gratuits actifs
- 1 fournisseur d'imprimantes validé et testé

### Condition de succès
1. MVP codé en 1 semaine
2. 5 commerçants pilotes en semaine 2
3. Premier client payant avant fin mois 2
4. Présence au Grand Marché minimum 3x/semaine

---

## 2. MARQUE & IDENTITÉ

### Nom
**Lissafi** (Li-ssa-fi) — « calcul », « compte » en haoussa. Mot connu de tous les commerçants.

### Extension
À choisir plus tard : lissafi.ne, lissafi.app, lissafi.com

### Slogan
**« Ton commerce, maîtrisé. »**

### Couleurs
- Principal : Vert #2E8B57 (confiance, commerce, prospérité)
- Accent : Orange #F4A460 (énergie, chaleur, accessibilité)
- Fond : Blanc cassé #FAF9F5

### Icône
Panier stylisé avec une coche, couleurs vert + orange.

### Ton
Tutoiement. Français simple. Zéro jargon. Toujours parler en francs CFA.

---

## 3. BUSINESS MODEL & PRICING

### Structure — 3 offres

| | Gratuit | **Essentiel** | **Pack Boutique** |
|---|---|---|---|
| **Prix** | 0 FCFA | **25 000 FCFA/an** | **60 000 FCFA** (une fois) |
| Scan codes-barres | ✅ | ✅ | ✅ |
| Panier + calcul monnaie | ✅ | ✅ | ✅ |
| Ticket WhatsApp | ✅ | ✅ | ✅ |
| Mode hors-ligne | ✅ | ✅ | ✅ |
| Produits au catalogue | **10 max** | Illimité | Illimité |
| Crédits clients | **10 débiteurs max** | Illimité | Illimité |
| Historique ventes | **30 jours** | Illimité | Illimité |
| Gestion de stock | Consultation seule | Gestion complète + alertes | Gestion complète + alertes |
| Export CSV | ❌ | ✅ | ✅ |
| Rapports (jour/semaine/mois) | ❌ | ✅ | ✅ |
| Support WhatsApp | Standard | Prioritaire (<2h) | Prioritaire (<2h) |
| Imprimante 58 mm Bluetooth | ❌ | ❌ | ✅ |
| Rouleaux thermiques | ❌ | ❌ | 2 offerts |
| Installation + formation | ❌ | ❌ | ✅ (sur place) |

**Le Pack Boutique = Essentiel 1 an (25 000 F) + imprimante 58 mm + 2 rouleaux + installation (35 000 F).** Le logiciel est facturé normalement dans le pack — il n'est jamais offert. Au renouvellement (année 2), le client Pack paie 25 000 F/an comme tout le monde.

### Pourquoi 25 000 F/an ?

| Référence | Montant | Ce que ça justifie |
|---|---|---|
| Valeur délivrée (crédits récupérés) | ~60 000 F/an | **25 000 F/an = 40 % de la valeur** → prix juste |
| Coût mensuel des concurrents payants | 11 400–18 400 F/mois | Lissafi (2 083 F/mois) est **5 à 9× moins cher que leur mois** |
| Psycho prix | 68 F/jour | « Moins qu'un sachet d'eau + un ticket de transport » |

### Activation Premium
1. Commerçant te paie (espèces ou Orange Money/Moov Money)
2. Tu ouvres l'écran admin sur son téléphone (protégé par code PIN)
3. Tu entres le code d'activation
4. Premium actif pour 365 jours

### Économie du Pack Boutique (par unité)

| Poste | Montant |
|---|---|
| Prix de vente | **60 000 F** |
| Coût imprimante (fournisseur local) | −20 000 F |
| Coût 2 rouleaux | −1 000 F |
| Licence Essentiel 1 an (incluse) | −25 000 F |
| **Marge nette** | **14 000 F** |

Zéro stock : dropshipping local — le client commande → achat fournisseur → installation → encaissement.

### Projection revenus (scénario réaliste, mix 80 % Essentiel / 20 % Pack)

**Année 1 — 50 clients payants :**

| Poste | Calcul | Montant |
|---|---|---|
| Revenu logiciel | 50 × 25 000 | 1 250 000 F |
| Marge matérielle | 10 × 14 000 | 140 000 F |
| Chiffre d'affaires brut | | 1 390 000 F |
| Charges (domaine 8 000 F/an) | | −8 000 F |
| **Résultat net** | | **1 382 000 F** |
| **Net / mois** | | **115 000 F** |

**Année 2 — 200 clients payants (150 nouveaux) :**

| Poste | Calcul | Montant |
|---|---|---|
| Revenu logiciel | 200 × 25 000 | 5 000 000 F |
| Marge matérielle | 30 × 14 000 | 420 000 F |
| Chiffre d'affaires brut | | 5 420 000 F |
| Charges | | −8 000 F |
| **Résultat net** | | **5 412 000 F** |
| **Net / mois** | | **451 000 F** |

**Année 3 — 500 clients payants (300 nouveaux) :**

| Poste | Calcul | Montant |
|---|---|---|
| Revenu logiciel | 500 × 25 000 | 12 500 000 F |
| Marge matérielle | 60 × 14 000 | 840 000 F |
| Chiffre d'affaires brut | | 13 340 000 F |
| Charges (domaine + hébergement ~5 000 F/mois) | | −68 000 F |
| **Résultat net** | | **13 272 000 F** |
| **Net / mois** | | **1 106 000 F** |

### Comparaison ancien prix (10 000 F/an)

| | Ancien | Nouveau | × |
|---|---|---|---|
| Année 1 net/mois | 41 000 F | **115 000 F** | ×2,8 |
| Année 2 net/mois | 166 000 F | **451 000 F** | ×2,7 |
| Année 3 net/mois | 412 000 F | **1 106 000 F** | ×2,7 |

---

## 4. SPÉCIFICATION TECHNIQUE (POUR DÉVELOPPEMENT)

### Stack

| Couche | Technologie |
|---|---|
| Langage | **Kotlin** |
| UI | **Jetpack Compose** (moderne, moins de code) |
| Base de données | **Room** (au-dessus de SQLite) |
| Sync background | **WorkManager** |
| Scan codes-barres | **CameraX + ML Kit Barcode Scanning** (Google, gratuit) |
| Génération ticket | **iTextPDF** ou canvas Android → image |
| Bluetooth imprimante | **BluetoothSocket** API native Android |
| Min SDK | **API 24 (Android 7.0)** — couvre 95% des téléphones en circulation |
| Target SDK | API 34 (Android 14) |

### Architecture

```
┌─────────────────────────────────────────────┐
│              LISSAFI APK                     │
│                                              │
│  UI Layer (Jetpack Compose)                  │
│  ├── MainScreen (caisse)                     │
│  ├── ProductsScreen (catalogue)              │
│  ├── ClientsScreen (crédits/débiteurs)       │
│  ├── ReportsScreen (stats, Premium only)     │
│  ├── SettingsScreen                          │
│  └── AdminScreen (activation, protégé PIN)   │
│                                              │
│  ViewModel Layer                             │
│  ├── CartViewModel                           │
│  ├── ProductViewModel                        │
│  ├── ClientViewModel                         │
│  └── ReportViewModel                         │
│                                              │
│  Data Layer (Room)                           │
│  ├── ProductDao                              │
│  ├── SaleDao                                 │
│  ├── ClientDao                               │
│  └── SyncDao (queue de synchro)              │
│                                              │
│  └── SQLite (source unique de vérité)        │
│                                              │
│  Services                                    │
│  ├── SyncWorker (WorkManager, fond)          │
│  ├── TicketGenerator                         │
│  ├── BluetoothPrinterService                 │
│  └── BarcodeScanner (CameraX + ML Kit)       │
│                                              │
└─────────────────────────────────────────────┘
```

### Principe fondamental : LOCAL-FIRST

Chaque opération écrit D'ABORD dans Room (SQLite locale). La synchro cloud est secondaire, asynchrone, et ne bloque jamais l'utilisateur.

```
VENTE : Scan → lookup Room → ajout panier (memoire) → encaisser → écriture Room → OK
                                                                         ↓
                                                                  WorkManager
                                                                  (si réseau)
                                                                         ↓
                                                                   Cloud (backup)
```

---

## 5. ARCHITECTURE BASE DE DONNÉES LOCALE (Room)

### Table `products`

```kotlin
@Entity(tableName = "products")
data class Product(
    @PrimaryKey val barcode: String,        // code-barres, ou "MANUAL-{timestamp}" si sans code-barres
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "buy_price") val buyPrice: Int,    // prix achat, en FCFA
    @ColumnInfo(name = "sell_price") val sellPrice: Int,  // prix vente, en FCFA
    @ColumnInfo(name = "stock") val stock: Int,           // quantité en stock
    @ColumnInfo(name = "min_stock") val minStock: Int = 5, // alerte stock bas
    @ColumnInfo(name = "category") val category: String = "",
    @ColumnInfo(name = "has_barcode") val hasBarcode: Boolean,  // true si code-barres réel
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
```

### Table `sales`

```kotlin
@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "date") val date: Long,            // timestamp
    @ColumnInfo(name = "total") val total: Int,            // FCFA
    @ColumnInfo(name = "amount_paid") val amountPaid: Int, // FCFA, montant donné par client
    @ColumnInfo(name = "change_given") val changeGiven: Int, // monnaie rendue
    @ColumnInfo(name = "is_credit") val isCredit: Boolean,
    @ColumnInfo(name = "client_id") val clientId: String? = null,
    @ColumnInfo(name = "synced") val synced: Boolean = false
)

@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sale_id") val saleId: Long,
    @ColumnInfo(name = "barcode") val barcode: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "price") val price: Int,
    @ColumnInfo(name = "quantity") val quantity: Double = 1.0
)
```

### Table `clients`

```kotlin
@Entity(tableName = "clients")
data class Client(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "phone") val phone: String = "",
    @ColumnInfo(name = "total_debt") val totalDebt: Int = 0,   // FCFA, dette totale
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(tableName = "debt_transactions")
data class DebtTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "client_id") val clientId: String,
    @ColumnInfo(name = "sale_id") val saleId: Long? = null,   // vente à crédit
    @ColumnInfo(name = "amount") val amount: Int,              // positif = dette, négatif = remboursement
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "note") val note: String = ""
)
```

### Table `app_settings`

```kotlin
@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "value") val value: String
)
// Clés utilisées :
// "is_premium" → "true"/"false"
// "premium_expiry" → timestamp
// "shop_name" → nom boutique
// "shop_phone" → téléphone boutique (pour ticket)
// "admin_pin" → code PIN admin
```

### Migrations

| Version | Changement |
|---|---|
| 1 | Tables initiales : products, sales, sale_items, clients, debt_transactions, app_settings |
| 2+ | Ajouts futurs : fournisseurs, dépenses, catégories |

---

## 6. ÉCRANS & PARCOURS UTILISATEUR

### 6.1 Écran principal — Caisse

```
┌─────────────────────────────────────────┐
│  LISSAFI                     ⚙️        │
│─────────────────────────────────────────│
│  🔍  Scanner ou rechercher un produit   │
│─────────────────────────────────────────│
│                                          │
│  PANIER (3 articles)                     │
│  ┌──────────────────────────────────┐   │
│  │ Maggi poulet  ×2       200 F    │   │
│  │ Lait Candia 1L ×1     1 500 F   │   │
│  │ Sucre au kg   ×1.5     750 F    │   │
│  │──────────────────────────────────│   │
│  │ TOTAL                 2 450 F    │   │
│  └──────────────────────────────────┘   │
│                                          │
│  Mode : ○ Comptant   ● Crédit           │
│  Client : [Moussa Diallo      ▼]        │
│                                          │
│  ┌──────────────────────────────────┐   │
│  │        ENCAISSER  2 450 F        │   │
│  └──────────────────────────────────┘   │
│                                          │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐   │
│  │Maggi │ │Sucre │ │Lait  │ │  +   │   │
│  │ 100F │ │ 500F │ │1500F │ │Ajout │   │
│  └──────┘ └──────┘ └──────┘ └──────┘   │
│           PRODUITS RÉCENTS              │
└─────────────────────────────────────────┘
```

### 6.2 Flux vente (comptant)

```
Écran caisse → Scan produit (bip) → Ajouté au panier (animé)
    → [répéter pour chaque produit]
    → Appui ENCAISSER
    → Dialog : "Montant donné par le client ?"
    → Saisie montant (ex: 3000)
    → Dialog : "Rendre 550 F au client"
    → Appui OK
    → Ticket WhatsApp ? [Oui] [Non]
    → Panier vidé. Prêt pour prochaine vente.
```

### 6.3 Flux vente (crédit)

```
Écran caisse → Sélection mode Crédit
    → Choix client dans liste (ou + Nouveau client)
    → Scan/ajout produits
    → Appui ENCAISSER
    → Pas de dialogue monnaie (crédit = pas de paiement)
    → Dette client incrémentée
    → Ticket WhatsApp ? [Oui] [Non]
    → Panier vidé.
```

### 6.4 Flux ajout rapide produit

```
Depuis écran caisse → Bouton "+" ou appui long sur "Ajout"
    → Dialog :
        "Nom du produit ?"  [________________]
        "Prix de vente ?"   [________________]
        "Prix d'achat ?"    [________________] (optionnel)
        "Quantité initiale ?" [________________] (optionnel)
        "A un code-barres ?" [Scanner] [Sans code-barres]
    → Appui AJOUTER
    → Produit ajouté au catalogue ET au panier en un geste
```

### 6.5 Écran Crédits clients

```
┌─────────────────────────────────────────┐
│  ← CRÉDITS CLIENTS                      │
│─────────────────────────────────────────│
│  Total dû : 47 500 F                    │
│                                          │
│  🔍 Rechercher un client                │
│─────────────────────────────────────────│
│  Moussa Diallo         12 500 F  ⚠️    │
│  Dernier achat : il y a 3 jours         │
│  ─────────────────────────────────────  │
│  Mariama Issa           8 000 F         │
│  Dernier achat : aujourd'hui            │
│  ─────────────────────────────────────  │
│  Oumarou Bako           5 500 F  🔴    │
│  Dernier achat : il y a 25 jours        │
│  ─────────────────────────────────────  │
│                                          │
│  ⚠️ = plus de 7j sans remboursement     │
│  🔴 = plus de 21j sans remboursement    │
│                                          │
│  [+ Nouveau client]                      │
└─────────────────────────────────────────┘
```

### 6.6 Écran Produits (catalogue)

```
┌─────────────────────────────────────────┐
│  ← CATALOGUE                 [➕]       │
│─────────────────────────────────────────│
│  🔍 Rechercher                          │
│─────────────────────────────────────────│
│  Lait Candia 1L                         │
│  Stock : 23  |  Vente : 1 500 F        │
│  ─────────────────────────────────────  │
│  Maggi poulet                           │
│  Stock : 5 ⚠️  |  Vente : 100 F       │
│  ─────────────────────────────────────  │
│  Sucre au kg                            │
│  Stock : —  |  Vente : 500 F           │
│                                         │
│  10/10 produits (Gratuit) ⬆ Passer à Essentiel │
└─────────────────────────────────────────┘
```

### 6.7 Écran Rapports (Premium uniquement)

```
┌─────────────────────────────────────────┐
│  ← RAPPORTS                             │
│─────────────────────────────────────────│
│  [Aujourd'hui] [Semaine] [Mois] [Perso] │
│─────────────────────────────────────────│
│  Ventes du jour : 47 500 F              │
│  Dont crédit :    12 500 F              │
│  Dont comptant :  35 000 F              │
│                                          │
│  Nb transactions : 23                   │
│  Panier moyen :    2 065 F              │
│                                          │
│  Top produits :                          │
│  1. Lait Candia 1L  — 12 ventes         │
│  2. Sucre au kg     — 8 ventes          │
│  3. Maggi poulet    — 7 ventes          │
│                                          │
│  [📤 Exporter CSV]                       │
└─────────────────────────────────────────┘
```

### 6.8 Écran Admin (protégé par PIN)

```
┌─────────────────────────────────────────┐
│  ADMINISTRATION                         │
│─────────────────────────────────────────│
│  Statut : ● Gratuit (9/10 produits)     │
│                                          │
│  Activer Premium :                       │
│  Code activation : [_______________]     │
│  [ACTIVER]                               │
│                                          │
│  ── ou ──                               │
│                                          │
│  Démo Premium (7 jours) :               │
│  [ACTIVER LA DÉMO]                      │
│                                          │
│  ─────────────────────────────────────  │
│  Informations :                          │
│  Premium jusqu'au : 15/08/2027          │
│  Client depuis le : 12/09/2026          │
│  Version app : 1.0.3                    │
│                                          │
│  ─────────────────────────────────────  │
│  Paramètres boutique :                   │
│  Nom : Chez Moussa                       │
│  Tél : 96 12 34 56                       │
│  [MODIFIER]                              │
│                                          │
│  [EXPORTER TOUTES LES DONNÉES]          │
│  [RÉINITIALISER L'APPLI] ⚠️             │
└─────────────────────────────────────────┘
```

---

## 7. COPYWRITING — TOUS LES TEXTES

### Écran d'accueil (première ouverture)

> *Bienvenue sur Lissafi !*
>
> *Votre caisse, votre stock, vos crédits. Tout dans votre téléphone. Même sans réseau.*
>
> *Commençons par ajouter votre premier produit.*
> *[AJOUTER UN PRODUIT]*

### Pitch 30 secondes (oral)

> *Tu sais combien tes clients te doivent, là, maintenant, sans ouvrir ton cahier ? Avec Lissafi, tu scannes le produit, le prix s'affiche, et le soir tu vois : ton chiffre, ton stock, et la liste de ceux qui te doivent. Ticket WhatsApp inclus. Sans réseau. Gratuit pour commencer.*

### Message WhatsApp présentation

> *Salam alaikum !*
>
> *Lissafi — l'appli de caisse des commerçants de Niamey.*
>
> *✅ Scan les codes-barres*
> *✅ Calcule la monnaie*
> *✅ Suit les crédits clients (plus jamais de dettes oubliées)*
> *✅ Gère ton stock*
> *✅ Marche sans réseau*
> *✅ Ticket WhatsApp pour tes clients*
>
> *C'est gratuit pour commencer. Je passe te montrer ?*

### Réponse à « Mon cahier me suffit »

> *Ton cahier, est-ce qu'il te dit combien Moussa te doit au total ? Et le mois dernier, as-tu récupéré TOUT ce qu'on te devait ? Un commerçant perd en moyenne 60 000 F de crédits oubliés par an. Lissafi est gratuit pour commencer — et même en payant 25 000 F/an, tu gagnes encore 35 000 F net. Teste 2 semaines gratos. Si tu ne récupères pas au moins 5 000 F de dettes, tu supprimes.*

### Messages dans l'appli

| Contexte | Texte |
|---|---|
| Limite produits atteinte (Gratuit) | *Tu as 10 produits. Passe à Essentiel pour en ajouter autant que tu veux. 25 000 F/an = 68 F/jour.* |
| Limite crédits atteinte (Gratuit) | *Tu suis déjà 10 débiteurs. Passe à Essentiel pour ne plus perdre un franc.* |
| Alerte stock bas | *Plus que 3 Maggi poulet en stock. Commander ?* |
| Résumé fin de journée | *Aujourd'hui : 52 500 F | 18 ventes | 3 crédits | Bravo !* |
| Hors-ligne | *Pas de réseau ? Pas de souci. Lissafi continue.* |
| Premium activé | *Bienvenue en Essentiel ! Tout est débloqué pour 365 jours. Merci pour ta confiance.* |
| Premium expire bientôt | *Ton Essentiel expire dans 15 jours. Contacte-nous pour renouveler. 25 000 F/an = 68 F/jour.* |

---

## 8. PLAN MARKETING 12 MOIS

### Mois 1-2 : Construction + pilotes

| Semaine | Action |
|---|---|
| S1 | Coder MVP avec DeepSeek |
| S2 | Tests internes + corrections |
| S3 | 5 commerçants pilotes. Installer, observer, corriger |
| S4 | V2 avec retours pilotes. Démarchage Grand Marché commence |

### Mois 3-4 : Early adopters

- Présence Grand Marché 4 matinées/semaine (7h-11h)
- Objectif : 10 installations/semaine, 15 clients payants fin M4
- Prix de lancement : 20 000 F/an pour les 10 premiers clients (« early adopter »)
- Recrutement 3 ambassadeurs (affichettes dans leur boutique)
- 1er témoignage vidéo WhatsApp

### Mois 5-6 : Effet réseau

- Programme parrainage actif (1 mois offert par filleul Premium)
- Groupes WhatsApp commerçants
- Partenariats grossistes (3 cibles)
- Objectif : 30 clients payants fin M6

### Mois 7-9 : Croissance organique

- Bouche-à-oreille devient canal principal
- Démarchage réduit à 2x/semaine
- Partenariats grossistes actifs
- Objectif : 40 clients payants fin M9

### Mois 10-12 : Consolidation

- Fidélisation et renouvellements
- Préparation expansion Zinder/Maradi
- Tests imprimantes avec importateur partenaire
- Objectif : 50 clients payants fin M12

---

## 9. PLAN DE LANCEMENT

### Phase 1 — Alpha (S1-S2)
- MVP fonctionnel : scan, panier, crédits, hors-ligne, ticket WhatsApp
- Tests sur ton téléphone + 1 ami
- APK signé prêt à distribuer

### Phase 2 — Beta pilotes (S3-S4)
- 5 commerçants pilotes (réseau personnel, famille)
- Installation + observation 2 jours
- Corrections bugs critiques
- Recueil retours (qu'est-ce qui est dur ? qu'est-ce qui manque ?)

### Phase 3 — Early Access (M2-M3)
- Démarchage Grand Marché 4x/semaine
- Conversion pilotes → Premium
- Premiers ambassadeurs + affichettes
- Témoignage vidéo WhatsApp

### Phase 4 — Lancement (M4-M6)
- Programme parrainage
- Partenariats grossistes
- 30 clients payants cible

---

## 10. ROADMAP 90 JOURS (DÉVELOPPEMENT)

### Semaine 1 — MVP Core

| Jour | Tâche | Priorité |
|---|---|---|
| J1 | Setup projet Kotlin/Compose. Room DB (tables products, sales, clients). | P0 |
| J2 | Écran caisse : panier (liste modifiable), bouton encaisser, dialogue monnaie. | P0 |
| J3 | Scan code-barres (CameraX + ML Kit). Lookup Room → ajout panier. | P0 |
| J4 | Crédits clients : ajout client, liste débiteurs, ajout dette, remboursement. | P0 |
| J5 | Ticket WhatsApp : génération image + partage via Intent. Catalogue basique (liste, ajout). | P0 |
| J6 | Mode hors-ligne vérifié (tout fonctionne sans réseau). Limites gratuites (10 produits, 10 crédits). | P0 |
| J7 | Tests complets. Corrections bugs bloquants. APK v0.1 signé. | P0 |

### Semaine 2 — Stabilisation + Premium

| Jour | Tâche | Priorité |
|---|---|---|
| J8-9 | Écran admin (PIN protégé). Activation Essentiel par code. | P1 |
| J10 | Écran rapports (journalier, hebdo, mensuel). Export CSV. | P1 |
| J11 | Résumé fin de journée. Alertes stock bas. | P1 |
| J12 | Gestion stock complète (entrées/sorties manuelles). | P1 |
| J13-14 | Tests complets. Corrections. APK v0.2. Tests sur téléphones bas de gamme. | P0 |

### Semaine 3 — Pilotes terrain

| Jour | Tâche |
|---|---|
| J15-17 | Installer sur 5 téléphones de commerçants pilotes |
| J18-19 | Observer 2 jours. Noter TOUS les problèmes |
| J20-21 | Corriger bugs critiques + améliorations UX remontées |

### Semaine 4 — Version publique

| Jour | Tâche |
|---|---|
| J22-23 | APK v1.0 final. Page WhatsApp Business. |
| J24-28 | Démarchage Grand Marché. 10 installations cible. |

### Semaine 5-12 — Itération continue

- Démarchage terrain 3-4x/semaine
- Corrections bugs remontés par utilisateurs
- Itérations rapides (nouvelle APK chaque semaine si nécessaire)
- Mise à jour manuelle chez les utilisateurs existants

---

## 11. PROJECTIONS FINANCIÈRES

### Scénario réaliste

| | Année 1 | Année 2 | Année 3 |
|---|---|---|---|
| **Clients payants (fin période)** | 50 | 200 | 500 |
| Nouveaux dans l'année | 50 | 150 | 300 |
| Dont Pack Boutique (20 % des nouveaux) | 10 | 30 | 60 |
| Revenus logiciel (25 000 F/an × total clients) | 1 250 000 F | 5 000 000 F | 12 500 000 F |
| Marge matérielle (14 000 F × nouveaux packs) | 140 000 F | 420 000 F | 840 000 F |
| Chiffre d'affaires brut | 1 390 000 F | 5 420 000 F | 13 340 000 F |
| Charges (domaine 8k/an, hébergement 0→60k) | -8 000 F | -8 000 F | -68 000 F* |
| **Résultat net** | **1 382 000 F** | **5 412 000 F** | **13 272 000 F** |
| **Net/mois** | **115 000 F** | **451 000 F** | **1 106 000 F** |

*\*Année 3 : hébergement devient payant (~5 000 F/mois) + domaine.*

### Seuil de rentabilité

- Investissement initial : **8 000 FCFA** (domaine)
- Rentabilisé au **1er client payant**

### Multiples scénarios année 1

| Scénario | Clients payants | Revenu logiciel | Marge Pack | Résultat net | Net/mois |
|---|---|---|---|---|---|
| Conservateur | 25 (20 Ess. + 5 Pack) | 625 000 F | 70 000 F | 687 000 F | 57 000 F |
| Réaliste | 50 (40 Ess. + 10 Pack) | 1 250 000 F | 140 000 F | 1 382 000 F | 115 000 F |
| Optimiste | 75 (60 Ess. + 15 Pack) | 1 875 000 F | 210 000 F | 2 077 000 F | 173 000 F |

---

## 12. RISQUES & MITIGATIONS

| # | Risque | Prob. | Mitigation |
|---|---|---|---|
| 1 | Commerçants ne voient pas la valeur | Élevée | Gratuit généreux. Démo en conditions réelles. ROI chiffré. |
| 2 | Bug perte de données | Moyenne | Backup auto quotidien. Protocole restauration testé. |
| 3 | Fatigue/découragement fondateur | Élevée | Célébrer petites victoires. 1er client payant = cap psychologique. |
| 4 | Smartphones incompatibles | Élevée | Tester sur 5 modèles avant lancement. Liste « téléphones recommandés ». |
| 5 | Concurrent bien financé | Faible | Présence terrain = barrière. Partenariat plutôt que compétition. |
| 6 | Difficulté collecte paiements | Moyenne | Paiement annuel upfront. Mobile money accepté. |

---

## 13. ACTIONS PRIORITAIRES (MAINTENANT)

| # | Action | Deadline |
|---|---|---|
| **1** | Coder MVP avec DeepSeek | Semaine 1 |
| **2** | Acheter nom de domaine (lissafi.ne ou .com) | Semaine 1 |
| **3** | Créer compte Cloudflare (gratuit) pour backend | Semaine 1 |
| **4** | Tester sur 5 commerçants | Semaine 3 |
| **5** | Premier client payant | Avant fin mois 2 |
| **6** | Présence Grand Marché 4x/semaine | Dès semaine 5 |

---

## ANNEXE A — Génération des codes d'activation Essentiel

Pour ne pas avoir à coder un backend complexe tout de suite :

```
code_activation = hash(commercant_id + mois_annee + secret)
```

Tu génères les codes sur ton PC avec un petit script. Tu les donnes au commerçant. L'appli vérifie localement.

```kotlin
// Dans l'appli, validation simplifiée :
fun validateCode(code: String): Boolean {
    // Format : LISSAFI-XXXX-XXXX-XXXX
    // Vérification checksum local
    // Active Essentiel pour 365 jours
}
```

Pour la V1, le plus simple : une liste de 100 codes pré-générés stockés dans l'APK. Chaque code utilisable une seule fois. Tu actives, tu barres le code de ta liste.

---

## ANNEXE B — Modèle APK de test pour téléphones bas de gamme

| Modèle | RAM | Android | Prix estimé | Test requis |
|---|---|---|---|---|
| Tecno Pop 7 | 2 Go | 12 (Go) | ~45 000 F | Scan, perf générale |
| Itel A60 | 2 Go | 12 (Go) | ~35 000 F | Scan, stockage |
| Samsung A04 | 3 Go | 12 | ~55 000 F | Bluetooth imprimante |
| Infinix Smart 8 | 2 Go | 13 (Go) | ~50 000 F | Scan, UI |
| Nokia C22 | 2 Go | 13 (Go) | ~40 000 F | Compatibilité générale |

Faire un tour au Grand Marché électronique pour trouver ces modèles. Tester l'APK sur au moins 3 avant lancement.

---

*Document final — mis à jour le 9 août 2026. Nom : Lissafi. Prix : Gratuit / Essentiel 25 000 F/an / Pack Boutique 60 000 F. Activation manuelle. Dropshipping imprimantes.*
