# Amélioration UX Réglages + Auth + Onboarding — Phase 3 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ajouter les micro-interactions finales (haptique, transitions animées) et l'insight de progression de la limite gratuite aux écrans Réglages, Auth et Onboarding.

**Architecture:** Ajouts locaux dans 4 fichiers (1 ViewModel + 3 écrans). Une seule modification de ViewModel (`SettingsViewModel`) pour exposer les compteurs produits/clients (les méthodes `getProductCount()`/`getClientCount()` du repository existent déjà). Aucune nouvelle dépendance ; `AnimatedContent` et `LocalHapticFeedback` sont des primitives Compose déjà utilisées dans le projet.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, aucun changement de dépendance.

## Global Constraints

- Aucun test unitaire dans le repo (`app/src`) — la vérification de chaque tâche se fait par compilation Kotlin (`./gradlew :app:compileDebugKotlin`), puis un `assembleDebug` final.
- `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` est requis avant toute commande Gradle.
- L'argent reste en `Int` FCFA (pas de montant concerné dans cette phase).
- Zéro nouvelle dépendance, zéro modification du backend (Supabase/AuthManager), du repository, de la DB, du thème ou de `LissafiComponents.kt`. Les seuils de limite viennent de `PremiumManager.MAX_FREE_PRODUCTS`/`MAX_FREE_CREDITS` (10/10) — les référencer, ne pas les hardcoder.
- Commits en français, conventional commits (`feat(...)`), jamais de mention d'IA/Claude.
- L'exécution se fait sur une branche dédiée (pas `main`), avec un build baseline propre.

---

## Task 1: `SettingsViewModel` — exposer les compteurs de limite

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/viewmodel/SettingsViewModel.kt`

**Interfaces:**
- Produces: `SettingsState.clientCount: Int` (nouveau) et `SettingsState.productCount: Int` (existant, désormais peuplé) — consommés par Task 2 (`SettingsScreen`).

- [ ] **Step 1: Ajouter `clientCount` au `SettingsState`**

Dans `app/src/main/java/com/lissafi/app/ui/viewmodel/SettingsViewModel.kt`, remplacer :

```kotlin
data class SettingsState(
    val shopName: String = "",
    val shopPhone: String = "",
    val adminPin: String = "0000",
    val isPremium: Boolean = false,
    val premiumExpiry: Long? = null,
    val premiumExpiryText: String = "",
    val productCount: Int = 0,
    val isSaving: Boolean = false
)
```

par :

```kotlin
data class SettingsState(
    val shopName: String = "",
    val shopPhone: String = "",
    val adminPin: String = "0000",
    val isPremium: Boolean = false,
    val premiumExpiry: Long? = null,
    val premiumExpiryText: String = "",
    val productCount: Int = 0,
    val clientCount: Int = 0,
    val isSaving: Boolean = false
)
```

- [ ] **Step 2: Peupler `productCount` et `clientCount` dans `loadSettings`**

Remplacer :

```kotlin
            val premium = repository.isPremium()
            val expiry = repository.getPremiumExpiry()

            _state.value = _state.value.copy(
                shopName = shopName,
                shopPhone = shopPhone,
                adminPin = adminPin,
                isPremium = premium,
                premiumExpiry = expiry,
                premiumExpiryText = if (expiry != null) FormatUtils.formatDate(expiry) else ""
            )
```

par :

```kotlin
            val premium = repository.isPremium()
            val expiry = repository.getPremiumExpiry()
            val productCount = repository.getProductCount()
            val clientCount = repository.getClientCount()

            _state.value = _state.value.copy(
                shopName = shopName,
                shopPhone = shopPhone,
                adminPin = adminPin,
                isPremium = premium,
                premiumExpiry = expiry,
                premiumExpiryText = if (expiry != null) FormatUtils.formatDate(expiry) else "",
                productCount = productCount,
                clientCount = clientCount
            )
```

- [ ] **Step 3: Compiler**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/viewmodel/SettingsViewModel.kt
git commit -m "feat(reglages): compteurs de limite dans le SettingsViewModel"
```

