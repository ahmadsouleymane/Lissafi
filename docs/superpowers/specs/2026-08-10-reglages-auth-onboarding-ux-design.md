# Amélioration UX Réglages + Auth + Onboarding — Phase 3

**Date** : 2026-08-10
**Statut** : Approuvé
**Portée** : Écrans Réglages, Auth et Onboarding. C'est la dernière phase du plan d'amélioration UX (Phases 1 et 2 déjà fusionnées dans `main`).

## Contexte

Les écrans Réglages, Auth et Onboarding sont **déjà au standard** du design system (bannières, cartes, sections, validation email temps réel, reset password, pager d'onboarding). Contrairement aux phases précédentes, il n'y a presque pas de nouvelle donnée à exposer. La Phase 3 se concentre donc sur les **micro-interactions** (haptique, transitions animées), un **insight** utile (progression de la limite gratuite) et de petits polishs. Design system intact : palette vert forêt / orange brûlé, Inter, Lucide, `LissafiCard`, etc.

Contraintes : zéro nouvelle dépendance, zéro nouvelle requête DB nouvelle-signature (on réutilise `getProductCount()`/`getClientCount()` du repository, déjà existants). Une seule modification de ViewModel (`SettingsViewModel`) pour exposer la progression de limite. Commits en français, conventional commits, jamais de mention d'IA/Claude.

## 1. Écran Réglages (`SettingsScreen.kt` + `SettingsViewModel.kt`)

### Progression de la limite gratuite
- `SettingsState` gagne `clientCount: Int = 0` (`productCount: Int` existe déjà, actuellement inutilisé).
- `loadSettings()` peuple `productCount` et `clientCount` via `repository.getProductCount()` / `repository.getClientCount()`.
- Dans la bannière "Version gratuite" de `SettingsScreen`, sous le texte "Limitée à 10 produits et 10 clients", afficher une mini-barre de progression (deux segments côte à côte) et le texte `X/10 produits · Y/10 clients`. Les seuils viennent de `PremiumManager.MAX_FREE_PRODUCTS` (10) et `MAX_FREE_CREDITS` (10) — les référencer, ne pas hardcoder (sauf si inaccessible, auquel cas constantes locales commentées).
- Quand `state.isPremium` : aucun changement (la bannière premium reste telle quelle).

### Haptique (`LocalHapticFeedback`, pattern des phases précédentes)
- Enregistrement boutique (`ShopInfoDialog` → `onSave`) : `HapticFeedbackType.LongPress`.
- Envoi de signalement (`ReportIssueDialog` → `onSend`) : `HapticFeedbackType.LongPress`.
- Déconnexion (le `TextButton` rouge du bloc COMPTE) : `HapticFeedbackType.LongPress`.

## 2. Écran Auth (`AuthScreen.kt`)

### Transition animée entre formulaires
- Envelopper le `when (state.mode)` (SIGN_IN / SIGN_UP / RESET_PASSWORD) dans un `AnimatedContent(targetState = state.mode)` avec `fadeIn + slideInVertically { -it / 8 }` en entrée et `fadeOut + slideOutVertically { it / 8 }` en sortie (léger, pas de désorientation). Le `SegmentedControl` reste hors de l'`AnimatedContent` (il ne doit pas se re-transitionner).

### Haptique
- Bascule d'onglets (`SegmentedControl` `onSelect`) : `HapticFeedbackType.LongPress`.
- Soumission (`AuthSubmitButton`/`AuthResetButton` `onClick`) : `HapticFeedbackType.LongPress`. Note assumée : l'haptique se déclenche au clic, donc même si le serveur répond en erreur — le feedback d'erreur visuel (`AuthMessage`) existe déjà et reste la source de vérité.

### Guidage contextuel
Le bandeau bénéfices de l'inscription et la validation email temps réel existent déjà — **aucun changement** (déjà conforme à l'objectif de guidage).

## 3. Écran Onboarding (`OnboardingScreen.kt`)

### Libellé du bouton animé
- Envelopper le `Text` du bouton principal dans un `AnimatedContent(targetState = pagerState.currentPage == OnboardingSlides.lastIndex)` pour animer la bascule `Continuer` ↔ `Commencer` (fade + léger slide).

### Haptique
- Clic sur le bouton principal (Continuer/Commencer) et sur `Passer` : `HapticFeedbackType.LongPress`.

## 4. Composants

Aucun nouveau composant partagé. `AnimatedContent` est un composant Compose standard déjà utilisé dans le projet (`CaisseScreen`, `ReportsScreen`). `LocalHapticFeedback`/`HapticFeedbackType` sont le pattern établi depuis la Phase 1.

## 5. Hors périmètre (Phase 3)

- Pas de dark mode, pas de nouvelle dépendance, pas de nouvelle requête DB nouvelle-signature.
- Pas de refonte des écrans (ils sont déjà au standard).
- Pas de changement du backend (Supabase/AuthManager) — uniquement de l'UI et un ViewModel.

## 6. Vérification

- `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug` doit passer.
- Aucun test unitaire dans le repo — vérification manuelle sur device/émulateur (limitée : l'auth Supabase backend n'est pas configurée dans cet environnement, donc l'écran Auth/Réglages complet n'est pas testable de bout en bout ; vérifier au minimum le lancement sans crash) :
  - Réglages : bannière gratuite affiche `X/10 produits · Y/10 clients` + barre de progression, haptique sur les 3 actions.
  - Auth : transitions animées entre les 3 modes, haptique sur les onglets et la soumission.
  - Onboarding : libellé animé Continuer↔Commencer, haptique sur le bouton et Passer.
