# Amélioration UX Caisse + Activité — Phase 1 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enrichir les écrans Caisse et Activité de Lissafi avec des insights de tendance, une alerte stock bas, une répartition horaire des ventes, et des micro-interactions (haptique, animations) — sans changer le système de design existant.

**Architecture:** Ajouts additifs sur les fichiers existants : nouvelle méthode DB/repository pour le stock bas, nouveaux champs calculés dans `ReportViewModel`, nouveaux composants réutilisables dans `LissafiComponents.kt`, nouveau `StateFlow` dans `CartViewModel` pour le total du jour. Aucune nouvelle dépendance externe — les graphiques utilisent des `Box`/`Row` Compose natifs (comme `TopProductBar` déjà présent), pas Vico, pour rester dans une API stable et déjà maîtrisée par le projet.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, SQLite brut (pas Room), coroutines. Aucune nouvelle dépendance.

## Global Constraints

- Aucun test unitaire dans le repo (`app/src`) — la vérification de chaque tâche se fait par compilation Kotlin (`./gradlew :app:compileDebugKotlin`) et, pour la dernière tâche, un `assembleDebug` complet.
- `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` est requis avant toute commande Gradle.
- L'argent reste en `Int` FCFA partout — jamais de `Double` pour un montant.
- Isolation par utilisateur : toute nouvelle requête DB doit filtrer par `user_id` comme les méthodes existantes (`getRecentProducts`, `getTopProducts`, etc.).
- Commits en français, conventional commits (`feat(...)`, `fix(...)`), jamais de mention d'IA/Claude.
- Ne pas toucher au thème (`ui/theme/Color.kt`, `ui/theme/Theme.kt`) — on réutilise les tokens existants (`Primary`, `Secondary`, `Success`, `TextSecondary`, etc.).

---

## Task 1: Requête stock bas (DB + Repository)

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/data/LissafiDatabase.kt:212` (juste après `getRecentProducts`, avant `upsertProduct` ligne 214)
- Modify: `app/src/main/java/com/lissafi/app/data/repository/LissafiRepository.kt:44` (juste après `getRecentProducts`, avant `upsertProduct` ligne 46)

**Interfaces:**
- Produces: `LissafiRepository.getLowStockProducts(): List<Product>` — utilisé par Task 6 (`ReportViewModel`).

- [ ] **Step 1: Ajouter `getLowStockProducts` dans `LissafiDatabase.kt`**

Dans `app/src/main/java/com/lissafi/app/data/LissafiDatabase.kt`, insérer après la ligne 212 (juste après la fermeture de `getRecentProducts`, avant `suspend fun upsertProduct`) :

```kotlin
    /** Produits dont le stock est descendu au niveau ou en dessous du seuil d'alerte. */
    suspend fun getLowStockProducts(userId: String = ""): List<Product> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Product>()
        val where = if (userId.isNotEmpty()) "AND user_id = ?" else ""
        val args = if (userId.isNotEmpty()) arrayOf(userId) else null
        readableDatabase.rawQuery(
            "SELECT * FROM products WHERE deleted = 0 AND stock <= min_stock $where ORDER BY stock ASC",
            args
        ).use { cursor ->
            while (cursor.moveToNext()) list.add(cursor.toProduct())
        }
        list
    }
```

- [ ] **Step 2: Exposer la méthode dans `LissafiRepository.kt`**

Dans `app/src/main/java/com/lissafi/app/data/repository/LissafiRepository.kt`, insérer après la ligne 44 (`getRecentProducts`), avant `suspend fun upsertProduct` :

```kotlin
    suspend fun getLowStockProducts(): List<Product> = db.getLowStockProducts(currentUserId)
```

- [ ] **Step 3: Compiler**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lissafi/app/data/LissafiDatabase.kt app/src/main/java/com/lissafi/app/data/repository/LissafiRepository.kt
git commit -m "feat(stock): requête produits en stock bas"
```

---

## Task 2: `FormatUtils.todayRange()`

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/service/FormatUtils.kt`

**Interfaces:**
- Produces: `FormatUtils.todayRange(): Pair<Long, Long>` — utilisé par Task 7 (`CartViewModel`).

- [ ] **Step 1: Ajouter l'import `Calendar`**

Dans `app/src/main/java/com/lissafi/app/service/FormatUtils.kt`, remplacer :

```kotlin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
```

par :

```kotlin
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
```

- [ ] **Step 2: Ajouter `todayRange()`**

Dans le même fichier, insérer après la fermeture de `formatDateShort` (ligne 40, juste avant le `}` final de l'objet ligne 41) :

```kotlin

    /** Plage [minuit aujourd'hui, maintenant) — pour les totaux "du jour" dans l'UI. */
    fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis to end
    }
```

- [ ] **Step 3: Compiler**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lissafi/app/service/FormatUtils.kt
git commit -m "feat(caisse): utilitaire de plage horaire du jour"
```

---