---

## Task 2: `SettingsScreen` — progression de limite + haptique

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/SettingsScreen.kt`

**Interfaces:**
- Consumes: `SettingsState.productCount`/`SettingsState.clientCount` (Task 1), `PremiumManager.MAX_FREE_PRODUCTS`/`MAX_FREE_CREDITS`.
- Produces: rien pour les autres tâches.

- [ ] **Step 1: Ajouter les imports**

Dans `app/src/main/java/com/lissafi/app/ui/screen/SettingsScreen.kt`, ajouter après `import androidx.compose.ui.platform.LocalContext` (ligne 15) :

```kotlin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
```

et après `import com.lissafi.app.service.FormatUtils` (ligne 23) :

```kotlin
import com.lissafi.app.service.PremiumManager
```

- [ ] **Step 2: Déclarer `haptic` dans `SettingsScreen`**

Remplacer :

```kotlin
    val state by viewModel.state.collectAsState()
    var showShopDialog by remember { mutableStateOf(false) }
```

par :

```kotlin
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    var showShopDialog by remember { mutableStateOf(false) }
```

- [ ] **Step 3: Progression de limite dans la bannière gratuite**

Dans la bannière Premium, remplacer le bloc du `Column` interne (lignes 106-121) :

```kotlin
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.isPremium) "Premium actif" else "Version gratuite",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = OnBackground
                        )
                        Text(
                            text = if (state.isPremium && state.premiumExpiry != null)
                                "Expire le ${FormatUtils.formatDate(state.premiumExpiry!!)}"
                            else "Limitée à 10 produits et 10 clients",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
```

par :

```kotlin
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.isPremium) "Premium actif" else "Version gratuite",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = OnBackground
                        )
                        Text(
                            text = if (state.isPremium && state.premiumExpiry != null)
                                "Expire le ${FormatUtils.formatDate(state.premiumExpiry!!)}"
                            else "Limitée à 10 produits et 10 clients",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                        if (!state.isPremium) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FreeLimitBar(
                                    label = "produits",
                                    count = state.productCount,
                                    max = PremiumManager.MAX_FREE_PRODUCTS,
                                    modifier = Modifier.weight(1f)
                                )
                                FreeLimitBar(
                                    label = "clients",
                                    count = state.clientCount,
                                    max = PremiumManager.MAX_FREE_CREDITS,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
```

- [ ] **Step 4: Ajouter le composant `FreeLimitBar`**

Ajouter à la fin du fichier (après la fermeture de `ShopInfoDialog`, ligne 530) :

```kotlin
// ============================================================
// BARRE DE PROGRESSION DE LIMITE — Usage de la version gratuite
// ============================================================
@Composable
private fun FreeLimitBar(label: String, count: Int, max: Int, modifier: Modifier = Modifier) {
    val fraction = (count.toFloat() / max).coerceIn(0f, 1f)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$count/$max",
                fontSize = 10.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Secondary.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (count >= max) Error else Secondary)
            )
        }
    }
}
```

- [ ] **Step 5: Haptique sur l'enregistrement boutique**

Dans le bloc `if (showShopDialog) { ShopInfoDialog(...) }`, remplacer le `onSave` :

```kotlin
            onSave = { name, phone ->
                viewModel.saveShopInfo(name, phone)
                showShopDialog = false
            }
```

par :

```kotlin
            onSave = { name, phone ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.saveShopInfo(name, phone)
                showShopDialog = false
            }
```

- [ ] **Step 6: Haptique sur l'envoi de signalement**

Dans le bloc `if (showReportDialog) { ReportIssueDialog(...) }`, remplacer le `onSend` :

```kotlin
            onSend = { subject, message ->
                showReportDialog = false
                app.supabaseApi.reportSupportTicket(subject, message)
                Toast.makeText(context, "Message envoyé ! Merci.", Toast.LENGTH_LONG).show()
            }
```

par :

```kotlin
            onSend = { subject, message ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showReportDialog = false
                app.supabaseApi.reportSupportTicket(subject, message)
                Toast.makeText(context, "Message envoyé ! Merci.", Toast.LENGTH_LONG).show()
            }
