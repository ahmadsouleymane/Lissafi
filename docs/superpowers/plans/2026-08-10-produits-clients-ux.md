# Amélioration UX Produits + Clients — Phase 2 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enrichir les écrans Produits, Clients et Détail Client de Lissafi avec des insights (compteurs de filtres, marge unitaire, total à recouvrer, total remboursé) et des micro-interactions (animateItem, haptique) — sans nouvelle requête DB ni dépendance.

**Architecture:** Ajouts purement locaux dans 3 fichiers d'écran. Tous les chiffres sont calculés depuis l'état déjà en mémoire (`state.products`, `state.clients`, `transactions`) — aucune modification des ViewModels, du repository, de la DB ou du thème. Les patterns (haptique via `LocalHapticFeedback`, `Modifier.animateItem()` avec clé stable, `StatMiniCard`) viennent de la Phase 1.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, aucun changement de dépendance.

## Global Constraints

- Aucun test unitaire dans le repo (`app/src`) — la vérification de chaque tâche se fait par compilation Kotlin (`./gradlew :app:compileDebugKotlin`), puis un `assembleDebug` final.
- `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` est requis avant toute commande Gradle.
- L'argent reste en `Int` FCFA partout — jamais de `Double` pour un montant.
- Zéro nouvelle requête DB, zéro nouvelle dépendance, aucune modification de `LissafiDatabase.kt`, `LissafiRepository.kt`, des ViewModels, du thème (`ui/theme/*`) ni de `LissafiComponents.kt`.
- Commits en français, conventional commits (`feat(...)`), jamais de mention d'IA/Claude.
- L'exécution se fait sur une branche dédiée (pas `main`), avec un build baseline propre vérifié avant de commencer.

---

## Task 1: Produits — compteurs sur les chips + haptique à l'ajout

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/ProductsScreen.kt`

**Interfaces:**
- Consumes: rien de neuf (haptique via `LocalHapticFeedback` — même pattern que la Phase 1).
- Produces: rien pour les autres tâches (modifications isolées dans ce fichier).

- [ ] **Step 1: Ajouter les imports haptique**

Dans `app/src/main/java/com/lissafi/app/ui/screen/ProductsScreen.kt`, ajouter après `import androidx.compose.ui.platform.LocalContext` (ligne 17) :

```kotlin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
```

- [ ] **Step 2: Déclarer `haptic` et les compteurs de filtres**

Dans le composable `ProductsScreen`, remplacer :

```kotlin
    val alertCount = state.products.count { isStockAlert(it) }
```

par :

```kotlin
    val haptic = LocalHapticFeedback.current
    val stockCount = state.products.count { it.stock > 0 }
    val alertCount = state.products.count { isStockAlert(it) }
```

- [ ] **Step 3: Ajouter un compteur au libellé de chaque chip**

Remplacer le bloc `Row { ProductFilter.values().forEach { f -> FilterChip(...) } }` (lignes 105-131) par :

```kotlin
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProductFilter.values().forEach { f ->
                val count = when (f) {
                    ProductFilter.TOUS -> state.products.size
                    ProductFilter.EN_STOCK -> stockCount
                    ProductFilter.ALERTE -> alertCount
                }
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = f.label,
                                fontSize = 12.sp,
                                fontWeight = if (filter == f) FontWeight.Medium else FontWeight.Normal
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "$count",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (filter == f) OnPrimary else TextSecondary
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = OnPrimary,
                        containerColor = SurfaceAlt,
                        labelColor = TextSecondary
                    )
                )
            }
        }
```

- [ ] **Step 4: Haptique à l'ajout réussi (les deux appels à `ProductFormDialog`)**

Remplacer les deux blocs `onSave = {` du composable `ProductsScreen` (celui de `newProductPrefill?.let` et celui de `editingProduct?.let`) pour insérer l'haptique avant `viewModel.addProduct` :

```kotlin
            onSave = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addProduct(it)
                newProductPrefill = null
            }
```

et

```kotlin
            onSave = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addProduct(it)
                editingProduct = null
            }
```

- [ ] **Step 5: Compiler**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/screen/ProductsScreen.kt
git commit -m "feat(produits): compteurs sur les filtres et haptique à l'ajout"
```

---