## Task 3: Composant `TrendBadge`

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/components/LissafiComponents.kt`

**Interfaces:**
- Consumes: `Success`, `Secondary` (couleurs du thème, déjà importées via `import com.lissafi.app.ui.theme.*`), `Lucide.TrendingUp`/`Lucide.TrendingDown` (déjà accessibles via `import com.composables.icons.lucide.*`).
- Produces: `@Composable fun TrendBadge(percentage: Int, modifier: Modifier = Modifier)` — utilisé par Task 8 (`ReportsScreen.KpiTile`).

- [ ] **Step 1: Ajouter `TrendBadge` après `StatusBadge`**

Dans `app/src/main/java/com/lissafi/app/ui/components/LissafiComponents.kt`, insérer après la fin de `StatusBadge` (ligne 579, juste avant le commentaire `// STEPPER` ligne 581-583) :

```kotlin
// ============================================================
// PASTILLE DE TENDANCE — Comparaison vs période précédente
// ============================================================
@Composable
fun TrendBadge(
    percentage: Int,
    modifier: Modifier = Modifier
) {
    val isPositive = percentage >= 0
    val color = if (isPositive) Success else Secondary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isPositive) Lucide.TrendingUp else Lucide.TrendingDown,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(11.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = "${if (isPositive) "+" else ""}$percentage%",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
```

- [ ] **Step 2: Compiler**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/components/LissafiComponents.kt
git commit -m "feat(ui): composant TrendBadge"
```

---

## Task 4: Composant `LowStockBanner`

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/components/LissafiComponents.kt`

**Interfaces:**
- Consumes: `Product` (entité, nécessite un nouvel import `com.lissafi.app.data.entity.Product`), `LissafiIcons.Alerte`, `SecondaryContainer`, `OnSecondaryContainer` (déjà dans le thème).
- Produces: `@Composable fun LowStockBanner(products: List<Product>, onViewAll: () -> Unit, modifier: Modifier = Modifier)` — utilisé par Task 8 (`ReportsScreen`).

- [ ] **Step 1: Ajouter l'import `Product`**

Dans `app/src/main/java/com/lissafi/app/ui/components/LissafiComponents.kt`, ajouter après la ligne 31 (`import com.lissafi.app.data.sync.SyncStatus`) :

```kotlin
import com.lissafi.app.data.entity.Product
```

- [ ] **Step 2: Ajouter `LowStockBanner` après `InfoRow`**

Insérer après la fin de `InfoRow` (ligne 928, juste avant le commentaire `// SEGMENTED CONTROL` ligne 930-932) :

```kotlin
// ============================================================
// BANNIÈRE STOCK BAS — Liste compacte des produits sous le seuil
// ============================================================
@Composable
fun LowStockBanner(
    products: List<Product>,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty()) return
    LissafiCard(
        modifier = modifier,
        cornerRadius = 18,
        elevation = 2,
        containerColor = SecondaryContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = LissafiIcons.Alerte,
                    contentDescription = null,
                    tint = Secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Stock bas",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = OnSecondaryContainer
                )
            }
            Spacer(Modifier.height(10.dp))
            products.take(3).forEach { product ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.name,
                        fontSize = 13.sp,
                        color = OnSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${product.stock} restant${if (product.stock > 1) "s" else ""}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Secondary
                    )
                }
            }
            if (products.size > 3) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "et ${products.size - 3} autre${if (products.size - 3 > 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onViewAll, contentPadding = PaddingValues(0.dp)) {
                Text("Voir les produits", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Secondary)
            }
        }
    }
}
```

- [ ] **Step 3: Compiler**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/components/LissafiComponents.kt
git commit -m "feat(ui): bannière stock bas"
```

---

## Task 5: Composant `SuccessPulse` + intégration dans le reçu de vente

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/components/LissafiComponents.kt`
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/CaisseScreen.kt:1026-1033`

**Interfaces:**
- Produces: `@Composable fun SuccessPulse(modifier: Modifier = Modifier, size: Int = 48, iconSize: Int = 24, backgroundColor: Color = PrimaryContainer, iconTint: Color = Primary)`

- [ ] **Step 1: Ajouter l'import `Spring`**

Dans `app/src/main/java/com/lissafi/app/ui/components/LissafiComponents.kt`, remplacer :

```kotlin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
```

par :

```kotlin
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
```

- [ ] **Step 2: Ajouter `SuccessPulse` après `IconCircle`**

Insérer à la toute fin du fichier, après la fermeture de `IconCircle` (ligne 1003-1004) :

```kotlin