```

- [ ] **Step 7: Haptique sur la déconnexion**

Dans le bloc COMPTE, remplacer le `TextButton` de déconnexion :

```kotlin
                    TextButton(onClick = { onSignOut() }) {
```

par :

```kotlin
                    TextButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSignOut()
                    }) {
```

- [ ] **Step 8: Compiler**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/screen/SettingsScreen.kt
git commit -m "feat(reglages): progression de la limite gratuite et haptique"
```

---

## Task 3: `AuthScreen` — transitions animées + haptique

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/AuthScreen.kt`

**Interfaces:**
- Consumes: rien de neuf.
- Produces: rien pour les autres tâches.

- [ ] **Step 1: Ajouter les imports animation + haptique**

Dans `app/src/main/java/com/lissafi/app/ui/screen/AuthScreen.kt`, ajouter en tête d'imports (après `import androidx.compose.foundation.BorderStroke`, ligne 3) :

```kotlin
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
```

et après `import androidx.compose.ui.draw.clip` (ligne 43) :

```kotlin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
```

- [ ] **Step 2: Déclarer `haptic` dans `AuthScreen`**

Remplacer :

```kotlin
fun AuthScreen(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
```

par :

```kotlin
fun AuthScreen(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    var passwordVisible by remember { mutableStateOf(false) }
```

- [ ] **Step 3: Haptique sur la bascule d'onglets**

Dans le `SegmentedControl`, remplacer le `onSelect` :

```kotlin
                        onSelect = { index ->
                            viewModel.setMode(if (index == 0) AuthMode.SIGN_IN else AuthMode.SIGN_UP)
                        }
```

par :

```kotlin
                        onSelect = { index ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setMode(if (index == 0) AuthMode.SIGN_IN else AuthMode.SIGN_UP)
                        }
```

- [ ] **Step 4: Envelopper le `when (state.mode)` dans un `AnimatedContent`**

Remplacer :

```kotlin
                    when (state.mode) {
                        AuthMode.SIGN_IN -> SignInForm(
                            state = state,
                            passwordVisible = passwordVisible,
                            onTogglePassword = { passwordVisible = !passwordVisible },
                            onEmailChange = { viewModel.setEmail(it) },
                            onPasswordChange = { viewModel.setPassword(it) },
                            onSubmit = { viewModel.signIn() },
                            onSwitchToReset = { viewModel.setMode(AuthMode.RESET_PASSWORD) }
                        )
                        AuthMode.SIGN_UP -> SignUpForm(
                            state = state,
                            passwordVisible = passwordVisible,
                            onTogglePassword = { passwordVisible = !passwordVisible },
                            onEmailChange = { viewModel.setEmail(it) },
                            onPasswordChange = { viewModel.setPassword(it) },
                            onConfirmPasswordChange = { viewModel.setConfirmPassword(it) },
                            onShopNameChange = { viewModel.setShopName(it) },
                            onSubmit = { viewModel.signUp() }
                        )
                        AuthMode.RESET_PASSWORD -> ResetPasswordForm(
                            state = state,
                            onEmailChange = { viewModel.setEmail(it) },
                            onSubmit = { viewModel.resetPassword() },
                            onSwitchToSignIn = { viewModel.setMode(AuthMode.SIGN_IN) }
                        )
                    }
```

par :

```kotlin
                    AnimatedContent(
                        targetState = state.mode,
                        transitionSpec = {
                            (fadeIn() + slideInVertically { -it / 8 }) togetherWith
                                (fadeOut() + slideOutVertically { it / 8 })
                        },
                        label = "authMode"
                    ) { mode ->
                        when (mode) {
                            AuthMode.SIGN_IN -> SignInForm(
                                state = state,
                                passwordVisible = passwordVisible,
                                onTogglePassword = { passwordVisible = !passwordVisible },
                                onEmailChange = { viewModel.setEmail(it) },
                                onPasswordChange = { viewModel.setPassword(it) },
                                onSubmit = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.signIn()
                                },
                                onSwitchToReset = { viewModel.setMode(AuthMode.RESET_PASSWORD) }
                            )
                            AuthMode.SIGN_UP -> SignUpForm(
                                state = state,
                                passwordVisible = passwordVisible,
                                onTogglePassword = { passwordVisible = !passwordVisible },
                                onEmailChange = { viewModel.setEmail(it) },
                                onPasswordChange = { viewModel.setPassword(it) },
                                onConfirmPasswordChange = { viewModel.setConfirmPassword(it) },
                                onShopNameChange = { viewModel.setShopName(it) },
                                onSubmit = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.signUp()
                                }
                            )
                            AuthMode.RESET_PASSWORD -> ResetPasswordForm(
                                state = state,
                                onEmailChange = { viewModel.setEmail(it) },
                                onSubmit = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.resetPassword()
                                },
                                onSwitchToSignIn = { viewModel.setMode(AuthMode.SIGN_IN) }
                            )
                        }
                    }
