# Spec — Onboarding Android (Lissafi)

**Date** : 2026-08-09
**Statut** : validé par l'utilisateur (design approuvé)

## Objectif

Afficher un onboarding de **4 écrans** à la première ouverture de l'app pour
présenter Lissafi aux nouveaux utilisateurs, **avant** l'écran d'inscription.
Affiché une seule fois par téléphone, avec bouton « Passer ».
Ignoré si l'utilisateur est déjà connecté.

## Contexte

Actuellement `LissafiNavHost` démarre directement sur `AUTH` (ou `CAISSE` si
connecté) — l'app tombe immédiatement sur l'inscription sans aucune
présentation. L'onboarding ajoute une étape de découverte au premier lancement.

## Comportement (validé)

- **4 écrans glissables** (`HorizontalPager`), indicateurs à points.
- Bouton **« Passer »** en haut à droite, **« Continuer → »** sur les pages
  1-3, **« Commencer »** sur la page 4.
- « Passer » ou « Commencer » → mémorise `onboarding_seen = true`, navigue vers
  l'écran d'inscription (`AUTH`).
- **Une seule fois** : le flag est stocké au niveau du téléphone
  (`SharedPreferences`). Après une déconnexion, l'onboarding ne réapparaît pas.
- **Déjà connecté** → l'onboarding est ignoré (startDestination = `CAISSE`).
- **Retour arrière** sur l'onboarding → ferme l'app (comportement standard).
- **Rotation d'écran** → l'état du pager est conservé (`rememberPagerState`).

## Contenu des 4 écrans

| # | Titre | Visuel | Texte |
|---|-------|--------|-------|
| 1 | Bienvenue | Logo `logo_auth` (drawable existant) | « Ta boutique dans ta poche » — Vends, encaisse et suis tes clients |
| 2 | Encaisser vite | Icône `LissafiIcons.Caisse` dans une grande carte arrondie | Panier en 2 touches, paiement, ticket imprimé ou WhatsApp |
| 3 | Clients & crédits | Icône `LissafiIcons.Clients` dans une grande carte arrondie | Vente à crédit, suivi des dettes, chaque client dans ta liste |
| 4 | Tes données en sécurité | Icône cloud (Material) dans une grande carte arrondie | Marche même hors-ligne, sauvegarde cloud automatique |

Style visuel : **identique au redesign actuel** — cartes flottantes, ombres
douces, coins arrondis (12-16 dp), couleurs du thème (`Surface`, `Primary`,
`PrimaryContainer`, `Background`).

## Fichiers

### 1. `data/OnboardingManager.kt` *(nouveau)*

Petit object suivant le pattern de `SupabaseManager` :

```kotlin
object OnboardingManager {
    private const val PREFS_NAME = "lissafi_prefs"
    private const val KEY_ONBOARDING_SEEN = "onboarding_seen"

    fun isCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_SEEN, false)

    fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply()
    }
}
```

### 2. `ui/screen/OnboardingScreen.kt` *(nouveau)*

Composable `OnboardingScreen(onFinish: () -> Unit)` :

- `HorizontalPager(state = rememberPagerState(pageCount = { 4 }))`
- 4 pages (`OnboardingPage(icon, title, description)` composable privé réutilisé)
- Colonne commune : visuel (carte), titre, sous-texte
- Indicateurs à points (état `PagerState`)
- Boutons :
  - **Passer** : `TextButton` aligné en haut à droite → `onFinish()`
  - **Continuer / Commencer** : `Button` en bas, `scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }` ou `onFinish()` sur la dernière page
- À la dernière page, le label devient « Commencer »

### 3. `ui/navigation/LissafiNavHost.kt` *(modifié)*

- Ajouter `const val ONBOARDING = "onboarding"` à `Routes`.
- Calculer `startDestination` :

```kotlin
val startDestination = when {
    isLoggedIn -> Routes.CAISSE
    !OnboardingManager.isCompleted(context) -> Routes.ONBOARDING
    else -> Routes.AUTH
}
```

- Ajouter `composable(Routes.ONBOARDING)` :

```kotlin
composable(Routes.ONBOARDING) {
    OnboardingScreen(onFinish = {
        OnboardingManager.markCompleted(context)
        navController.navigate(Routes.AUTH) {
            popUpTo(Routes.ONBOARDING) { inclusive = true }
        }
    })
}
```

- Adapter le `LaunchedEffect(isLoggedIn)` existant (lignes 96-107) : il
  navigue actuellement vers `AUTH` dès que `isLoggedIn == false`, ce qui
  **écraserait l'onboarding au 1er lancement**. Le corriger ainsi :

```kotlin
LaunchedEffect(isLoggedIn) {
    if (isLoggedIn) {
        app.syncManager.syncInBackground()
        navController.navigate(Routes.CAISSE) {
            popUpTo(Routes.AUTH) { inclusive = true }
        }
    } else if (OnboardingManager.isCompleted(context)) {
        // Déjà passé par l'onboarding (ou déconnecté) → retour à l'inscription
        navController.navigate(Routes.AUTH) {
            popUpTo(0) { inclusive = true }
        }
    }
    // Sinon : on reste sur ONBOARDING, ne rien faire
}
```

  → Quand `isLoggedIn` devient vrai → `CAISSE`. Quand il devient faux et que
  l'onboarding a déjà été vu → `AUTH`. Quand il devient faux et que l'onboarding
  n'a pas été vu → rester sur `ONBOARDING`.

## Cas particuliers & décisions

- **Flag au niveau du téléphone, pas du compte** : un même appareil n'affiche
  l'onboarding qu'une fois, quel que soit le compte connecté ensuite. C'est le
  comportement demandé (« Passer, une seule fois »).
- **Pas de retour dans Réglages** : décidé (pas de « Revoir la présentation »).
- **Pas de stockage du flag en base `app_settings`** : le flag est local au
  téléphone, pas synchronisé — un 2e appareil du même compte reverra
  l'onboarding (comportement souhaité : découverte sur chaque nouvel appareil).

## Vérification

- Compilation : `export JAVA_HOME=...` puis `./gradlew assembleDebug`.
- Test manuel : 1re installation → onboarding → Passer → inscription ;
  réouverture → inscription directe ; compte connecté → caisse directe.

## Hors périmètre (autres piliers du lancement)

Back-office full data, landing page, audit sécurité/synchro — traités
séparément dans leurs propres specs/plans.