// ============================================================
// PULSE DE SUCCÈS — Animation de confirmation (vente encaissée)
// ============================================================
@Composable
fun SuccessPulse(
    modifier: Modifier = Modifier,
    size: Int = 48,
    iconSize: Int = 24,
    backgroundColor: Color = PrimaryContainer,
    iconTint: Color = Primary
) {
    var started by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (started) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "successPulseScale"
    )
    LaunchedEffect(Unit) { started = true }
    Box(
        modifier = modifier
            .size(size.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = LissafiIcons.Succes,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}
```

- [ ] **Step 3: Utiliser `SuccessPulse` dans `ReceiptSheet`**

Dans `app/src/main/java/com/lissafi/app/ui/screen/CaisseScreen.kt`, dans la fonction privée `ReceiptSheet`, remplacer :

```kotlin
                IconCircle(
                    icon = LissafiIcons.Succes,
                    backgroundColor = PrimaryContainer,
                    iconTint = Primary,
                    size = 48,
                    iconSize = 24
                )
```

par :

```kotlin
                SuccessPulse(size = 48, iconSize = 24)
```

Ajouter l'import correspondant, après la ligne `import com.lissafi.app.ui.components.SegmentedControl` :

```kotlin
import com.lissafi.app.ui.components.SuccessPulse
```

- [ ] **Step 4: Compiler**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/components/LissafiComponents.kt app/src/main/java/com/lissafi/app/ui/screen/CaisseScreen.kt
git commit -m "feat(caisse): animation de succès sur le reçu de vente"
```

---

## Task 6: Haptique sur le stepper de quantité

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/components/LissafiComponents.kt:584-639`

- [ ] **Step 1: Ajouter les imports haptique**

Dans `app/src/main/java/com/lissafi/app/ui/components/LissafiComponents.kt`, ajouter après `import androidx.compose.ui.graphics.vector.ImageVector` (ligne 22) :

```kotlin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
```

- [ ] **Step 2: Modifier `QuantityStepper` pour déclencher un retour haptique**

Remplacer la fonction complète (lignes 584-636) :

```kotlin
@Composable
fun QuantityStepper(
    quantity: Double,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Primary
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF000000).copy(alpha = 0.04f))
                .clickable(onClick = onDecrease),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = LissafiIcons.Fermer,
                contentDescription = "Retirer",
                tint = TextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = formatQuantity(quantity),
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = OnBackground,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.10f))
                .clickable(onClick = onIncrease),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = LissafiIcons.Ajouter,
                contentDescription = "Ajouter",
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
```

par :

```kotlin
@Composable
fun QuantityStepper(
    quantity: Double,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Primary
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF000000).copy(alpha = 0.04f))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDecrease()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = LissafiIcons.Fermer,
                contentDescription = "Retirer",
                tint = TextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = formatQuantity(quantity),
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = OnBackground,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.10f))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onIncrease()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = LissafiIcons.Ajouter,
                contentDescription = "Ajouter",
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
```

- [ ] **Step 3: Compiler**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/components/LissafiComponents.kt
git commit -m "feat(caisse): retour haptique sur le stepper de quantité"
```

---

## Task 7: `ReportViewModel` — tendances, répartition horaire, stock bas, meilleur jour

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/viewmodel/ReportViewModel.kt` (remplacement complet du fichier)

**Interfaces:**
- Consumes: `LissafiRepository.getLowStockProducts()` (Task 1), `LissafiRepository.sumTotalBetween/sumCreditBetween/countSalesBetween/getSalesBetween/getSaleItems/getProduct/getTopProducts` (existants, inchangés).
- Produces: `ReportState` avec les nouveaux champs `comptantTrend: Int?`, `creditTrend: Int?`, `profitTrend: Int?`, `hourlyBreakdown: List<Int>` (taille 24, index = heure 0-23), `lowStockProducts: List<Product>`, `bestDay: RevenuePoint?` — consommés par Task 8 (`ReportsScreen`).

- [ ] **Step 1: Remplacer le contenu complet de `ReportViewModel.kt`**

```kotlin
package com.lissafi.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lissafi.app.data.LissafiDatabase.TopProduct
import com.lissafi.app.data.entity.Product
import com.lissafi.app.data.entity.Sale
import com.lissafi.app.data.repository.LissafiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.util.Calendar

enum class ReportPeriod { TODAY, WEEK, MONTH }

// Point de la série temporelle du chiffre d'affaires (pour la sparkline)
data class RevenuePoint(
    val date: Long,    // epoch millis du début du jour
    val amount: Int    // CA total ce jour-là
)

data class ReportState(
    val period: ReportPeriod = ReportPeriod.TODAY,
    val totalVentes: Int = 0,
    val totalCredits: Int = 0,
    val totalComptant: Int = 0,
    val estimatedProfit: Int = 0,
    val nbTransactions: Int = 0,
    val panierMoyen: Int = 0,
    val topProducts: List<TopProduct> = emptyList(),
    val revenueSeries: List<RevenuePoint> = emptyList(),
    val comptantTrend: Int? = null,
    val creditTrend: Int? = null,
    val profitTrend: Int? = null,
    val hourlyBreakdown: List<Int> = emptyList(),
    val lowStockProducts: List<Product> = emptyList(),
    val bestDay: RevenuePoint? = null,
    val isLoading: Boolean = false
)

class ReportViewModel(private val repository: LissafiRepository) : ViewModel() {

    private val _state = MutableStateFlow(ReportState())
    val state: StateFlow<ReportState> = _state.asStateFlow()

    init {
        loadReport(ReportPeriod.TODAY)
    }

