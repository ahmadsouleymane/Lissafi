# Spec — Pricing v2 : deux plans + Pack, limites par niveau

**Date** : 2026-08-12
**Statut** : validé par l'utilisateur (brainstorming), implémenté landing + app Android

## Objectif

Repartir de zéro sur le pricing après rejet du précédent (plan unique « Essentiel »
jugé mal positionné, prix intermédiaire 35 000 F/an jugé trop élevé). Nouveau modèle
à **deux plans payants + un pack matériel**, avec des **limites réelles par niveau**
(enforcement dans l'app, pas seulement du marketing), et un cadrage « prix de lancement »
pour préparer une future hausse.

## Décisions validées (brainstorming)

| | **Gratuit** | **Lissafi Plus** | **Lissafi Business** |
|---|---|---|---|
| Prix | 0 F | **30 000 F/an** (82 F/jour) | **75 000 F/an** (205 F/jour) |
| Produits | 10 | 100 | illimité |
| Clients | 10 | 100 | illimité |
| Ventes | **10/jour** | illimitées | illimitées |
| Historique | illimité | illimité | illimité |
| Stock / rapports / export | — | ✅ | ✅ |
| Multi-utilisateurs | — | — | ✅ |
| Sauvegarde cloud renforcée | — | — | ✅ |
| Accompagnement + formation | — | — | ✅ |
| Support prioritaire | — | — | ✅ |

**Pack Lancement — 60 000 F (une fois)** : imprimante thermique 58 mm POS + 2 rouleaux
de papier thermique + **1 an Lissafi Plus offert**. Coût imprimante à l'achat < 15 000 F
→ marge du pack saine.

**Logique** : Plus = plan de volume (hameçon, limites hautes mais réelles), Business =
marge (services + illimité), Pack = cash + équipement. « Prix de lancement » sur toute
la section tarifs : le prix augmentera après le lancement.

## Application Android — enforcement

`PremiumManager.kt` (service) est le seul point de décision, appelé par les ViewModels.

- **Niveau** : `enum Plan { FREE, PLUS, BUSINESS }`, lu via `getPlan()` depuis le setting
  `plan` (`"plus"` | `"business"`) combiné à `is_premium` + expiration (comportement
  existant conservé).
- **`canAddProduct()`** : FREE ≤ 10, PLUS ≤ 100, BUSINESS ∞ (via `getProductCount()`).
- **`canAddClient()`** : FREE ≤ 10, PLUS ≤ 100, BUSINESS ∞ (via `getClientCount()`).
- **`canMakeSale()`** : FREE ≤ 10 ventes par jour (via `countSalesBetween(todayRange())`),
  PLUS et BUSINESS illimité. Bloqué dans `CartViewModel.encaisser()` → retour `false` →
  `CaisseScreen` affiche un toast et ne vide pas le panier.
- **Activation** : `activateDemo()` = 7 jours en **Plus** ; `activateWithCode()` active
  **Plus** (codes `LISSAFI-PREMIUM-*`) ou **Business** (codes `LISSAFI-BUSINESS-*`).
- **Renommage** : toutes les chaînes utilisateur passent de « Premium/Essentiel » à
  « Lissafi Plus / Lissafi Business » (Réglages, dialogues de limite, toasts, message
  d'activation WhatsApp).
- **Réglages** : le statut affiche le nom du plan et son plafond (free : 10 produits /
  10 clients / 10 ventes ; plus : 100 produits / 100 clients ; business : tout illimité).

## Landing page

- Section tarifs : 3 paliers (Gratuit / Plus / Business) + **Pack Lancement** pleine
  largeur, cadrage « Prix de lancement — ils augmenteront après le lancement ».
- FAQ alignée (limites du gratuit = 10 produits / 10 clients / 10 ventes par jour ;
  paiement Plus/Business en espèces ou mobile money, activation dans l'app).
- Meta + données structurées (`index.html`) : prix Lissafi Plus = 30 000 F/an.
- Un seul CTA partout : « Télécharger l'APK » (aucun contact WhatsApp sur la landing).

## Hors périmètre

- Back-office : l'activation Business par un admin doit écrire `is_premium=true` +
  `plan=business` dans `app_settings` (le setting est déjà synchronisé ; à faire dans
  une étape suivante si besoin).
- Ajustement de `plan-lissafi.md` : mise à jour des chiffres (faite sur les points
  saillants ; les projections de revenus restent à recalculer par l'utilisateur).

## Vérification

- `./gradlew assembleRelease` sans erreur, APK copié dans `landing/public/lissafi.apk`.
- `cd landing && npm run build` sans erreur.
- Vérif visuelle desktop/mobile de la section tarifs.
- Plus aucune chaîne « Premium/Essentiel » visible dans l'app (grep dans `ui/`).