## Task 2: Produits — marge unitaire, haptique à la suppression, animation des cartes

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/ProductsScreen.kt`

**Interfaces:**
- Consumes: `haptic` déclaré en Task 1 (même fichier, même composable).
- Produces: rien pour les autres tâches.

- [ ] **Step 1: Marge unitaire dans `ProductCard`**

Dans la fonction privée `ProductCard`, remplacer le bloc de la colonne de droite (lignes 323-351) :

```kotlin
            Column(horizontalAlignment = Alignment.End) {
                AmountText(amount = product.sellPrice, fontSize = 18)
                if (product.buyPrice > 0) {
                    Text(
                        text = "Achat ${FormatUtils.formatFCFA(product.buyPrice)}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = LissafiIcons.Modifier,
                            contentDescription = "Modifier",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = LissafiIcons.Supprimer,
                            contentDescription = "Supprimer",
                            tint = Error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
```

par :

```kotlin
            Column(horizontalAlignment = Alignment.End) {
                AmountText(amount = product.sellPrice, fontSize = 18)
                if (product.buyPrice > 0) {
                    val margin = product.sellPrice - product.buyPrice
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Marge ${FormatUtils.formatFCFA(margin)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (margin >= 0) Success else Error
                    )
                    Text(
                        text = "Achat ${FormatUtils.formatFCFA(product.buyPrice)}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = LissafiIcons.Modifier,
                            contentDescription = "Modifier",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = LissafiIcons.Supprimer,
                            contentDescription = "Supprimer",
                            tint = Error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
```

- [ ] **Step 2: Haptique à la suppression confirmée**

Dans `ProductsScreen`, le callback `onDelete` passé à `ProductCard` (dans l'appel `items(filtered, key = { it.barcode })`), remplacer :

```kotlin
                        onDelete = {
                            viewModel.deleteProduct(product)
                            Toast.makeText(context, "${product.name} supprimé", Toast.LENGTH_SHORT).show()
                        }
```

par :

```kotlin
                        onDelete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.deleteProduct(product)
                            Toast.makeText(context, "${product.name} supprimé", Toast.LENGTH_SHORT).show()
                        }
```

- [ ] **Step 3: Ajouter un paramètre `modifier` à `ProductCard`**

Remplacer la signature de `ProductCard` :

```kotlin
private fun ProductCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
```

par :

```kotlin
private fun ProductCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
```

puis dans le corps, remplacer la ligne d'ouverture du `LissafiCard` :

```kotlin
    LissafiCard(modifier = Modifier.padding(vertical = 4.dp), cornerRadius = 18, elevation = 2) {
```

par :

```kotlin
    LissafiCard(modifier = modifier.padding(vertical = 4.dp), cornerRadius = 18, elevation = 2) {
```

- [ ] **Step 4: Appliquer `Modifier.animateItem()` à chaque carte**

Remplacer l'appel `items(filtered, key = { it.barcode }) { product -> ProductCard(...) }` pour passer le modifier :

```kotlin
                items(filtered, key = { it.barcode }) { product ->
                    ProductCard(
                        product = product,
                        onEdit = { editingProduct = product },
                        onDelete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.deleteProduct(product)
                            Toast.makeText(context, "${product.name} supprimé", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.animateItem()
                    )
                }
```

- [ ] **Step 5: Compiler**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/screen/ProductsScreen.kt
git commit -m "feat(produits): marge unitaire, haptique suppression et animation des cartes"
```

---

## Task 3: Clients — compteurs sur les chips + carte "À recouvrer"

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/ClientsScreen.kt`

**Interfaces:**
- Consumes: rien de neuf.
- Produces: rien pour les autres tâches.

- [ ] **Step 1: Ajouter les imports**

Dans `app/src/main/java/com/lissafi/app/ui/screen/ClientsScreen.kt`, ajouter après `import androidx.compose.ui.platform.LocalContext` (n'existe pas dans ce fichier — ajouter après `import androidx.compose.ui.text.style.TextOverflow`, ligne 16) :

```kotlin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
```

et ajouter aux imports du thème, après `import com.lissafi.app.ui.theme.Secondary` (ligne 39) :

```kotlin
import com.lissafi.app.ui.theme.OnSecondaryContainer
import com.lissafi.app.ui.theme.SecondaryContainer
```

- [ ] **Step 2: Déclarer `haptic` et les compteurs**

Dans le composable `ClientsScreen`, remplacer :

```kotlin
    val recentCutoff = System.currentTimeMillis() - RECENT_WINDOW_DAYS * DAY_MS
```

par :

```kotlin
    val haptic = LocalHapticFeedback.current
    val recentCutoff = System.currentTimeMillis() - RECENT_WINDOW_DAYS * DAY_MS
    val debtCount = state.clients.count { it.totalDebt > 0 }
    val recentCount = state.clients.count { it.updatedAt >= recentCutoff }
```

- [ ] **Step 3: Ajouter un compteur au libellé de chaque chip**

Remplacer le bloc `Row { ClientFilter.values().forEach { f -> FilterChip(...) } }` (lignes 93-119) par :

```kotlin
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ClientFilter.values().forEach { f ->
                val count = when (f) {
                    ClientFilter.TOUS -> state.clients.size
                    ClientFilter.AVEC_DETTE -> debtCount
                    ClientFilter.RECENTS -> recentCount
                }
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = f.label,
                                fontSize = 12.sp,
                                fontWeight = if (filter == f) FontWeight.Medium else FontWeight.Normal
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "$count",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (filter == f) OnPrimary else TextSecondary
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = OnPrimary,
                        containerColor = SurfaceAlt,
                        labelColor = TextSecondary
                    )
                )
            }
        }
```

- [ ] **Step 4: Ajouter la carte "À recouvrer"**

Après le `Row` des chips (après sa fermeture, ligne 119) et avant le `if (filtered.isEmpty())` (ligne 121), insérer :

```kotlin
        val totalDettes = state.clients.sumOf { it.totalDebt }
        if (state.searchQuery.isBlank() && totalDettes > 0) {
            LissafiCard(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                cornerRadius = 18,
                elevation = 2,
                containerColor = SecondaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconCircle(
                        icon = LissafiIcons.Encaisser,
                        backgroundColor = Secondary.copy(alpha = 0.1f),
                        iconTint = Secondary,
                        size = 40,
                        iconSize = 20
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Total à recouvrer",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSecondaryContainer
                        )
                        AmountText(amount = totalDettes, fontSize = 20, color = Secondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
```

- [ ] **Step 5: Compiler**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/screen/ClientsScreen.kt
git commit -m "feat(clients): compteurs sur les filtres et total à recouvrer"
```

---

## Task 4: Clients — clé stable, animation des cartes, haptique à l'ajout

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/ClientsScreen.kt`

**Interfaces:**
- Consumes: `haptic` déclaré en Task 3 (même fichier).
- Produces: rien pour les autres tâches.

- [ ] **Step 1: Ajouter `key` et `animateItem` à la liste**

Remplacer le bloc `items(filtered) { client -> ... ClientCard(...) }` (lignes 143-150) par :

```kotlin
                items(filtered, key = { it.id }) { client ->
                    val daysSince = (System.currentTimeMillis() - client.updatedAt) / DAY_MS
                    ClientCard(
                        client = client,
                        daysSince = daysSince.toInt(),
                        onClick = { onClientClick(client.id) },
                        modifier = Modifier.animateItem()
                    )
                }
```

- [ ] **Step 2: Ajouter un paramètre `modifier` à `ClientCard`**

Remplacer la signature de `ClientCard` :

```kotlin
private fun ClientCard(
    client: Client,
    daysSince: Int,
    onClick: () -> Unit
) {
```

par :

```kotlin
private fun ClientCard(
    client: Client,
    daysSince: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
```

puis dans le corps, remplacer :

```kotlin
    LissafiCard(modifier = Modifier.padding(vertical = 4.dp), onClick = onClick, cornerRadius = 18, elevation = 2) {
```

par :

```kotlin
    LissafiCard(modifier = modifier.padding(vertical = 4.dp), onClick = onClick, cornerRadius = 18, elevation = 2) {
```

- [ ] **Step 3: Haptique à l'ajout client**

Dans le bloc `if (showAddDialog) { AddClientDialog(...) }`, remplacer le `onSave` :

```kotlin
            onSave = { name, phone ->
                viewModel.addClient(name, phone)
                showAddDialog = false
            }
```

par :

```kotlin
            onSave = { name, phone ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addClient(name, phone)
                showAddDialog = false
            }
```

- [ ] **Step 4: Compiler**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/screen/ClientsScreen.kt
git commit -m "feat(clients): animation des cartes et haptique à l'ajout"
```

---

## Task 5: Détail Client — mini-stats à 3 tuiles + haptique

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/ClientDetailScreen.kt`

**Interfaces:**
- Consumes: rien de neuf.
- Produces: rien pour les autres tâches.

- [ ] **Step 1: Ajouter les imports haptique**

Dans `app/src/main/java/com/lissafi/app/ui/screen/ClientDetailScreen.kt`, ajouter après `import androidx.compose.ui.graphics.vector.ImageVector` (ligne 15) :

```kotlin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
```

- [ ] **Step 2: Déclarer `haptic` dans le composable principal**

Dans `ClientDetailScreen`, remplacer :

```kotlin
    val client by viewModel.selectedClient.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
```

par :

```kotlin
    val client by viewModel.selectedClient.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val haptic = LocalHapticFeedback.current
```

- [ ] **Step 3: Haptique à l'ajout de dette**

Dans le bloc `if (showAddDebtDialog) { AddDebtDialog(...) }`, remplacer le `onSave` :

```kotlin
            onSave = { amount, note ->
                viewModel.addDebt(clientId, amount, note)
                showAddDebtDialog = false
            }
```

par :

```kotlin
            onSave = { amount, note ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addDebt(clientId, amount, note)
                showAddDebtDialog = false
            }
```

- [ ] **Step 4: Haptique au remboursement**

Dans le bloc `if (showRepayDialog) { RepayDialog(...) }`, remplacer le `onSave` :

```kotlin
            onSave = { amount ->
                viewModel.addRepayment(clientId, amount)
                showRepayDialog = false
            }
```

par :

```kotlin
            onSave = { amount ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addRepayment(clientId, amount)
                showRepayDialog = false
            }
```

- [ ] **Step 5: Étendre `MiniStats` à 3 tuiles**

Remplacer la fonction privée `MiniStats` (lignes 287-310) par :

```kotlin
@Composable
private fun MiniStats(client: Client, transactions: List<DebtTransaction>) {
    val purchaseCount = transactions.count { it.amount > 0 }
    val totalRepaid = transactions.filter { it.amount < 0 }.sumOf { -it.amount }
    val daysSince = ((System.currentTimeMillis() - client.createdAt) / (24 * 3600 * 1000)).toInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatMiniCard(
            icon = LissafiIcons.Panier,
            value = "$purchaseCount",
            label = "ventes à crédit",
            color = Secondary,
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            icon = LissafiIcons.Rembourser,
            value = FormatUtils.formatFCFA(totalRepaid).removeSuffix(" FCFA"),
            label = "total remboursé",
            color = Success,
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            icon = LissafiIcons.Recents,
            value = if (daysSince <= 0) "Aujourd'hui" else "Il y a ${daysSince}j",
            label = "client depuis",
            color = Primary,
            modifier = Modifier.weight(1f)
        )
    }
}
```

Note : la valeur de la tuile "total remboursé" utilise `removeSuffix(" FCFA")` car `StatMiniCard` affiche une valeur courte sur une ligne dans un tiers d'écran — le label "total remboursé" porte déjà le contexte FCFA, comme les autres tuiles qui affichent des nombres nus.

- [ ] **Step 6: Compiler**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/screen/ClientDetailScreen.kt
git commit -m "feat(clients): mini-stats enrichies et haptique sur les actions de dette"
```

---

## Task 6: Build complet et vérification

**Files:** Aucun changement de code — vérification finale uniquement.

- [ ] **Step 1: Build debug complet**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Installer sur un appareil/émulateur connecté (si disponible)**

Run: `./gradlew installDebug`
Expected: `BUILD SUCCESSFUL`, l'app s'installe sans erreur.

- [ ] **Step 3: Vérification manuelle (limitée par l'environnement)**

Les écrans Produits/Clients/Détail ne sont accessibles qu'après connexion, et l'authentification Supabase n'est pas configurée dans cet environnement (limitation connue du pilier onboarding). Vérifications à faire par l'utilisateur sur device/émulateur avec un compte valide :
- Produits : compteurs sur chips (`Tous · N`, `En stock · N`, `Alerte stock · N`), marge unitaire verte/rouge quand `buyPrice > 0`, animation d'insertion/retrait des cartes, haptique à l'ajout et à la suppression.
- Clients : compteurs sur chips, carte "À recouvrer" visible quand dettes > 0 et recherche vide (cachée pendant une recherche ou sans dette), animation des cartes, haptique à l'ajout.
- Détail Client : 3 tuiles (ventes à crédit / total remboursé / client depuis), haptique à l'ajout de dette et au remboursement.