    fun loadReport(period: ReportPeriod) {
        _state.value = _state.value.copy(period = period, isLoading = true)
        viewModelScope.launch {
            val (start, end) = getDateRange(period)
            val total = repository.sumTotalBetween(start, end)
            val credit = repository.sumCreditBetween(start, end)
            val count = repository.countSalesBetween(start, end)
            val top = repository.getTopProducts(start, end)
            val sales = repository.getSalesBetween(start, end)
            val comptant = total - credit
            val profit = computeProfit(sales)

            // Série temporelle du CA par jour (pour la sparkline de l'écran Activité).
            val revenueSeries = sales
                .groupBy { startOfDay(it.date) }
                .map { (day, daySales) -> RevenuePoint(day, daySales.sumOf { it.total }) }
                .sortedBy { it.date }

            // Répartition par heure de la journée — calculée en mémoire à partir des
            // ventes déjà chargées, pas de requête DB supplémentaire.
            val hourly = IntArray(24)
            for (sale in sales) {
                val cal = Calendar.getInstance()
                cal.timeInMillis = sale.date
                hourly[cal.get(Calendar.HOUR_OF_DAY)]++
            }

            // Comparaison avec la période équivalente immédiatement précédente
            // (même durée que [start, end), juste avant start).
            val (prevStart, prevEnd) = getPreviousDateRange(start, end)
            val prevTotal = repository.sumTotalBetween(prevStart, prevEnd)
            val prevCredit = repository.sumCreditBetween(prevStart, prevEnd)
            val prevComptant = prevTotal - prevCredit
            val prevSales = repository.getSalesBetween(prevStart, prevEnd)
            val prevProfit = computeProfit(prevSales)

            val lowStock = repository.getLowStockProducts()
            val bestDay = revenueSeries.maxByOrNull { it.amount }

            _state.value = _state.value.copy(
                totalVentes = total,
                totalCredits = credit,
                totalComptant = comptant,
                estimatedProfit = profit,
                nbTransactions = count,
                panierMoyen = if (count > 0) total / count else 0,
                topProducts = top,
                revenueSeries = revenueSeries,
                comptantTrend = trendPercent(comptant, prevComptant),
                creditTrend = trendPercent(credit, prevCredit),
                profitTrend = trendPercent(profit, prevProfit),
                hourlyBreakdown = hourly.toList(),
                lowStockProducts = lowStock,
                bestDay = bestDay,
                isLoading = false
            )
        }
    }

    // Bénéfice estimé : somme des ventes - somme des prix d'achat. Toutes les
    // ventes comptent dans la marge (comptant ET crédit) : la marchandise sort
    // dans les deux cas.
    private suspend fun computeProfit(sales: List<Sale>): Int {
        var profit = 0
        for (sale in sales) {
            val items = repository.getSaleItems(sale.id)
            for (item in items) {
                val product = repository.getProduct(item.barcode)
                if (product != null && product.buyPrice > 0) {
                    profit += ((item.price - product.buyPrice) * item.quantity).roundToInt()
                } else {
                    // Pas de prix d'achat connu → on compte le prix de vente comme bénéfice
                    profit += (item.price * item.quantity).roundToInt()
                }
            }
        }
        return profit
    }

    // % d'évolution vs la période précédente. null si la période précédente n'a
    // aucune donnée (évite un pourcentage absurde du type +∞%).
    private fun trendPercent(current: Int, previous: Int): Int? {
        if (previous <= 0) return null
        return (((current - previous).toDouble() / previous) * 100).roundToInt()
    }

