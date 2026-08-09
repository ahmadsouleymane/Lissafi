# Spec — Audit production : signature APK, bugs, guide Supabase (Lissafi)

**Date** : 2026-08-09
**Statut** : validé par l'utilisateur (choix « Signing + bugs + guide Supabase »)

## Objectif

Mettre l'app Android en état de **distribution** : produire un APK **release
signé** installable, corriger 2 bugs UX identifiés, et documenter le réglage
Supabase Auth qui bloque l'inscription. Rendre l'app réellement prête pour le
lancement officiel.

## Audit (constats)

**Sain (vérifié, rien à faire) :**
- Pas de clé `service_role` dans l'app (seule la clé `anon`, publique par
  design) · RLS active sur toutes les tables · `allowBackup="false"` ·
  HTTPS uniquement (pas de cleartext autorisé) · un seul composant exporté
  (MainActivity, launcher) · pas de logs de tokens/mots de passe ·
  R8 (`isMinifyEnabled = true`) activé pour le release, proguard garde
  `com.lissafi.app.data.entity.**` (sérialisation) et les annotations.

**Blocage 1 — APK release non signé :** `buildTypes.release` n'a pas de
`signingConfig` → `app-release-unsigned.apk`. Impossible de distribuer une
vraie release installable.

**Blocage 2 — Inscription échoue sur le backend :** la création de compte
renvoie « Erreur d'inscription » (réponse GoTrue). Cause probable : signups
désactivés ou confirmation par email requise dans la config Supabase Auth.
Non corrigeable depuis le code → guide documenté.

**Bug A — Double navigation Caisse au démarrage connecté :**
`LissafiNavHost.kt` `LaunchedEffect(isLoggedIn)`, branche `if (isLoggedIn)` :
au cold start connecté (startDestination = CAISSE), `navigate(CAISSE)` empile
un **doublon** et `popUpTo(AUTH)` cible une route absente de la pile (no-op).

**Bug B — Bouton « Passer » de l'onboarding sous la barre de statut :**
`OnboardingScreen.kt` : le `Box` racine n'a pas de `statusBarsPadding()`, et le
NavHost est en edge-to-edge sans inset haut (le Scaffold n'ajoute que les
insets horizontaux + bas). Le bouton « Passer » (aligné en haut, `top = 24.dp`)
peut chevaucher la barre de statut sur les écrans à encoche haute.

## Fonctionnalités (validées)

### 1. Signature release

- **Keystore** : générer `app/lissafi-release.keystore` (keytool, RSA 2048,
  validité 10 000 jours, alias `lissafi`, mot de passe fort généré aléatoirement).
  **Jamais commité**.
- **`keystore.properties`** (racine, **gitignoré**) : `storeFile`,
  `storePassword`, `keyAlias`, `keyPassword` — lu par le build.
- **`app/build.gradle.kts`** : ajouter `signingConfigs { create("release") }`
  qui lit `keystore.properties` (avec valeurs par défaut), et appliquer
  `signingConfig = signingConfigs.getByName("release")` au `buildTypes.release`.
- **`.gitignore`** : ajouter `*.keystore` et `keystore.properties`.
- **Build + test** : `./gradlew assembleRelease` → `app-release.apk` signé ;
  vérifier la signature (`apksigner verify`), **l'installer sur l'émulateur**
  et faire un smoke test (lancement sans crash, écran d'auth) — valide que R8
  n'a rien cassé.
- **Mettre à jour la landing** : `npm run update-apk <release signé>` pour que
  le téléchargement serve l'APK release signé (plus léger que le debug).

### 2. Bug A — double navigation Caisse

Dans `LissafiNavHost.kt`, branche `if (isLoggedIn)` du `LaunchedEffect` :
ajouter `launchSingleTop = true` à `navigate(Routes.CAISSE)`. Effets :
- Cold start connecté : startDestination = CAISSE → `navigate(CAISSE)` devient
  un no-op (singleTop), plus de doublon.
- Après connexion depuis AUTH : AUTH est dépilé (`popUpTo(AUTH) inclusive`),
  CAISSE reste unique — comportement inchangé.

### 3. Bug B — bouton « Passer » sous la barre de statut

Dans `OnboardingScreen.kt`, ajouter `.statusBarsPadding()` au `Box` racine
(`Modifier.fillMaxSize().background(Background).statusBarsPadding()`). Le
contenu (bouton « Passer » en haut) se place alors sous la barre de statut.

### 4. Guide Supabase (documentation)

Créer `docs/lancement-supabase-auth.md` : étapes pour activer l'inscription —
dashboard Supabase (projet `fnyuhpfzkvunscuylvqv`) → Authentication →
Providers → Email → cocher **« Enable Signups »** et décocher **« Confirm
email »** (ou l'expliquer), puis tester une inscription. Inclure aussi :
vérifier la RLS (déjà en place), et note sur le compte démo (optionnel pour
les vidéos).

## Fichiers

- Modify : `app/build.gradle.kts`, `app/src/main/java/com/lissafi/app/ui/navigation/LissafiNavHost.kt`,
  `app/src/main/java/com/lissafi/app/ui/screen/OnboardingScreen.kt`, `.gitignore` (racine).
- Create : `app/lissafi-release.keystore` (binaire, **gitignoré**),
  `keystore.properties` (**gitignoré**), `docs/lancement-supabase-auth.md`.
- Update (landing) : `landing/public/lissafi.apk` (via `npm run update-apk`).

## Décisions techniques

- **Keystore en local uniquement** : il ne doit JAMAIS être commité ni envoyé.
  L'utilisateur doit le sauvegarder (fichier + mots de passe dans
  `keystore.properties`) — sans lui, impossible de mettre à jour l'app sous la
  même identité.
- **Signing strict** : le release utilise le keystore via `keystore.properties`.
  Si le fichier est absent, le build release échoue avec un message clair
  (plutôt que de signer silencieusement avec une mauvaise clé).
- **Mot de passe du keystore** : généré fort aléatoirement, stocké dans
  `keystore.properties` (gitignoré) et communiqué à l'utilisateur pour backup.
- **R8** : le smoke test de l'APK release signé sur l'émulateur valide que la
  minification ne casse rien ; si un crash survient, ajouter les règles
  proguard nécessaires (hors périmètre initial).

## Hors périmètre

- Modifier la config Supabase (action utilisateur dans le dashboard).
- Play Store / AAB : distribution hors Play Store (APK direct) conservée.
- Nouveaux tests automatisés.

## Vérification

- `export JAVA_HOME=...` + `./gradlew assembleRelease` → `app-release.apk` signé.
- `apksigner verify --verbose app-release.apk` → signature OK.
- Installer sur l'émulateur + smoke test (lancement, écran d'auth).
- `cd landing && npm run update-apk ../app/build/outputs/apk/release/app-release.apk`
  puis `npm run build`.
- Compilation des 2 fixes de bugs (assembleDebug passe).
