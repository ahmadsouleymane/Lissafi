# Onboarding Android — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Afficher un onboarding de 4 écrans glissables à la première ouverture de l'app, avant l'écran d'inscription.

**Architecture:** Nouvelle route `ONBOARDING` dans `LissafiNavHost`. `startDestination` choisit CAISSE / ONBOARDING / AUTH selon l'état de connexion et le flag local `onboarding_seen` (SharedPreferences). L'écran utilise `HorizontalPager` (Compose Foundation) avec 4 slides, indicateurs à points et boutons Passer/Continuer/Commencer.

**Tech Stack:** Kotlin 2.2.10, Jetpack Compose (BOM 2024.09.00), Material 3, Navigation Compose, icônes Lucide (`com.composables.icons.lucide.Lucide`).

## Global Constraints

- **Compilation :** `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` puis `./gradlew assembleDebug`. Aucun JDK système — le build échoue sans ce JAVA_HOME (JBR d'Android Studio, JDK 25). Premier build ~8 min, puis incrémental.
- **Aucun test** n'existe dans `app/src` — la vérification de chaque tâche = build qui passe + liste de test manuel.
- **Langue :** commentaires et messages de commit en **français**, commits en conventional style (`feat(onboarding): …`).
- **Jamais** de mention d'IA/Claude dans les commits.
- **Style UI :** réutiliser le thème existant (`Primary`, `PrimaryContainer`, `Surface`, `TextSecondary`, `Border`, `Background`), les icônes `LissafiIcons` et le style flottant (ombres douces, coins arrondis).
- **minSdk 24 / targetSdk 36.**

---

### Task 1: OnboardingManager (mémorisation de la 1re ouverture)

**Files:**
- Create: `app/src/main/java/com/lissafi/app/data/OnboardingManager.kt`

**Interfaces:**
- Produces: `object OnboardingManager` avec `isCompleted(context: Context): Boolean` et `markCompleted(context: Context)` — utilisé par Task 3.

- [ ] **Step 1: Créer le fichier**

Crée `app/src/main/java/com/lissafi/app/data/OnboardingManager.kt` :

```kotlin
package com.lissafi.app.data

import android.content.Context

/**
 * Mémorise si l'onboarding a déjà été affiché sur ce téléphone.
 *
 * Flag LOCAL au téléphone, jamais synchronisé : chaque nouvel appareil
 * d'un même compte reverra la présentation (comportement souhaité).
 */
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

- [ ] **Step 2: Compiler**

Run:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL (pas d'erreur).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lissafi/app/data/OnboardingManager.kt
git commit -m "feat(onboarding): mémorise la 1re ouverture (OnboardingManager)"
```

---

### Task 2: Écran d'onboarding (HorizontalPager 4 slides)

**Files:**
- Create: `app/src/main/java/com/lissafi/app/ui/screen/OnboardingScreen.kt`

**Interfaces:**
- Consumes: rien de Task 1 (le flag est écrit par Task 3, via le callback `onFinish`).
- Produces: `@Composable fun OnboardingScreen(onFinish: () -> Unit)` — appelé par Task 3.
- Références existantes vérifiées : `LissafiIcons.Caisse` (Lucide.Store), `LissafiIcons.Clients` (Lucide.Users), `LissafiIcons.Sync` (Lucide.Cloud), `LissafiIcons.Info` (Lucide.Info), drawable `R.drawable.logo_auth`, couleurs `Primary`/`PrimaryContainer`/`TextSecondary`/`Border`/`Background`, typo `MaterialTheme.typography`.

- [ ] **Step 1: Créer le fichier**

Crée `app/src/main/java/com/lissafi/app/ui/screen/OnboardingScreen.kt` :

```kotlin
package com.lissafi.app.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lissafi.app.R
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.Border
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.PrimaryContainer
import com.lissafi.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private data class OnboardingSlide(
    val title: String,
    val description: String,
    val icon: ImageVector? = null,
    val imageRes: Int? = null
)

private val OnboardingSlides = listOf(
    OnboardingSlide(
        title = "Ta boutique dans ta poche",
        description = "Vends, encaisse et suis tes clients, tout simplement.",
        imageRes = R.drawable.logo_auth
    ),
    OnboardingSlide(
        title = "Encaisser vite",
        description = "Panier en 2 touches, paiement et ticket imprimé ou envoyé par WhatsApp.",
        icon = LissafiIcons.Caisse
    ),
    OnboardingSlide(
        title = "Clients & crédits",
        description = "Vente à crédit, suivi des dettes, chaque client dans ta liste.",
        icon = LissafiIcons.Clients
    ),
    OnboardingSlide(
        title = "Tes données en sécurité",
        description = "Lissafi marche même hors-ligne, et tes données sont sauvegardées dans le cloud.",
        icon = LissafiIcons.Sync
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { OnboardingSlides.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── PASSER (haut droite) ──
        TextButton(
            onClick = onFinish,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 16.dp)
        ) {
            Text("Passer", color = TextSecondary)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingSlideView(OnboardingSlides[page])
            }

            // ── INDICATEURS ──
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(OnboardingSlides.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(width = if (selected) 22.dp else 8.dp, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (selected) Primary else Border)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── BOUTON PRINCIPAL ──
            Button(
                onClick = {
                    if (pagerState.currentPage < OnboardingSlides.lastIndex) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(52.dp)
            ) {
                Text(
                    if (pagerState.currentPage == OnboardingSlides.lastIndex) "Commencer" else "Continuer"
                )
            }
        }
    }
}

@Composable
private fun OnboardingSlideView(slide: OnboardingSlide) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (slide.imageRes != null) {
            // Slide 1 : logo pleine largeur (comme sur l'écran d'auth)
            Image(
                painter = painterResource(slide.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
            )
        } else {
            // Slides 2-4 : icône dans une pastille ronde flottante
            Surface(
                modifier = Modifier.size(140.dp),
                shape = CircleShape,
                color = PrimaryContainer,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = slide.icon ?: LissafiIcons.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Primary
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = slide.title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = slide.description,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
```

- [ ] **Step 2: Compiler**

Run:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/screen/OnboardingScreen.kt
git commit -m "feat(onboarding): écran d'accueil 4 slides (HorizontalPager)"
```

---

### Task 3: Câbler l'onboarding dans la navigation

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/navigation/LissafiNavHost.kt`

**Interfaces:**
- Consumes: `OnboardingManager` (Task 1), `OnboardingScreen(onFinish: () -> Unit)` (Task 2).
- Produces: route `Routes.ONBOARDING`, `startDestination` conditionnel, navigation fin d'onboarding → AUTH.

- [ ] **Step 1: Ajouter la route et les imports**

Dans `app/src/main/java/com/lissafi/app/ui/navigation/LissafiNavHost.kt` :

Ajouter l'import en haut (après les imports `ui.screen.*`) :
```kotlin
import com.lissafi.app.data.OnboardingManager
```

Dans `object Routes`, ajouter la ligne :
```kotlin
object Routes {
    const val AUTH          = "auth"
    const val ONBOARDING    = "onboarding"
    const val CAISSE        = "caisse"
```

- [ ] **Step 2: startDestination conditionnel**

Dans `LissafiNavHost`, remplacer la ligne :
```kotlin
startDestination = if (isLoggedIn) Routes.CAISSE else Routes.AUTH,
```
par :
```kotlin
startDestination = when {
    isLoggedIn -> Routes.CAISSE
    !OnboardingManager.isCompleted(context) -> Routes.ONBOARDING
    else -> Routes.AUTH
},
```

- [ ] **Step 3: Corriger le LaunchedEffect**

Remplacer le bloc `LaunchedEffect(isLoggedIn) { ... }` par :
```kotlin
LaunchedEffect(isLoggedIn) {
    if (isLoggedIn) {
        app.syncManager.syncInBackground()
        navController.navigate(Routes.CAISSE) {
            popUpTo(Routes.AUTH) { inclusive = true }
        }
    } else if (OnboardingManager.isCompleted(context)) {
        // Déjà passé par l'onboarding (ou déconnecté) → inscription
        navController.navigate(Routes.AUTH) {
            popUpTo(0) { inclusive = true }
        }
    }
    // Sinon : on reste sur ONBOARDING, ne rien faire (évite d'écraser l'onboarding)
}
```

- [ ] **Step 4: Ajouter la route ONBOARDING au NavHost**

Dans le `NavHost`, avant `composable(Routes.AUTH)`, ajouter :
```kotlin
composable(Routes.ONBOARDING) {
    OnboardingScreen(
        onFinish = {
            OnboardingManager.markCompleted(context)
            navController.navigate(Routes.AUTH) {
                popUpTo(Routes.ONBOARDING) { inclusive = true }
            }
        }
    )
}
```

- [ ] **Step 5: Compiler**

Run:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL (APK généré). Si `./gradlew assembleDebug` échoue pour une raison réseau/ressource, au minimum `:app:compileDebugKotlin` doit passer.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/navigation/LissafiNavHost.kt
git commit -m "feat(onboarding): route + startDestination dans le NavHost"
```

---

### Task 4: Vérification manuelle sur appareil

**Files:** aucun (vérification).

- [ ] **Step 1: Installer l'APK**

Run:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew installDebug
```
(Si plusieurs appareils connectés, spécifier le device avec `ANDROID_SERIAL`.)

- [ ] **Step 2: Scénario 1 — Nouvel utilisateur**

1. Désinstaller Lissafi (données téléphone vierges) puis installer.
2. Ouvrir → **l'onboarding 4 écrans doit s'afficher** (logo, puis caisse, clients, cloud).
3. Glisser entre les écrans → les points d'indicateurs suivent.
4. Taper « Passer » → écran d'inscription.
5. Fermer l'app, la rouvrir → **l'inscription s'affiche directement, pas d'onboarding**.

- [ ] **Step 3: Scénario 2 — Déjà connecté**

1. Se connecter avec un compte existant.
2. Fermer l'app, la rouvrir → **la caisse s'affiche directement** (pas d'onboarding).

- [ ] **Step 4: Scénario 3 — Fin de parcours**

1. Après avoir désinstallé/réinstallé (ou effacé les données), ouvrir l'app.
2. Glisser jusqu'au dernier écran → le bouton affiche « Commencer ».
3. Taper « Commencer » → écran d'inscription.

---

## Self-Review (à exécuter après rédaction)

- [ ] Spec couverte : comportement (4 écrans, Passer, une seule fois, ignoré si connecté), fichiers, cas particuliers (retour, rotation, flag local).
- [ ] Aucun placeholder : chaque étape contient le code complet.
- [ ] Types cohérents : `OnboardingManager.isCompleted/markCompleted(context)`, `OnboardingScreen(onFinish)`, `Routes.ONBOARDING` cohérents entre tâches.