    private fun getDateRange(period: ReportPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = when (period) {
            ReportPeriod.TODAY -> cal.timeInMillis
            ReportPeriod.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.timeInMillis
            }
            ReportPeriod.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis
            }
        }
        return start to end
    }

    // Période équivalente précédente : même durée que [start, end), juste avant `start`.
    private fun getPreviousDateRange(start: Long, end: Long): Pair<Long, Long> {
        val duration = end - start
        return (start - duration) to start
    }

    // Ramène un timestamp au début du jour (minuit) pour regrouper les ventes par jour
    private fun startOfDay(epoch: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epoch
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
```

- [ ] **Step 2: Compiler**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/viewmodel/ReportViewModel.kt
git commit -m "feat(activite): tendances, repartition horaire, stock bas et meilleur jour"
```

---

## Task 8: `ReportsScreen` — intégrer tendances, stock bas, heures de pointe, meilleur jour

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/ReportsScreen.kt` (remplacement complet du fichier)
- Modify: `app/src/main/java/com/lissafi/app/ui/navigation/LissafiNavHost.kt:265-270`

**Interfaces:**
- Consumes: `ReportState.comptantTrend/creditTrend/profitTrend/hourlyBreakdown/lowStockProducts/bestDay` (Task 7), `TrendBadge` (Task 3), `LowStockBanner` (Task 4).
- Produces: `ReportsScreen(viewModel: ReportViewModel, onBack: () -> Unit, onNavigateToProducts: () -> Unit)` — signature changée, consommée par `LissafiNavHost`.

- [ ] **Step 1: Remplacer le contenu complet de `ReportsScreen.kt`**

```kotlin
package com.lissafi.app.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.components.AmountText
import com.lissafi.app.ui.components.EmptyState
import com.lissafi.app.ui.components.IconCircle
import com.lissafi.app.ui.components.InfoRow
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.components.LowStockBanner
import com.lissafi.app.ui.components.SectionHeader
import com.lissafi.app.ui.components.SegmentedControl
import com.lissafi.app.ui.components.TrendBadge
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.OnBackground
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.Secondary
import com.lissafi.app.ui.theme.Success
import com.lissafi.app.ui.theme.SurfaceAlt
import com.lissafi.app.ui.theme.TextSecondary
import com.lissafi.app.ui.theme.TextTertiary
import com.lissafi.app.ui.theme.White
import com.lissafi.app.ui.viewmodel.ReportPeriod
import com.lissafi.app.ui.viewmodel.ReportState
import com.lissafi.app.ui.viewmodel.ReportViewModel
import com.lissafi.app.ui.viewmodel.RevenuePoint
import com.lissafi.app.data.LissafiDatabase.TopProduct
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import java.text.SimpleDateFormat
import java.util.Locale

// ============================================================
// ÉCRAN ACTIVITÉ — Dashboard avec sparkline Vico + KPIs
// ============================================================
@Composable
fun ReportsScreen(
    viewModel: ReportViewModel,
    onBack: () -> Unit,
    onNavigateToProducts: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        LissafiHeader(
            title = "Activité",
            subtitle = when (state.period) {
                ReportPeriod.TODAY -> "Aujourd'hui"
                ReportPeriod.WEEK -> "Cette semaine"
                ReportPeriod.MONTH -> "Ce mois"
            },
            onBack = onBack
        )

        // ── PÉRIODE ──
        SegmentedControl(
            options = listOf("Aujourd'hui", "Cette semaine", "Ce mois"),
            selectedIndex = when (state.period) {
                ReportPeriod.TODAY -> 0
                ReportPeriod.WEEK -> 1
                ReportPeriod.MONTH -> 2
            },
            onSelect = { index ->
                viewModel.loadReport(
                    when (index) {
                        0 -> ReportPeriod.TODAY
                        1 -> ReportPeriod.WEEK
                        else -> ReportPeriod.MONTH
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
            }
        } else if (state.nbTransactions == 0) {
            EmptyState(
                icon = LissafiIcons.Activite,
                title = "Aucune vente",
                message = "Les chiffres de ta caisse apparaîtront ici. Enregistre une vente depuis la caisse pour commencer.",
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                // ── STOCK BAS — la plus actionnable, en premier ──
                if (state.lowStockProducts.isNotEmpty()) {
                    item {
                        LowStockBanner(
                            products = state.lowStockProducts,
                            onViewAll = onNavigateToProducts,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                // ── CARTE CHIFFRE D'AFFAIRES + SPARKLINE ──
                item { RevenueCard(state) }

                // ── GRILLE DE KPIs 2×2 ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiTile(
                            label = "Comptant",
                            value = state.totalComptant,
                            icon = LissafiIcons.Encaisser,
                            color = Primary,
                            percentage = if (state.totalVentes > 0) (state.totalComptant.toLong() * 100 / state.totalVentes).toInt() else 0,
                            trend = state.comptantTrend,
                            modifier = Modifier.weight(1f)
                        )
                        KpiTile(
                            label = "À crédit",
                            value = state.totalCredits,
                            icon = LissafiIcons.Credit,
                            color = Secondary,
                            percentage = if (state.totalVentes > 0) (state.totalCredits.toLong() * 100 / state.totalVentes).toInt() else 0,
                            trend = state.creditTrend,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiTile(
                            label = "Marge estimée",
                            value = state.estimatedProfit,
                            icon = LissafiIcons.Marge,
                            color = Success,
                            percentage = if (state.totalVentes > 0) (state.estimatedProfit.toLong() * 100 / state.totalVentes).toInt() else 0,
                            trend = state.profitTrend,
                            modifier = Modifier.weight(1f)
                        )
                        KpiTile(
                            label = "Panier moyen",
                            value = state.panierMoyen,
                            icon = LissafiIcons.Panier,
                            color = TextSecondary,
                            percentage = null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── HEURES DE POINTE — peu lisible sur un mois entier ──
                if (state.period != ReportPeriod.MONTH && state.hourlyBreakdown.any { it > 0 }) {
                    item {
                        SectionHeader(
                            text = "Heures de pointe",
                            icon = LissafiIcons.Recents,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    item {
                        LissafiCard(cornerRadius = 18, elevation = 2) {
                            HourlyBarsChart(
                                hourly = state.hourlyBreakdown,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }
                }

                // ── PRODUITS LES PLUS VENDUS ──
                if (state.topProducts.isNotEmpty()) {
                    item {
                        SectionHeader(
                            text = "Produits les plus vendus",
                            icon = LissafiIcons.Produit,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(state.topProducts.take(10)) { product ->
                        TopProductBar(
                            product = product,
                            maxCount = state.topProducts.first().count
                        )
                    }
                }

                // ── CRÉDITS EN ATTENTE ──
                if (state.totalCredits > 0) {
                    item {
                        SectionHeader(
                            text = "Crédits en attente",
                            icon = LissafiIcons.Credit,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    item {
                        LissafiCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                InfoRow(
                                    icon = LissafiIcons.Credit,
                                    label = "Total à recouvrer",
                                    value = FormatUtils.formatFCFA(state.totalCredits),
                                    valueColor = Secondary
                                )
                                InfoRow(
                                    icon = LissafiIcons.Encaisser,
                                    label = "Ventes comptant",
                                    value = FormatUtils.formatFCFA(state.totalComptant),
                                    valueColor = Primary
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ============================================================
// CARTE CHIFFRE D'AFFAIRES — Fond vert Primary + sparkline Vico
// ============================================================
@Composable
private fun RevenueCard(state: ReportState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = LissafiIcons.Activite,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Chiffre d'affaires",
                    color = White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(8.dp))
            AnimatedContent(targetState = state.totalVentes, label = "chiffreAffaires") { total ->
                Text(
                    text = FormatUtils.formatFCFA(total),
                    color = White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${state.nbTransactions} vente${if (state.nbTransactions > 1) "s" else ""} sur la période",
                color = White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
            if (state.bestDay != null && state.revenueSeries.size >= 2) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Meilleur jour : ${formatDayLabel(state.bestDay.date)} · ${FormatUtils.formatFCFA(state.bestDay.amount)}",
                    color = White.copy(alpha = 0.75f),
                    fontSize = 11.sp
                )
            }

            // Sparkline du CA par jour — seulement si assez de points
            if (state.revenueSeries.size >= 2) {
                Spacer(Modifier.height(12.dp))
                RevenueSparkline(series = state.revenueSeries)
            }
        }
    }
}

private fun formatDayLabel(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEE d", Locale.FRENCH)
    return sdf.format(java.util.Date(timestamp))
}

// ============================================================
// SPARKLINE VICO — Ligne blanche sur fond vert, sans axes
// ============================================================
@Composable
private fun RevenueSparkline(series: List<RevenuePoint>) {
    val modelProducer = remember { CartesianChartModelProducer() }

    // Alimente le modèle avec les montants (série temporelle du CA)
    LaunchedEffect(series) {
        modelProducer.runTransaction {
            lineSeries {
                val amounts: List<Number> = series.map { it.amount.toFloat() as Number }
                series(amounts)
            }
        }
    }

    // Ligne blanche (contraste sur la carte vert Primary) avec remplissage translucide.
    // Les autres paramètres (points, labels, connecteur) gardent leur valeur par défaut.
    val line = remember {
        LineCartesianLayer.Line(
            fill = LineCartesianLayer.LineFill.single(fill(Color.White)),
            stroke = LineCartesianLayer.LineStroke.Continuous(4f),
            areaFill = LineCartesianLayer.AreaFill.single(fill(Color.White.copy(alpha = 0.25f)))
        )
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(line)
            )
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    )
}

// ============================================================
// BARRES HORAIRES — Répartition des ventes par heure (0-23h)
// Composant Compose natif (comme TopProductBar), pas Vico : évite
// la complexité d'un axe personnalisé pour 24 barres compactes.
// ============================================================
@Composable
private fun HourlyBarsChart(hourly: List<Int>, modifier: Modifier = Modifier) {
    val maxCount = (hourly.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            hourly.forEach { count ->
                val fraction = (count.toFloat() / maxCount).coerceIn(0.04f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(if (count > 0) Primary else SurfaceAlt)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, 6, 12, 18, 23).forEach { h ->
                Text(text = "${h}h", fontSize = 10.sp, color = TextTertiary)
            }
        }
    }
}

// ============================================================
// TUILE KPI — Petite carte blanche avec icône en cercle
// ============================================================
@Composable
private fun KpiTile(
    label: String,
    value: Int,
    icon: ImageVector,
    color: Color,
    percentage: Int?,
    modifier: Modifier = Modifier,
    trend: Int? = null
) {
    LissafiCard(modifier = modifier, cornerRadius = 18, elevation = 2) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconCircle(
                    icon = icon,
                    backgroundColor = color.copy(alpha = 0.1f),
                    iconTint = color,
                    size = 36,
                    iconSize = 18
                )
                if (percentage != null) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "$percentage%",
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            AmountText(
                amount = value,
                fontSize = 17,
                color = color,
                fontWeight = FontWeight.Bold
            )
            if (trend != null) {
                Spacer(Modifier.height(4.dp))
                TrendBadge(percentage = trend)
            }
        }
    }
}

// ============================================================
// BARRE PRODUIT — Barre horizontale proportionnelle
// ============================================================
@Composable
private fun TopProductBar(product: TopProduct, maxCount: Int) {
    val fraction = if (maxCount > 0) (product.count.toFloat() / maxCount).coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = product.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${product.count}×",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SurfaceAlt)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Primary)
            )
        }
    }
}
```

- [ ] **Step 2: Wire `onNavigateToProducts` dans `LissafiNavHost.kt`**

Dans `app/src/main/java/com/lissafi/app/ui/navigation/LissafiNavHost.kt`, remplacer :

```kotlin
                composable(Routes.ACTIVITY) {
                    ReportsScreen(
                        viewModel = reportViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
```

par :

```kotlin
                composable(Routes.ACTIVITY) {
                    ReportsScreen(
                        viewModel = reportViewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToProducts = { navController.navigate(Routes.PRODUCTS) }
                    )
                }
```

- [ ] **Step 3: Compiler**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/screen/ReportsScreen.kt app/src/main/java/com/lissafi/app/ui/navigation/LissafiNavHost.kt
git commit -m "feat(activite): affichage tendances, stock bas, heures de pointe et meilleur jour"
```

---

## Task 9: `CartViewModel` — total du jour

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/viewmodel/CartViewModel.kt`

**Interfaces:**
- Consumes: `FormatUtils.todayRange()` (Task 2), `repository.sumTotalBetween(start, end)` (existant).
- Produces: `CartViewModel.todayTotal: StateFlow<Int>` — consommé par Task 10 (`CaisseScreen`).

- [ ] **Step 1: Ajouter l'import `FormatUtils`**

Dans `app/src/main/java/com/lissafi/app/ui/viewmodel/CartViewModel.kt`, ajouter après la ligne 7 (`import com.lissafi.app.service.PremiumManager`) :

```kotlin
import com.lissafi.app.service.FormatUtils
```

- [ ] **Step 2: Ajouter le `StateFlow` et le rafraîchir au démarrage et après une vente**

Remplacer :

```kotlin
    private val _lastSale = MutableStateFlow<LastSale?>(null)
    val lastSale: StateFlow<LastSale?> = _lastSale.asStateFlow()

    fun clearScanResult() { _scanResult.value = null }
    fun clearLastSale() { _lastSale.value = null }

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(isPremium = premiumManager.isPremium())
            loadRecentProducts()
        }
    }
```

par :

```kotlin
    private val _lastSale = MutableStateFlow<LastSale?>(null)
    val lastSale: StateFlow<LastSale?> = _lastSale.asStateFlow()

    private val _todayTotal = MutableStateFlow(0)
    val todayTotal: StateFlow<Int> = _todayTotal.asStateFlow()

    fun clearScanResult() { _scanResult.value = null }
    fun clearLastSale() { _lastSale.value = null }

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(isPremium = premiumManager.isPremium())
            loadRecentProducts()
            refreshTodayTotal()
        }
    }

    private suspend fun refreshTodayTotal() {
        val (start, end) = FormatUtils.todayRange()
        _todayTotal.value = repository.sumTotalBetween(start, end)
    }
```

- [ ] **Step 3: Rafraîchir le total après un encaissement**

Dans la fonction `encaisser`, remplacer :

```kotlin
        _lastSale.value = LastSale(
            items = s.items.toList(),
            total = s.total,
            amountPaid = if (s.isCredit) 0 else amountPaid,
            changeGiven = if (s.isCredit) 0 else amountPaid - s.total,
            isCredit = s.isCredit,
            date = System.currentTimeMillis()
        )
        return true
```

par :

```kotlin
        _lastSale.value = LastSale(
            items = s.items.toList(),
            total = s.total,
            amountPaid = if (s.isCredit) 0 else amountPaid,
            changeGiven = if (s.isCredit) 0 else amountPaid - s.total,
            isCredit = s.isCredit,
            date = System.currentTimeMillis()
        )
        refreshTodayTotal()
        return true
```

- [ ] **Step 4: Compiler**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/viewmodel/CartViewModel.kt
git commit -m "feat(caisse): total du jour dans le CartViewModel"
```

---

## Task 10: `CaisseScreen` — pastille du jour, haptique, animation du panier

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/CaisseScreen.kt`

**Interfaces:**
- Consumes: `CartViewModel.todayTotal` (Task 9).

- [ ] **Step 1: Ajouter les imports haptique**

Dans `app/src/main/java/com/lissafi/app/ui/screen/CaisseScreen.kt`, ajouter après la ligne `import androidx.compose.ui.draw.clip` (ligne 33) :

```kotlin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
```

- [ ] **Step 2: Déclarer `haptic` dans le composable principal**

Remplacer :

```kotlin
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
```

par :

```kotlin
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
```

- [ ] **Step 3: Retour haptique sur ajout au panier (scan et recherche passent tous les deux par `scanProduct` → `scanResult`)**

Remplacer :

```kotlin
    LaunchedEffect(scanResult) {
        scanResult?.let {
            scanIsSuccess = it.startsWith("OK:")
            scanFeedback = when {
                it.startsWith("OK:") -> "${it.removePrefix("OK:")} ajouté"
                it.startsWith("NOT_FOUND:") -> "Code-barres inconnu"
                else -> null
            }
            viewModel.clearScanResult()
            delay(2200)
            scanFeedback = null
        }
    }
```

par :

```kotlin
    LaunchedEffect(scanResult) {
        scanResult?.let {
            scanIsSuccess = it.startsWith("OK:")
            if (scanIsSuccess) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scanFeedback = when {
                it.startsWith("OK:") -> "${it.removePrefix("OK:")} ajouté"
                it.startsWith("NOT_FOUND:") -> "Code-barres inconnu"
                else -> null
            }
            viewModel.clearScanResult()
            delay(2200)
            scanFeedback = null
        }
    }
```

- [ ] **Step 4: Pastille "Aujourd'hui" dans le header**

Remplacer :

```kotlin
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = LissafiIcons.Reglages,
                            contentDescription = "Réglages",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
```

par :

```kotlin
                actions = {
                    val todayTotal by viewModel.todayTotal.collectAsState()
                    if (todayTotal > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Primary.copy(alpha = 0.08f))
                                .clickable(onClick = onNavigateToReports)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Aujourd'hui · ${FormatUtils.formatFCFA(todayTotal)}",
                                color = Primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = LissafiIcons.Reglages,
                            contentDescription = "Réglages",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
```

- [ ] **Step 5: Retour haptique sur la validation de vente (comptant et crédit)**

Remplacer :

```kotlin
                    onClick = {
                        when {
                            state.items.isEmpty() ->
                                Toast.makeText(context, "Ajoute au moins un article au panier", Toast.LENGTH_SHORT).show()
                            state.isCredit && state.selectedClient == null -> {
                                creditError = true
                                showClientPicker = true
                            }
                            state.isCredit -> scope.launch {
                                viewModel.encaisser(0)
                                viewModel.clearCart()
                            }
                            else -> {
                                amountText = ""
                                showEncaisseSheet = true
                            }
                        }
                    },
```

par :

```kotlin
                    onClick = {
                        when {
                            state.items.isEmpty() ->
                                Toast.makeText(context, "Ajoute au moins un article au panier", Toast.LENGTH_SHORT).show()
                            state.isCredit && state.selectedClient == null -> {
                                creditError = true
                                showClientPicker = true
                            }
                            state.isCredit -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    viewModel.encaisser(0)
                                    viewModel.clearCart()
                                }
                            }
                            else -> {
                                amountText = ""
                                showEncaisseSheet = true
                            }
                        }
                    },
```

Puis, dans l'appel à `EncaisseSheet`, remplacer :

```kotlin
            onValidate = {
                scope.launch {
                    viewModel.encaisser(paid)
                    viewModel.clearCart()
                }
                amountText = ""
                showEncaisseSheet = false
            },
```

par :

```kotlin
            onValidate = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    viewModel.encaisser(paid)
                    viewModel.clearCart()
                }
                amountText = ""
                showEncaisseSheet = false
            },
```

- [ ] **Step 6: Animation d'entrée des articles du panier**

Remplacer :

```kotlin
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(state.items) { index, item ->
                            CartItemRow(
                                item = item,
                                onDecrease = {
                                    if (item.quantity > 1) viewModel.updateQuantity(index, item.quantity - 1)
                                    else viewModel.removeItem(index)
                                },
                                onIncrease = { viewModel.updateQuantity(index, item.quantity + 1) }
                            )
                            if (index < state.items.size - 1) {
                                HorizontalDivider(
                                    color = Border,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
```

par :

```kotlin
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(
                            items = state.items,
                            key = { _, item -> item.barcode }
                        ) { index, item ->
                            CartItemRow(
                                item = item,
                                onDecrease = {
                                    if (item.quantity > 1) viewModel.updateQuantity(index, item.quantity - 1)
                                    else viewModel.removeItem(index)
                                },
                                onIncrease = { viewModel.updateQuantity(index, item.quantity + 1) },
                                modifier = Modifier.animateItem()
                            )
                            if (index < state.items.size - 1) {
                                HorizontalDivider(
                                    color = Border,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
```

- [ ] **Step 7: `CartItemRow` accepte un `modifier`**

Remplacer :

```kotlin
@Composable
private fun CartItemRow(
    item: CartItem,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
```

par :

```kotlin
@Composable
private fun CartItemRow(
    item: CartItem,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
```

- [ ] **Step 8: Compiler**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/screen/CaisseScreen.kt
git commit -m "feat(caisse): pastille du jour, retour haptique et animation du panier"
```

---

## Task 11: Build complet et vérification manuelle

**Files:** Aucun changement de code — vérification finale uniquement.

- [ ] **Step 1: Build debug complet**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Installer sur un appareil/émulateur connecté**

Run: `./gradlew installDebug`
Expected: `BUILD SUCCESSFUL`, l'app s'installe sans erreur.

- [ ] **Step 3: Vérification manuelle — flux caisse**

Sur l'appareil : ouvrir Caisse → vérifier que la pastille "Aujourd'hui · …" n'apparaît PAS si aucune vente n'a encore eu lieu aujourd'hui. Scanner/rechercher un produit → sentir la vibration à l'ajout, vérifier l'animation d'entrée de la ligne dans le panier. Utiliser le stepper +/- → sentir la vibration à chaque tap. Encaisser une vente comptant → sentir la vibration à la validation, vérifier l'animation de pulsation sur l'icône de succès du reçu. Revenir à la Caisse → la pastille "Aujourd'hui · …" doit maintenant afficher le nouveau total et être cliquable vers Activité.

- [ ] **Step 4: Vérification manuelle — écran Activité**

Naviguer vers Activité. Vérifier sur chaque période (Aujourd'hui/Semaine/Mois) : les `TrendBadge` s'affichent sur Comptant/À crédit/Marge estimée quand une période précédente a des données (sinon absents, pas de crash). La section "Heures de pointe" apparaît sur Aujourd'hui/Semaine (pas sur Mois) avec au moins une barre non vide. La bannière "Stock bas" apparaît en haut si un produit a `stock <= min_stock` (vérifiable en éditant un produit depuis Produits pour forcer un stock bas), et son bouton "Voir les produits" navigue bien vers l'écran Produits. Le texte "Meilleur jour" apparaît sous le chiffre d'affaires quand la série a au moins 2 jours de données.

- [ ] **Step 5: Commit final si des ajustements manuels ont été nécessaires**

Si la vérification manuelle a révélé des corrections mineures, les committer normalement (une par correctif, message conventional-commit).
