# Audit production — Plan d'implémentation (signature APK, 2 bugs, guide Supabase)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produire un APK release signé installable, corriger 2 bugs UX (double-nav Caisse, bouton « Passer » sous la barre de statut), et documenter le réglage Supabase Auth qui bloque l'inscription.

**Architecture:** Signature Android via `keytool` + `keystore.properties` (gitignoré) lu par `build.gradle.kts`. Les 2 bugs sont des corrections Compose ponctuelles. Le guide est un doc markdown.

**Tech Stack:** Gradle/Kotlin DSL, Jetpack Compose, Android SDK (JBR d'Android Studio).

## Global Constraints

- **Build :** `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` puis `./gradlew ...`. Aucun JDK système.
- **Sécurité keystore :** `app/lissafi-release.keystore` et `keystore.properties` sont **gitignorés** — JAMAIS commités. Le mot de passe généré est communiqué à l'utilisateur pour backup.
- **Langue :** commentaires, doc et messages de commit en **français**. Jamais de mention IA/Claude.
- **Tests :** aucun test unitaire — vérification = build + smoke test sur émulateur.
- **Ne pas commiter** le keystore ni `keystore.properties` ni `local.properties`.

---

### Task 1: Signature release (keystore + build.gradle.kts + APK signé testé)

**Files:**
- Create: `app/lissafi-release.keystore` (binaire, **gitignoré**)
- Create: `keystore.properties` (racine, **gitignoré**)
- Modify: `app/build.gradle.kts`
- Modify: `.gitignore` (racine)

**Interfaces:**
- Produces: `app/build/outputs/apk/release/app-release.apk` **signé** (utilisé par Task 3 pour la landing) ; `signingConfigs.release` dans build.gradle.kts.

- [ ] **Step 1: Ajouter les entrées au `.gitignore`**

Dans `.gitignore` (racine), après la ligne `local.properties` (ou en fin de fichier), ajouter :

```
*.keystore
keystore.properties
```

- [ ] **Step 2: Générer le keystore + `keystore.properties`**

Run (depuis `/Users/macbookair/Desktop/Lissafi`) :

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
KEYTOOL="$JAVA_HOME/bin/keytool"
PASS=$(openssl rand -base64 18 | tr -dc 'A-Za-z0-9' | head -c 24)
mkdir -p app
"$KEYTOOL" -genkeypair -v \
  -keystore app/lissafi-release.keystore \
  -alias lissafi \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$PASS" -keypass "$PASS" \
  -dname "CN=Lissafi, OU=Lissafi, O=Lissafi, L=Niamey, ST=Niger, C=NE"
cat > keystore.properties <<EOF
storeFile=app/lissafi-release.keystore
storePassword=$PASS
keyAlias=lissafi
keyPassword=$PASS
EOF
echo "Keystore généré. Password (à sauvegarder !) : $PASS"
```

Attendu : le keystore est créé et `keystore.properties` contient le mot de passe. **Note le mot de passe** pour le communiquer à l'utilisateur.

- [ ] **Step 3: Modifier `app/build.gradle.kts`**

En tout premier du fichier (avant `plugins {`), ajouter l'import :

```kotlin
import java.util.Properties
```

Dans le bloc `android {`, **avant** `buildTypes {`, ajouter le bloc `signingConfigs` :

```kotlin
    signingConfigs {
        create("release") {
            // Signature release depuis keystore.properties (jamais commité).
            // Si le fichier est absent, le build release échoue volontairement.
            val props = Properties()
            val propsFile = rootProject.file("keystore.properties")
            if (propsFile.exists()) props.load(propsFile.inputStream())
            storeFile = rootProject.file(props.getProperty("storeFile", "app/lissafi-release.keystore"))
            storePassword = props.getProperty("storePassword", "")
            keyAlias = props.getProperty("keyAlias", "lissafi")
            keyPassword = props.getProperty("keyPassword", "")
        }
    }
```

Dans `buildTypes { release { ... } }`, ajouter la ligne `signingConfig` (après `proguardFiles(...)` et la fermeture de `proguardFiles`) :

```kotlin
            signingConfig = signingConfigs.getByName("release")
```

Résultat attendu du bloc `buildTypes` :

```kotlin
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
```

- [ ] **Step 4: Build le release signé**

Run:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleRelease
```
Expected: BUILD SUCCESSFUL → `app/build/outputs/apk/release/app-release.apk`.

- [ ] **Step 5: Vérifier la signature**

Run:
```bash
"$JAVA_HOME/build-tools/36.0.0/apksigner" verify --verbose app/build/outputs/apk/release/app-release.apk 2>/dev/null \
  || "$HOME/Library/Android/sdk/build-tools/$(ls "$HOME/Library/Android/sdk/build-tools" | tail -1)/apksigner" verify --verbose app/build/outputs/apk/release/app-release.apk
```
Expected: `Verifies` + `Verified using v1 scheme` et/ou `v2 scheme`.

- [ ] **Step 6: Smoke test sur l'émulateur**

Run:
```bash
export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"
adb devices
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.lissafi.app/.MainActivity
sleep 4
adb exec-out screencap -p > /tmp/release_smoke.png
```
Vérifie (capture) : l'app démarre sans crash → l'écran d'auth (ou l'onboarding à la 1re install) s'affiche. Si l'app crashe (R8 a cassé quelque chose), note-le en concern — il faudra des règles proguard supplémentaires.

- [ ] **Step 7: Commit (sans le keystore ni keystore.properties)**

```bash
cd /Users/macbookair/Desktop/Lissafi
git add app/build.gradle.kts .gitignore
git commit -m "feat(build): signature release via keystore.properties (jamais commité)"
```
⚠️ Ne PAS commiter `app/lissafi-release.keystore`, `keystore.properties`, `local.properties`.

---

### Task 2: Corriger les 2 bugs (double-nav Caisse + bouton « Passer »)

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/navigation/LissafiNavHost.kt`
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/OnboardingScreen.kt`

**Interfaces:**
- Consumes: rien de Task 1.
- Produces: navigation Caisse sans doublon ; onboarding avec `statusBarsPadding`.

- [ ] **Step 1: Bug A — `launchSingleTop` sur la navigation Caisse**

Dans `LissafiNavHost.kt`, `LaunchedEffect(isLoggedIn)`, branche `if (isLoggedIn)` :

```kotlin
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            app.syncManager.syncInBackground()
            navController.navigate(Routes.CAISSE) {
                popUpTo(Routes.AUTH) { inclusive = true }
            }
        } else if (OnboardingManager.isCompleted(context)) {
```

Ajouter `launchSingleTop = true` dans le `navigate(Routes.CAISSE)` :

```kotlin
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            app.syncManager.syncInBackground()
            navController.navigate(Routes.CAISSE) {
                launchSingleTop = true
                popUpTo(Routes.AUTH) { inclusive = true }
            }
        } else if (OnboardingManager.isCompleted(context)) {
```

- [ ] **Step 2: Bug B — `statusBarsPadding()` sur l'onboarding**

Dans `OnboardingScreen.kt`, le `Box` racine :

```kotlin
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
```

Ajouter `.statusBarsPadding()` après `.background(Background)` :

```kotlin
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
```

Vérifier l'import `androidx.compose.foundation.layout.statusBarsPadding` (le fichier importe déjà `androidx.compose.foundation.layout.*` → couvert).

- [ ] **Step 3: Build debug (vérifie la compilation)**

Run:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
cd /Users/macbookair/Desktop/Lissafi
git add app/src/main/java/com/lissafi/app/ui/navigation/LissafiNavHost.kt app/src/main/java/com/lissafi/app/ui/screen/OnboardingScreen.kt
git commit -m "fix(ui): plus de doublon Caisse au démarrage connecté, bouton Passer hors barre de statut"
```

---

### Task 3: Guide Supabase + mise à jour de l'APK de la landing

**Files:**
- Create: `docs/lancement-supabase-auth.md`
- Update: `landing/public/lissafi.apk` (via script, non commité manuellement)

**Interfaces:**
- Consumes: `app/build/outputs/apk/release/app-release.apk` (Task 1) ; `npm run update-apk` (landing).

- [ ] **Step 1: Créer le guide**

Crée `docs/lancement-supabase-auth.md` :

```markdown
# Lancement — Activer l'inscription dans Supabase Auth

Le constat : l'app renvoie « Erreur d'inscription. Vérifie tes informations. »
quand un nouveau compte est créé. C'est la configuration Auth de Supabase qui
bloque — rien à changer dans le code Android.

## Étapes (dashboard Supabase)

1. Connecte-toi sur https://supabase.com/dashboard et ouvre le projet
   `fnyuhpfzkvunscuylvqv` (Lissafi).
2. Menu de gauche → **Authentication** → **Providers**.
3. Dans la ligne **Email**, clique sur le crayon (éditer).
4. Coche **« Enable Signups »** (autoriser les inscriptions).
5. Décoche **« Confirm email »** si tu veux que le compte soit actif
   immédiatement (sans lien de confirmation dans la boîte mail). Sinon, laisse
   coché et l'utilisateur devra cliquer le lien reçu par email.
6. **Save**.

## Tester

1. Installe l'APK sur un téléphone.
2. Ouvre l'app → crée un compte (email + mot de passe 6+ caractères).
3. Normal : l'app passe à la caisse directement.

## Rappels sécurité (déjà en place, rien à faire)

- RLS active sur toutes les tables (`supabase-schema.sql`) : chaque utilisateur
  ne voit que ses données (`auth.uid() = user_id`).
- La clé `service_role` n'est **jamais** dans l'app Android (seulement dans le
  back-office, côté serveur).
- Le back-office vérifie l'admin via `getAdminSession()`.

## Optionnel : compte démo (vidéos TikTok)

Le bouton « Tester avec la démo » a été retiré de l'app. Si tu veux un compte
pré-rempli pour les vidéos, exécute `scripts/seed-demo.sql` dans le SQL Editor
(compte `demo@lissafi.app` / `demo123456`).
```

- [ ] **Step 2: Mettre à jour l'APK de la landing (release signé)**

Run:
```bash
cd /Users/macbookair/Desktop/Lissafi/landing
npm run update-apk ../app/build/outputs/apk/release/app-release.apk
```
Expected: `✅ APK copié vers .../landing/public/lissafi.apk (N MB)` — le fichier est remplacé par le release signé.

- [ ] **Step 3: Build landing (vérifie que l'APK est bien copié dans dist)**

Run: `cd landing && npm run build`
Expected: BUILD SUCCESSFUL ; `ls -la dist/lissafi.apk` existe.

- [ ] **Step 4: Commit**

```bash
cd /Users/macbookair/Desktop/Lissafi
git add docs/lancement-supabase-auth.md landing/public/lissafi.apk
git commit -m "docs(auth): guide d'activation des inscriptions Supabase + APK release signé sur la landing"
```

---

## Self-Review (à exécuter après rédaction)

- [ ] Spec couverte : signature release (keystore + keystore.properties + build.gradle + test émulateur + landing), Bug A (launchSingleTop), Bug B (statusBarsPadding), guide Supabase.
- [ ] Aucun placeholder : chaque étape contient le code/commande exacts.
- [ ] Cohérence : `keystore.properties` (chemin `app/lissafi-release.keystore`) ↔ `signingConfigs` (`rootProject.file`) ; release APK (Task 1) ↔ update-apk landing (Task 3) ; `.gitignore` couvre keystore + properties.