```

Note : les trois haptiques de soumission sont intégrés ici (au clic, assumé par la spec — ils vibrent même si le serveur répond en erreur, le feedback d'erreur visuel reste la source de vérité).

- [ ] **Step 5: Compiler**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/screen/AuthScreen.kt
git commit -m "feat(auth): transitions animées entre les formulaires et haptique"
```

---

## Task 4: `OnboardingScreen` — libellé animé + haptique

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/OnboardingScreen.kt`

**Interfaces:**
- Consumes: rien de neuf.
- Produces: rien pour les autres tâches.

- [ ] **Step 1: Ajouter les imports**

Dans `app/src/main/java/com/lissafi/app/ui/screen/OnboardingScreen.kt`, ajouter après `import androidx.compose.foundation.Image` (ligne 3) :

```kotlin
import androidx.compose.animation.AnimatedContent
```

et après `import androidx.compose.ui.res.painterResource` (ligne 16) :

```kotlin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
```

- [ ] **Step 2: Déclarer `haptic` dans `OnboardingScreen`**

Remplacer :

```kotlin
fun OnboardingScreen(onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { OnboardingSlides.size })
```

par :

```kotlin
fun OnboardingScreen(onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val pagerState = rememberPagerState(pageCount = { OnboardingSlides.size })
```

- [ ] **Step 3: Haptique sur `Passer`**

Remplacer :

```kotlin
        TextButton(
            onClick = onFinish,
```

par :

```kotlin
        TextButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onFinish()
            },
```

- [ ] **Step 4: Haptique + libellé animé sur le bouton principal**

Remplacer le bloc du `Button` principal (lignes 112-128) :

```kotlin
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
```

par :

```kotlin
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                AnimatedContent(
                    targetState = pagerState.currentPage == OnboardingSlides.lastIndex,
                    label = "onboardingButton"
                ) { isLast ->
                    Text(if (isLast) "Commencer" else "Continuer")
                }
            }
```

- [ ] **Step 5: Compiler**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/screen/OnboardingScreen.kt
git commit -m "feat(onboarding): libellé animé et haptique"
```

---

## Task 5: Build complet et vérification

**Files:** Aucun changement de code — vérification finale uniquement.

- [ ] **Step 1: Build debug complet**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Installer sur un appareil/émulateur connecté (si disponible)**

Run: `./gradlew installDebug`
Expected: `BUILD SUCCESSFUL`, l'app s'installe sans erreur.

- [ ] **Step 3: Vérification manuelle (limitée par l'environnement)**

L'écran Auth complet n'est testable de bout en bout qu'avec un compte valide (auth Supabase non configurée dans cet environnement). Vérifications à faire par l'utilisateur :
- Onboarding (1re ouverture, hors connexion) : libellé animé Continuer↔Commencer, haptique sur le bouton et Passer — testable sans compte.
- Réglages (après connexion) : bannière gratuite affiche `X/10 produits · Y/10 clients` + barres de progression, haptique sur les 3 actions.
- Auth (avec un compte valide) : transitions animées entre les 3 modes, haptique sur les onglets et la soumission.
