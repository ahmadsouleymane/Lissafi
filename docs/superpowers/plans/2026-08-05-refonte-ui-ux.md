# Refonte UI/UX & Copywriting Lissafi — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refonte complète du design system, de la navigation, des 8 écrans et du copywriting — sans toucher aux ViewModels ni à la logique métier.

**Architecture:** Design System d'abord (couleurs, typo Inter, icônes Lucide, espacements) → composants partagés → navigation (HorizontalPager 4 onglets + FAB contextuel) → écrans un par un (Caisse, Activité, Produits, Clients, Réglages, Admin, Auth) → copywriting final.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Lucide Compose (icônes), Vico (graphiques), Inter (police)

## Global Constraints

- **Local-first** : toute écriture passe D'ABORD en SQLite, puis `syncToRemote` en arrière-plan
- **L'argent est en `Int` FCFA** partout, les quantités en `Double`
- **Isolation par utilisateur** : chaque table a `user_id`, `LissafiRepository` porte `currentUserIdProvider`
- **Fusion des ids de vente** : `db.reassignSaleId(localId, remoteId)` à préserver
- **Schéma DB = miroir de `supabase-schema.sql`** : ne pas modifier les tables
- **Premium** : codes statiques `LISSAFI-PREMIUM-XXXX`, démo 7 jours, limites 10 produits / 10 crédits
- Les entités (`data/entity/*.kt`) sont `@Serializable`, noms de champs = colonnes PostgREST
- Les dates sont des `Long` (epoch millis), booléens SQLite = 0/1
- Tout le code et les commits en **français**, style conventional commits
- **NE PAS modifier** : `ui/viewmodel/*`, `data/*`, `service/*`, `MainActivity.kt`, `LissafiApp.kt`
- Compiler avec `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` puis `./gradlew assembleDebug`

---

### Task 1: Design System — Palette et typographie Inter

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/lissafi/app/ui/theme/Theme.kt`
- Create: `app/src/main/res/font/inter_regular.ttf`
- Create: `app/src/main/res/font/inter_medium.ttf`
- Create: `app/src/main/res/font/inter_semibold.ttf`
- Create: `app/src/main/res/font/inter_light.ttf`

**Interfaces:**
- Produces: `LissafiTheme` — nouveau thème Material 3 avec palette fusion moderne-chaud + typographie Inter
- Produces: Tous les tokens de couleur (`Primary`, `Secondary`, `Background`, `Surface`, `SurfaceAlt`, `Border`, `Error`, `TextSecondary`, `TextTertiary`, `ErrorContainer`, `OnPrimary`, `OnSecondary`, `OnBackground`, `OnSurface`, `PrimaryContainer`, `OnPrimaryContainer`, `SecondaryContainer`, `OnSecondaryContainer`)

- [ ] **Step 1: Télécharger Inter depuis Google Fonts**

Télécharger le package Inter (Regular, Medium, SemiBold, Light) depuis https://fonts.google.com/specimen/Inter et placer les 4 fichiers `.ttf` dans `app/src/main/res/font/`.

```bash
# Vérifier que les fichiers sont bien placés :
ls app/src/main/res/font/inter_*.ttf
```

- [ ] **Step 2: Réécrire Color.kt avec la nouvelle palette**

Remplacer le contenu de `app/src/main/java/com/lissafi/app/ui/theme/Color.kt` :

```kotlin
package com.lissafi.app.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// LISSAFI — Palette fusion moderne-chaud
// Vert émeraude profond + terre cuivrée + fond blanc cassé
// Style SaaS premium, inspiration Vercel × Niamey
// ============================================================

// ── Primary — Vert émeraude profond ─────────────────────────
val Primary = Color(0xFF1A7F4F)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFE8F5EE)
val OnPrimaryContainer = Color(0xFF0D3D24)

// ── Secondary — Terre cuivrée ───────────────────────────────
val Secondary = Color(0xFFE8913A)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFFFF3E8)
val OnSecondaryContainer = Color(0xFF5C2D0E)

// ── Background ──────────────────────────────────────────────
val Background = Color(0xFFF9F8F6)
val OnBackground = Color(0xFF1B1B1B)

// ── Surface ─────────────────────────────────────────────────
val Surface = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF1B1B1B)
val SurfaceAlt = Color(0xFFF4F3F0)

// ── Texte ───────────────────────────────────────────────────
val TextSecondary = Color(0xFF6B6B6B)
val TextTertiary = Color(0xFF9E9E9E)

// ── Border ──────────────────────────────────────────────────
val Border = Color(0xFFEBEBEB)

// ── Error ───────────────────────────────────────────────────
val Error = Color(0xFFDC5A5A)
val ErrorContainer = Color(0xFFFEE2E2)

// ── Overlay ─────────────────────────────────────────────────
val Scrim = Color(0x66000000)

// ── Alias pour compatibilité avec code existant (transition) ─
val LissafiGreen = Primary
val LissafiGreenLight = Primary
val LissafiGreenDark = Primary
val LissafiOrange = Secondary
val LissafiOrangeLight = Secondary
val LissafiOrangeDark = Secondary
val LissafiCream = Background
val LissafiWhite = Surface
val LissafiBlack = OnBackground
val LissafiWarning = Secondary
val LissafiDanger = Error
val LissafiSuccess = Primary

// Alias neutres pour la transition
val Neutral50 = Color(0xFFF8F9FA)
val Neutral100 = Color(0xFFF1F3F5)
val Neutral200 = Color(0xFFE8EBEE)
val Neutral300 = Color(0xFFDEE2E6)
val Neutral400 = Color(0xFF6B6B6B)
val Neutral500 = Color(0xFF6B6B6B)
val Neutral600 = Color(0xFF9E9E9E)
val Neutral700 = Color(0xFF495057)
val Neutral800 = Color(0xFF343A40)
val Neutral900 = Color(0xFF212529)

val White = Color(0xFFFFFFFF)
val OffWhite = Color(0xFFFCFCFB)
val Black = Color(0xFF1B1B1B)
val SoftBlack = Color(0xFF2D2D2D)

val Green50 = Color(0xFFE8F5EE)
val Green100 = Color(0xFFC5E6D3)
val Green200 = Color(0xFF9FD4B5)
val Green300 = Color(0xFF79C297)
val Green400 = Color(0xFF5CB580)
val Green500 = Color(0xFF3EA86A)
val Green600 = Primary
val Green700 = Color(0xFF267347)
val Green800 = Color(0xFF1E5C38)
val Green900 = Color(0xFF164529)

val Orange50 = SecondaryContainer
val Orange100 = Color(0xFFFFE0C0)
val Orange200 = Color(0xFFFFCD98)
val Orange300 = Color(0xFFE8913A)
val Orange400 = Secondary
val Orange500 = Color(0xFFE89540)
val Orange600 = Color(0xFFD4893F)
val Orange700 = Color(0xFFB07030)
val Orange800 = Color(0xFF8C5820)
val Orange900 = Color(0xFF684010)

val Cream50 = Color(0xFFFEFDFB)
val Cream100 = Background
val Cream200 = Color(0xFFF5F2EB)
val Cream300 = Color(0xFFEDE8DD)
val Cream400 = Color(0xFFE5DFD0)
val Cream500 = Color(0xFFD8D0BD)

val SurfaceDefault = Surface
val SurfaceDim = SurfaceAlt
val SurfaceContainer = SurfaceAlt
val SurfaceContainerLowest = Surface
val SurfaceContainerLow = OffWhite
val SurfaceContainerHigh = Color(0xFFF5F2EB)
val SurfaceContainerHighest = Color(0xFFEDE8DD)

val Success = Primary
val SuccessLight = PrimaryContainer
val SuccessDark = Color(0xFF0D3D24)
val Warning = Secondary
val WarningLight = SecondaryContainer
val WarningDark = Color(0xFF5C2D0E)
val Danger = Error
val DangerLight = ErrorContainer
val DangerDark = Error
val Info = Color(0xFF3B82F6)
val InfoLight = Color(0xFFDBEAFE)

val OverlayLight = Color(0x33000000)
val OverlayMedium = Scrim
val OverlayDark = Color(0x99000000)
```

- [ ] **Step 3: Réécrire Theme.kt avec Inter et la nouvelle palette**

Remplacer le contenu de `app/src/main/java/com/lissafi/app/ui/theme/Theme.kt` :

```kotlin
package com.lissafi.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.lissafi.app.R

// ============================================================
// TYPOGRAPHIE — Inter (Vercel, GitHub, Figma)
// ============================================================

val InterFont = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_semibold, FontWeight.Bold)  // fallback Bold → SemiBold
)

val LissafiTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Light,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)

// ============================================================
// FORMES — Plus resserrées, style SaaS
// ============================================================
val LissafiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// ============================================================
// COLOR SCHEME — Light uniquement
// ============================================================
private val LissafiLightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceAlt,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Surface,
    surfaceContainerLow = OffWhite,
    surfaceContainer = SurfaceAlt,
    surfaceContainerHigh = Color(0xFFF5F2EB),
    surfaceContainerHighest = Color(0xFFEDE8DD),
    outline = Border,
    outlineVariant = Border,
    error = Error,
    onError = OnPrimary,
    errorContainer = ErrorContainer,
    onErrorContainer = Error,
    inverseSurface = Color(0xFF1B1B1B),
    inverseOnSurface = OnPrimary,
    inversePrimary = Primary,
    scrim = Scrim
)

// ============================================================
// THÈME PRINCIPAL
// ============================================================
@Composable
fun LissafiTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LissafiLightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Barre de statut assortie au fond (clair)
            window.statusBarColor = Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LissafiTypography,
        shapes = LissafiShapes,
        content = content
    )
}
```

- [ ] **Step 4: Build de vérification**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/theme/Color.kt \
        app/src/main/java/com/lissafi/app/ui/theme/Theme.kt \
        app/src/main/res/font/
git commit -m "feat(ui): nouvelle palette et typographie Inter"
```

---

### Task 2: Dépendances Lucide + Vico

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces: `icons-lucide-android` disponible — icônes via `composables.icons.Lucide.X` (ImageVector)
- Produces: `vico:compose-m3` disponible — graphiques dans l'écran Activité

- [ ] **Step 1: Ajouter les dépendances dans le version catalog**

Le projet utilise `gradle/libs.versions.toml`. Ajouter dans `[versions]` :

```toml
lucideIcons = "1.1.0"
vico = "2.1.3"
```

Dans `[libraries]` :

```toml
composables-lucide = { group = "com.composables", name = "icons-lucide-android", version.ref = "lucideIcons" }
vico-compose-m3 = { group = "com.patrykandpatrick.vico", name = "compose-m3", version.ref = "vico" }
```

Puis dans le bloc `dependencies` de `app/build.gradle.kts` :

```kotlin
// Icônes Lucide (style Vercel/Linear) — ImageVectors via composables.icons.Lucide
implementation(libs.composables.lucide)

// Graphiques Vico
implementation(libs.vico.compose.m3)
```

- [ ] **Step 2: Build de vérification (téléchargement des dépendances)**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "build: ajout dépendances Lucide (icônes) et Vico (graphiques)"
```

---

### Task 3: Composants partagés — refonte LissafiComponents.kt

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/components/LissafiComponents.kt`

**Interfaces:**
- Consumes: `LissafiTheme` (Task 1), `lucide-compose` (Task 2)
- Produces: `LissafiCard`, `LissafiHeader`, `SectionHeader`, `EmptyState`, `SearchField`, `PrimaryActionButton`, `SecondaryActionButton`, `AmountText`, `HelpHint`, `StatusBadge`, `QuantityStepper`, `QuickAmountChips`, `AmountField`, `ConfirmDialog`, `SyncIndicator`, `InfoRow`, `LissafiSpacing`
- Produces: `LissafiIcons` — objet avec toutes les icônes Lucide mappées

- [ ] **Step 1: Réécrire LissafiComponents.kt**

Remplacer le contenu de `app/src/main/java/com/lissafi/app/ui/components/LissafiComponents.kt` par la version refondue ci-dessous.

```kotlin
package com.lissafi.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.data.sync.SyncStatus
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.theme.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*

// ============================================================
// ESPACEMENT — Grille 4dp
// ============================================================
object LissafiSpacing {
    val XXS = 2.dp
    val XS = 4.dp
    val SM = 8.dp
    val MD = 12.dp
    val LG = 16.dp
    val XL = 20.dp
    val XXL = 24.dp
    val XXXL = 32.dp
    val screen = 16.dp
}

// ============================================================
// ICÔNES — Mapping Lucide
// API : com.composables.icons.lucide.Lucide — chaque icône est une
// propriété d'extension `val Lucide.Store: ImageVector`.
// Import requis : `import com.composables.icons.lucide.Lucide`
// + `import com.composables.icons.lucide.*` (ou par icône).
// ============================================================
object LissafiIcons {
    // Navigation
    val Caisse     = Lucide.Store
    val Produits   = Lucide.Package
    val Clients    = Lucide.Users
    val Activite   = Lucide.ChartColumnBig   // ex BarChart3 (renommée)
    val Reglages   = Lucide.Settings2
    val Retour     = Lucide.ArrowLeft

    // Actions
    val Scanner    = Lucide.Scan
    val Ajouter    = Lucide.Plus
    val Rechercher = Lucide.Search
    val Fermer     = Lucide.X
    val Valider    = Lucide.Check
    val Modifier   = Lucide.Pencil
    val Supprimer  = Lucide.Trash2
    val Partager   = Lucide.Share2
    val Imprimer   = Lucide.Printer

    // Finance
    val Encaisser  = Lucide.Banknote
    val Credit     = Lucide.CreditCard
    val Rembourser = Lucide.Undo2
    val Marge      = Lucide.DollarSign
    val Panier     = Lucide.ShoppingCart

    // Entités
    val Produit    = Lucide.Package
    val Client     = Lucide.User
    val Boutique   = Lucide.Building2
    val Telephone  = Lucide.Phone
    val Email      = Lucide.Mail
    val Motdepasse = Lucide.Lock
    val Logout     = Lucide.LogOut

    // Statut
    val Sync       = Lucide.Cloud
    val SyncOk     = Lucide.CircleCheck     // ex CloudCheck
    val SyncErr    = Lucide.CloudOff        // ex CloudAlert
    val Alerte     = Lucide.TriangleAlert   // ex AlertTriangle
    val Succes     = Lucide.CircleCheck     // ex CheckCircle2
    val Erreur     = Lucide.CircleAlert     // ex AlertCircle
    val Tendance   = Lucide.TrendingUp
    val Baisse     = Lucide.TrendingDown
    val Recents    = Lucide.Clock
    val Info       = Lucide.Info
    val Version    = Lucide.FileText
    val Conditions = Lucide.ScrollText
}

// ============================================================
// CARTE — Bordée, sans ombre par défaut
// ============================================================
@Composable
fun LissafiCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 16,
    onClick: (() -> Unit)? = null,
    borderColor: Color = Border,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, borderColor)
    ) { content() }
}

// ============================================================
// EN-TÊTE — Minimal, fond transparent
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LissafiHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    color = OnBackground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = LissafiIcons.Retour,
                        contentDescription = "Retour",
                        tint = OnBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = OnBackground
        )
    )
}

// ============================================================
// TITRE DE SECTION — Subtil, en minuscules
// ============================================================
@Composable
fun SectionHeader(
    text: String,
    icon: ImageVector? = null,
    tint: Color = TextSecondary,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = tint,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            trailing()
        }
    }
}

// ============================================================
// ÉTAT VIDE — Icône subtile, texte chaleureux
// ============================================================
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = OnBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            PrimaryActionButton(
                text = actionLabel,
                icon = LissafiIcons.Ajouter,
                onClick = onAction
            )
        }
    }
}

// ============================================================
// CHAMP DE RECHERCHE — Fond SurfaceAlt, coins 12dp
// ============================================================
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 14.sp,
                color = TextTertiary
            )
        },
        leadingIcon = {
            Icon(
                imageVector = LissafiIcons.Rechercher,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = SurfaceAlt,
            unfocusedContainerColor = SurfaceAlt,
            cursorColor = Primary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// ============================================================
// BOUTON PRINCIPAL — Pleine largeur, 56dp, scale press
// ============================================================
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Int = 56,
    containerColor: Color = Primary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.97f else 1f,
        spring(dampingRatio = 0.5f),
        label = "scale"
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .scale(scale),
        shape = RoundedCornerShape(14.dp),
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = OnPrimary,
            disabledContainerColor = Color(0xFF000000).copy(alpha = 0.06f),
            disabledContentColor = Color(0xFF000000).copy(alpha = 0.30f)
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp
        )
    }
}

// ============================================================
// BOUTON SECONDAIRE — Contour primary
// ============================================================
@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Int = 56
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, Primary),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Primary
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp
        )
    }
}

// ============================================================
// TEXTE DE MONTANT — Cohérent partout
// ============================================================
@Composable
fun AmountText(
    amount: Int,
    modifier: Modifier = Modifier,
    fontSize: Int = 18,
    color: Color = OnBackground,
    fontWeight: FontWeight = FontWeight.SemiBold
) {
    Text(
        text = FormatUtils.formatFCFA(amount),
        modifier = modifier,
        color = color,
        fontWeight = fontWeight,
        fontSize = fontSize.sp,
        maxLines = 1
    )
}

// ============================================================
// ASTUCE — Message informatif
// ============================================================
@Composable
fun HelpHint(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = Secondary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = LissafiIcons.Info,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = tint,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============================================================
// BADGE — Pastille colorée
// ============================================================
@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

// ============================================================
// STEPPER — Horizontal compact
// ============================================================
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
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF000000).copy(alpha = 0.04f))
                .clickable(onClick = onDecrease),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = LissafiIcons.Fermer,  // X pour −, plus fin que Remove
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
                .clip(RoundedCornerShape(8.dp))
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

private fun formatQuantity(q: Double): String =
    if (q == q.toInt().toDouble()) q.toInt().toString() else q.toString()

// ============================================================
// CHIPS MONTANTS RAPIDES — Paiement
// ============================================================
@Composable
fun QuickAmountChips(
    amounts: List<Int>,
    current: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        amounts.forEach { amt ->
            FilterChip(
                selected = current == amt,
                onClick = { onSelect(amt) },
                label = {
                    Text(
                        text = FormatUtils.formatFCFA(amt),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary,
                    selectedLabelColor = OnPrimary,
                    containerColor = Surface
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ============================================================
// CHAMP DE MONTANT — Saisie FCFA
// ============================================================
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { input -> onValueChange(input.filter { c -> c.isDigit() }) },
            label = { Text(text = label, fontWeight = FontWeight.Medium) },
            leadingIcon = {
                Text(
                    text = "FCFA",
                    color = Primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Border,
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface,
                cursorColor = Primary,
                focusedLabelColor = Primary,
                unfocusedLabelColor = TextSecondary
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (hint != null) {
            Text(
                text = hint,
                fontSize = 12.sp,
                color = TextTertiary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

// ============================================================
// DIALOGUE DE CONFIRMATION — Cohérent
// ============================================================
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    icon: ImageVector = LissafiIcons.Info,
    destructive: Boolean = false,
    iconTint: Color = Primary
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (destructive) Error else Primary
                )
            ) {
                Text(
                    text = confirmLabel,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Annuler", color = TextSecondary, fontSize = 14.sp)
            }
        }
    )
}

// ============================================================
// INDICATEUR DE SYNCHRONISATION
// ============================================================
@Composable
fun SyncIndicator(
    status: SyncStatus,
    modifier: Modifier = Modifier
) {
    when (status) {
        SyncStatus.SYNCING -> CircularProgressIndicator(
            modifier = modifier.size(16.dp),
            color = Primary,
            strokeWidth = 2.dp
        )
        SyncStatus.SUCCESS -> Icon(
            imageVector = LissafiIcons.SyncOk,
            contentDescription = "Données à jour",
            tint = Primary,
            modifier = modifier.size(18.dp)
        )
        SyncStatus.ERROR -> Icon(
            imageVector = LissafiIcons.SyncErr,
            contentDescription = "Synchronisation impossible",
            tint = Secondary,
            modifier = modifier.size(18.dp)
        )
        else -> Icon(
            imageVector = LissafiIcons.Sync,
            contentDescription = "Synchronisation automatique",
            tint = TextTertiary,
            modifier = modifier.size(18.dp)
        )
    }
}

// ============================================================
// LIGNE D'INFO — Icône + label + valeur
// ============================================================
@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    iconTint: Color = Primary,
    valueColor: Color = OnBackground,
    valueWeight: FontWeight = FontWeight.Medium
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                color = OnBackground
            )
        }
        Text(
            text = value,
            fontWeight = valueWeight,
            fontSize = 14.sp,
            color = valueColor
        )
    }
}

// ============================================================
// SEGMENTED CONTROL — Style iOS
// ============================================================
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceAlt)
            .padding(2.dp)
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) Surface else Color.Transparent
                    )
                    .then(
                        if (isSelected) Modifier.shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(8.dp),
                            ambientColor = Color.Black.copy(alpha = 0.06f),
                            spotColor = Color.Black.copy(alpha = 0.06f)
                        ) else Modifier
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) OnBackground else TextSecondary
                )
            }
        }
    }
}
```

- [ ] **Step 2: Build de vérification**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/components/LissafiComponents.kt
git commit -m "feat(ui): refonte des composants partagés avec icônes Lucide"
```

---

### Task 4: Navigation — HorizontalPager + 4 onglets + FAB contextuel

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/navigation/LissafiNavHost.kt`

**Interfaces:**
- Consumes: `LissafiComponents` (Task 3), `LissafiTheme` (Task 1)
- Produces: `Routes` — routes de navigation (inchangées, plus `ACTIVITY` au lieu de `REPORTS`)
- Produces: `LissafiNavHost` — nouveau scaffold avec HorizontalPager, bottom bar 4 onglets, FAB contextuel

- [ ] **Step 1: Réécrire LissafiNavHost.kt avec HorizontalPager et navigation repensée**

Remplacer le contenu de `app/src/main/java/com/lissafi/app/ui/navigation/LissafiNavHost.kt` :

```kotlin
package com.lissafi.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lissafi.app.LissafiApp
import com.lissafi.app.data.auth.AuthManager
import com.lissafi.app.data.repository.LissafiRepository
import com.lissafi.app.service.PremiumManager
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.screen.*
import com.lissafi.app.ui.theme.*
import com.lissafi.app.ui.viewmodel.*
import kotlinx.coroutines.launch

object Routes {
    const val AUTH          = "auth"
    const val CAISSE        = "caisse"
    const val PRODUCTS      = "products"
    const val CLIENTS       = "clients"
    const val CLIENT_DETAIL = "client_detail/{clientId}"
    const val ACTIVITY      = "activity"
    const val SETTINGS      = "settings"
    const val ADMIN         = "admin"

    fun clientDetail(id: String) = "client_detail/$id"
}

private val bottomNavItems = listOf(
    BottomNavItem(Routes.CAISSE,   "Caisse",   LissafiIcons.Caisse),
    BottomNavItem(Routes.PRODUCTS, "Produits",  LissafiIcons.Produits),
    BottomNavItem(Routes.CLIENTS,  "Clients",   LissafiIcons.Clients),
    BottomNavItem(Routes.ACTIVITY, "Activité",  LissafiIcons.Activite)
)

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LissafiNavHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app     = remember { context.applicationContext as LissafiApp }
    val db      = remember { app.database }
    val api     = remember { app.supabaseApi }

    val authManager    = remember { AuthManager(context) }
    val authViewModel  = remember { AuthViewModel(authManager) }
    val authState      by authViewModel.state.collectAsState()

    val isLoggedIn = authState.isLoggedIn || app.authManager.isLoggedIn()
    val userId = app.authManager.currentUserId() ?: ""
    val repository = remember(userId) {
        LissafiRepository(db, api).withUserId(userId)
    }
    val premiumManager = remember { PremiumManager(repository) }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Routes principales (4 onglets)
    val mainRoutes = setOf(Routes.CAISSE, Routes.PRODUCTS, Routes.CLIENTS, Routes.ACTIVITY)
    val showBottomBar = currentRoute in mainRoutes

    // Sync
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            app.syncManager.syncInBackground()
            navController.navigate(Routes.CAISSE) {
                popUpTo(Routes.AUTH) { inclusive = true }
            }
        } else {
            navController.navigate(Routes.AUTH) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val syncStatus by app.syncManager.status.collectAsState()

    // ViewModels
    val cartViewModel: CartViewModel = remember { CartViewModel(repository, premiumManager) }
    val productViewModel: ProductViewModel = remember { ProductViewModel(repository, premiumManager) }
    val clientViewModel: ClientViewModel = remember { ClientViewModel(repository, premiumManager) }
    val reportViewModel: ReportViewModel = remember { ReportViewModel(repository) }
    val settingsViewModel: SettingsViewModel = remember { SettingsViewModel(repository) }

    // Pager state pour HorizontalPager
    val pagerState = rememberPagerState(pageCount = { bottomNavItems.size })
    val scope = rememberCoroutineScope()

    // Synchroniser le pager avec la navigation
    LaunchedEffect(currentRoute) {
        val index = bottomNavItems.indexOfFirst { it.route == currentRoute }
        if (index >= 0 && index != pagerState.currentPage) {
            pagerState.animateScrollToPage(index)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Background,
        contentWindowInsets = WindowInsets.systemBars
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar && isLoggedIn,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                NavigationBar(
                    containerColor = Surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .border(
                            width = 0.5.dp,
                            color = Border
                        )
                ) {
                    bottomNavItems.forEachIndexed { index, item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Primary,
                                selectedTextColor = Primary,
                                indicatorColor = Primary.copy(alpha = 0.10f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) Routes.CAISSE else Routes.AUTH,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Routes.AUTH) {
                AuthScreen(viewModel = authViewModel)
            }

            composable(Routes.CAISSE) {
                CaisseScreen(
                    viewModel = cartViewModel,
                    syncStatus = syncStatus,
                    onNavigateToProducts = { navController.navigate(Routes.PRODUCTS) },
                    onNavigateToClients  = { navController.navigate(Routes.CLIENTS) },
                    onNavigateToReports  = { navController.navigate(Routes.ACTIVITY) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.PRODUCTS) {
                ProductsScreen(
                    viewModel = productViewModel,
                    premiumManager = premiumManager,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.CLIENTS) {
                ClientsScreen(
                    viewModel = clientViewModel,
                    onClientClick = { clientId -> navController.navigate(Routes.clientDetail(clientId)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.CLIENT_DETAIL,
                arguments = listOf(navArgument("clientId") { type = NavType.StringType })
            ) { backStackEntry ->
                val clientId = backStackEntry.arguments?.getString("clientId") ?: return@composable
                ClientDetailScreen(
                    clientId = clientId,
                    viewModel = clientViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.ACTIVITY) {
                ReportsScreen(  // sera renommé en ActivityScreen dans la Task 6
                    viewModel = reportViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    authManager = authManager,
                    onBack = { navController.popBackStack() },
                    onNavigateToAdmin = { navController.navigate(Routes.ADMIN) },
                    onSignOut = {}
                )
            }
            composable(Routes.ADMIN) {
                AdminScreen(
                    viewModel = settingsViewModel,
                    premiumManager = premiumManager,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

```

**Note :** Pour la bordure en haut de la `NavigationBar`, Material 3 trace déjà une ligne de séparation subtile avec `tonalElevation = 0.dp`. Ne pas ajouter de helper `.border`.

- [ ] **Step 2: Build de vérification**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/navigation/LissafiNavHost.kt
git commit -m "feat(ui): navigation repensée — 4 onglets, HorizontalPager, FAB contextuel"
```

---

### Task 5: Écran Caisse — redesign complet

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/CaisseScreen.kt`

**Interfaces:**
- Consumes: `LissafiComponents` (Task 3), `LissafiTheme` (Task 1), `CartViewModel` (existant)
- Produces: `CaisseScreen` — écran principal refondu

- [ ] **Step 1: Réécrire CaisseScreen.kt**

Remplacer le contenu de `app/src/main/java/com/lissafi/app/ui/screen/CaisseScreen.kt` par la version refondue. Les changements principaux :
- Header minimal sans fond vert (fond transparent, titre « LISSAFI », sous-titre avec date)
- Barre de recherche persistante avec dropdown inline
- Produits récents en pills horizontaux
- Carte Total bordée (pas ombrée)
- Stepper horizontal compact
- Segmented control Comptant/Crédit
- FAB Scanner
- Tous les dialogues en bottom sheets
- Copywriting refondu

Le code complet fait ~700 lignes. Voici la structure et les sections clés :

```kotlin
package com.lissafi.app.ui.screen

// ... imports (conserver les imports existants + ajouter Lucide)

private val QUICK_CASH = listOf(500, 1000, 2000, 5000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaisseScreen(
    viewModel: CartViewModel,
    syncStatus: SyncStatus = SyncStatus.IDLE,
    onNavigateToProducts: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val lastSale by viewModel.lastSale.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showEncaisseSheet by remember { mutableStateOf(false) }
    var showScannerScreen by remember { mutableStateOf(false) }
    var showReceiptSheet by remember { mutableStateOf(false) }
    var showClientPicker by remember { mutableStateOf(false) }
    var showBluetoothPicker by remember { mutableStateOf(false) }
    var currentReceipt by remember { mutableStateOf<LastSale?>(null) }
    var amountText by remember { mutableStateOf("") }
    var creditError by remember { mutableStateOf(false) }
    var scanFeedback by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchResults by remember { mutableStateOf(false) }

    // Date du jour formatée
    val todayString = remember {
        val sdf = java.text.SimpleDateFormat("EE d MMM", java.util.Locale.FRENCH)
        sdf.format(java.util.Date())
    }

    // Guide premier lancement
    val prefs = remember { context.getSharedPreferences("lissafi_prefs", Context.MODE_PRIVATE) }
    var showHelp by remember {
        mutableStateOf(!prefs.getBoolean("caisse_help_seen", false))
    }

    // Feedback de scan
    LaunchedEffect(scanResult) {
        scanResult?.let {
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

    // Reçu après vente
    LaunchedEffect(lastSale) {
        lastSale?.let { sale ->
            currentReceipt = sale
            showReceiptSheet = true
        }
    }

    val paid = amountText.toIntOrNull() ?: 0
    val realTimeChange = if (paid >= state.total) paid - state.total else 0

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── HEADER ──
            LissafiHeader(
                title = "LISSAFI",
                subtitle = "$todayString · ${if (state.total > 0) "En cours" else "Prêt"}",
                actions = {
                    SyncIndicator(status = syncStatus)
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = LissafiIcons.Reglages,
                            contentDescription = "Réglages",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            // ── AIDE PREMIÈRE UTILISATION ──
            AnimatedVisibility(visible = showHelp) {
                FirstUseHelpCard(onDismiss = {
                    showHelp = false
                    prefs.edit().putBoolean("caisse_help_seen", true).apply()
                })
            }

            // ── RECHERCHE PRODUIT ──
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                SearchField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        showSearchResults = it.isNotBlank()
                    },
                    placeholder = "Chercher un produit..."
                )
                // Dropdown résultats inline
                if (showSearchResults) {
                    ProductSearchDropdown(
                        query = searchQuery,
                        onSelect = { product ->
                            viewModel.scanProduct(product.barcode)
                            searchQuery = ""
                            showSearchResults = false
                        },
                        onDismiss = {
                            showSearchResults = false
                            searchQuery = ""
                        }
                    )
                }
            }

            // ── FEEDBACK SCAN ──
            AnimatedVisibility(scanFeedback != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (scanFeedback?.startsWith("✓") == true || !scanFeedback!!.startsWith("C"))
                                Primary.copy(alpha = 0.08f)
                            else
                                Secondary.copy(alpha = 0.08f)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (scanFeedback?.startsWith("✓") == true || !scanFeedback!!.startsWith("C"))
                            LissafiIcons.Succes else LissafiIcons.Alerte,
                        contentDescription = null,
                        tint = if (scanFeedback?.startsWith("✓") == true || !scanFeedback!!.startsWith("C"))
                            Primary else Secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = scanFeedback ?: "",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (scanFeedback?.startsWith("✓") == true || !scanFeedback!!.startsWith("C"))
                            Primary else Secondary
                    )
                }
            }

            // ── PRODUITS RÉCENTS ──
            if (state.recentProducts.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    SectionHeader(
                        text = "Récents",
                        icon = LissafiIcons.Recents,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.recentProducts) { p ->
                            SpeedProductPill(p) { viewModel.scanProduct(p.barcode) }
                        }
                    }
                }
            }

            // ── TOTAL ──
            LissafiCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                cornerRadius = 16
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total à encaisser",
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (state.items.isEmpty()) "Panier vide" else
                                "${state.items.size} article${if (state.items.size > 1) "s" else ""}",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                    AnimatedContent(targetState = state.total, label = "total") { total ->
                        Text(
                            text = FormatUtils.formatFCFA(total),
                            color = if (total > 0) Primary else TextTertiary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 32.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // ── PANIER ──
            LissafiCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                cornerRadius = 16
            ) {
                if (state.items.isEmpty()) {
                    EmptyState(
                        icon = LissafiIcons.Scanner,
                        title = "Panier vide",
                        message = "Scanne un code-barres ou cherche un produit pour commencer."
                    )
                } else {
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
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── MODE DE PAIEMENT ──
            SegmentedControl(
                options = listOf("Comptant", "Crédit"),
                selectedIndex = if (state.isCredit) 1 else 0,
                onSelect = { index ->
                    viewModel.setCreditMode(index == 1)
                    creditError = false
                    if (index == 1 && state.selectedClient == null) showClientPicker = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Client sélectionné pour le crédit
            AnimatedVisibility(state.isCredit && state.selectedClient != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Secondary.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = LissafiIcons.Client,
                        contentDescription = null,
                        tint = Secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Crédit · ${state.selectedClient?.name ?: ""}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Secondary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { showClientPicker = true },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Changer", fontSize = 12.sp, color = Secondary)
                    }
                }
            }
            AnimatedVisibility(creditError) {
                Text(
                    text = "Choisis un client pour le crédit",
                    color = Error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )
            }

            // ── BOUTON ENCAISSER ──
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                PrimaryActionButton(
                    text = if (state.isCredit)
                        "Enregistrer · ${FormatUtils.formatFCFA(state.total)}"
                    else
                        "Encaisser · ${FormatUtils.formatFCFA(state.total)}",
                    icon = if (state.isCredit) LissafiIcons.Credit else LissafiIcons.Encaisser,
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
                    enabled = state.total > 0
                )
            }
        }

        // ── FAB SCANNER ──
        FloatingActionButton(
            onClick = { showScannerScreen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp)
                .size(56.dp),
            shape = CircleShape,
            containerColor = Primary,
            contentColor = OnPrimary
        ) {
            Icon(
                imageVector = LissafiIcons.Scanner,
                contentDescription = "Scanner",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // ── BOTTOM SHEETS ──
    if (showClientPicker) {
        ClientPickerSheet(
            viewModel = viewModel,
            onDismiss = { showClientPicker = false },
            onClientSelected = {
                viewModel.selectClient(it)
                creditError = false
                showClientPicker = false
            }
        )
    }
    if (showEncaisseSheet) {
        EncaisseSheet(
            total = state.total,
            amountText = amountText,
            onAmountChange = { amountText = it },
            paid = paid,
            change = realTimeChange,
            onValidate = {
                scope.launch {
                    viewModel.encaisser(paid)
                    viewModel.clearCart()
                }
                amountText = ""
                showEncaisseSheet = false
            },
            onDismiss = {
                showEncaisseSheet = false
                amountText = ""
            }
        )
    }
    if (showScannerScreen) {
        BarcodeScannerScreen(
            onBarcodeScanned = {
                viewModel.scanProduct(it)
                showScannerScreen = false
            },
            onDismiss = { showScannerScreen = false }
        )
    }
    if (showReceiptSheet && currentReceipt != null) {
        ReceiptSheet(
            sale = currentReceipt!!,
            onImprimer = {
                val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                if (adapter != null && adapter.isEnabled) {
                    showBluetoothPicker = true
                } else {
                    // btLauncher
                }
            },
            onWhatsApp = {
                val receiptText = buildReceiptText(currentReceipt!!, context)
                ReceiptService.shareViaWhatsApp(context, receiptText)
            },
            onPartager = {
                val receiptText = buildReceiptText(currentReceipt!!, context)
                ReceiptService.shareText(context, receiptText)
            },
            onFermer = {
                showReceiptSheet = false
                viewModel.clearLastSale()
            }
        )
    }
    if (showBluetoothPicker) {
        val printers = remember { ReceiptService.getPairedPrinters() }
        BluetoothPrinterSheet(
            printers = printers,
            receiptText = buildReceiptText(currentReceipt!!, context),
            onDismiss = { showBluetoothPicker = false }
        )
    }
}

// ... (fonctions privées : SpeedProductPill, CartItemRow, ProductSearchDropdown,
//      EncaisseSheet, ReceiptSheet, ClientPickerSheet, BluetoothPrinterSheet,
//      FirstUseHelpCard, HelpStep, buildReceiptText)
```

**Fonctions privées clés :**

```kotlin
@Composable
private fun SpeedProductPill(product: Product, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Surface, RoundedCornerShape(22.dp))
            .border(1.dp, Border, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = LissafiIcons.Produit,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = product.name,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${FormatUtils.formatFCFA(item.price)} / unité",
                fontSize = 12.sp,
                color = TextTertiary
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = FormatUtils.formatFCFA((item.price * item.quantity).toInt()),
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = OnBackground
        )
        Spacer(Modifier.width(12.dp))
        QuantityStepper(
            quantity = item.quantity,
            onDecrease = onDecrease,
            onIncrease = onIncrease
        )
    }
}
```

- [ ] **Step 2: Build de vérification**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/screen/CaisseScreen.kt
git commit -m "feat(ui): refonte écran Caisse — header minimal, search persistante, pills, segmented control"
```

---

### Task 6: Écran Activité — Vico + dashboard analytics

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/ReportsScreen.kt` (renommé conceptuellement en ActivityScreen)
- Modify: `app/src/main/java/com/lissafi/app/ui/navigation/LissafiNavHost.kt` (mettre à jour l'import)

**Interfaces:**
- Consumes: `LissafiComponents` (Task 3), `Vico` (Task 2), `ReportViewModel` (existant)
- Produces: `ReportsScreen` — dashboard avec sparkline, bar charts, KPIs, heatmap horaire

- [ ] **Step 1: Réécrire ReportsScreen.kt avec les graphiques Vico**

Le fichier refondu inclut :
- SegmentedControl période (Aujourd'hui / Cette semaine / Ce mois)
- Carte chiffre d'affaires avec sparkline Vico (`CartesianChartHost` + `LineSpec`)
- Grille KPIs 2×2 (Comptant, Crédit, Marge, Panier moyen)
- Bar chart horizontal top produits (barres proportionnelles)
- Column chart répartition horaire (colonnes Vico fines)
- Stacked bar ventes vs crédits par jour
- Liste crédits en cours avec barres de progression

```kotlin
package com.lissafi.app.ui.screen

// ... imports (Vico: CartesianChartHost, LineSpec, ColumnSpec, etc.)

@Composable
fun ReportsScreen(
    viewModel: ReportViewModel,
    onBack: () -> Unit
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

        // Période
        val periodLabels = listOf("Aujourd'hui", "Cette semaine", "Ce mois")
        val periodIndex = when (state.period) {
            ReportPeriod.TODAY -> 0
            ReportPeriod.WEEK -> 1
            ReportPeriod.MONTH -> 2
        }
        SegmentedControl(
            options = periodLabels,
            selectedIndex = periodIndex,
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
                modifier = Modifier.fillMaxWidth().padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
            }
        } else if (state.nbTransactions == 0) {
            EmptyState(
                icon = LissafiIcons.Activite,
                title = "Aucune vente",
                message = "Lance ta première vente depuis la caisse !",
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                // Carte CA avec sparkline
                item { RevenueCard(state) }

                // Grille KPIs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiTile("Comptant", state.totalComptant, LissafiIcons.Encaisser, Primary,
                            if (state.totalVentes > 0) (state.totalComptant * 100 / state.totalVentes) else 0,
                            Modifier.weight(1f))
                        KpiTile("À crédit", state.totalCredits, LissafiIcons.Credit, Secondary,
                            if (state.totalVentes > 0) (state.totalCredits * 100 / state.totalVentes) else 0,
                            Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiTile("Marge estimée", state.estimatedProfit, LissafiIcons.Marge, Primary,
                            if (state.totalVentes > 0) (state.estimatedProfit * 100 / state.totalVentes) else 0,
                            Modifier.weight(1f))
                        KpiTile("Panier moyen", state.panierMoyen, LissafiIcons.Panier, TextSecondary, null,
                            Modifier.weight(1f))
                    }
                }

                // Top produits (bar chart horizontal)
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

                // Crédits en cours (si disponible via totalCredits > 0)
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
                                    label = "Déjà encaissé",
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

// Fonctions privées : RevenueCard (sparkline Vico LineSpec), KpiTile, TopProductBar
// RevenueCard utilise CartesianChartHost + LineSpec de Vico avec les données existantes
```

**Note :** Les données existantes de `ReportState` suffisent pour la sparkline et les KPIs. Les pourcentages sont calculés côté UI. La heatmap horaire (`HourlyChart`) et la timeline détaillée des crédits nécessiteraient d'étendre le ViewModel — à faire dans une itération suivante.

- [ ] **Step 2: Build de vérification**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/screen/ReportsScreen.kt
git commit -m "feat(ui): refonte écran Activité avec graphiques Vico — sparkline, barres, KPIs"
```

---

### Task 7: Écrans Produits + Clients

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/ProductsScreen.kt`
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/ClientsScreen.kt`
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/ClientDetailScreen.kt`

**Interfaces:**
- Consumes: `LissafiComponents` (Task 3), ViewModels existants
- Produces: Écrans Produits, Clients et ClientDetail refondus

- [ ] **Step 1: Réécrire ProductsScreen.kt**

Appliquer le nouveau design system :
- Header minimal avec titre « Produits » + sous-titre dynamique (nb produits, alertes stock)
- Filter chips : Tous / En stock / Alerte stock
- Liste épurée : icône + nom + prix + stock (avec badge si bas/rupture)
- Swipe actions (éditer, supprimer)
- FAB `Plus` pour ajouter
- Formulaire ajout/modification en push écran

- [ ] **Step 2: Réécrire ClientsScreen.kt**

Appliquer le nouveau design system :
- Header minimal avec titre « Clients » + sous-titre (nb clients, dette totale)
- Filter chips : Tous / Avec dette / Récents
- Liste avec avatar + nom + nb achats + dette
- FAB `UserPlus` pour ajouter
- Formulaire simplifié

- [ ] **Step 3: Réécrire ClientDetailScreen.kt**

Nouveau design :
- Header avec nom du client + ancienneté
- Carte dette proéminente avec barre de progression
- Mini-stats (achats, ancienneté)
- Timeline des transactions
- FAB « Rembourser »

- [ ] **Step 4: Build de vérification**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/screen/ProductsScreen.kt \
        app/src/main/java/com/lissafi/app/ui/screen/ClientsScreen.kt \
        app/src/main/java/com/lissafi/app/ui/screen/ClientDetailScreen.kt
git commit -m "feat(ui): refonte écrans Produits, Clients et Détail client"
```

---

### Task 8: Écrans Réglages + Admin

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/SettingsScreen.kt`
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/AdminScreen.kt`

**Interfaces:**
- Consumes: `LissafiComponents` (Task 3), ViewModels existants
- Produces: Écrans Réglages et Admin refondus

- [ ] **Step 1: Réécrire SettingsScreen.kt**

Design par sections :
- **Boutique** (nom, téléphone)
- **Compte** (email, mot de passe, déconnexion)
- **Lissafi Premium** (statut visuel avec barres de progression, lien upgrade)
- **À propos** (version, conditions)

Chaque section = un groupe de `InfoRow` avec séparateurs fins.

- [ ] **Step 2: Réécrire AdminScreen.kt**

Design :
- Grande carte statut premium avec barres de progression
- Saisie code d'activation style code PIN (5 inputs)
- Carte essai gratuit 7 jours avec icône cadeau
- Section sync manuelle

- [ ] **Step 3: Build de vérification**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/screen/SettingsScreen.kt \
        app/src/main/java/com/lissafi/app/ui/screen/AdminScreen.kt
git commit -m "feat(ui): refonte écrans Réglages et Admin avec sections groupées"
```

---

### Task 9: Écran Auth — branding épuré

**Files:**
- Modify: `app/src/main/java/com/lissafi/app/ui/screen/AuthScreen.kt`

**Interfaces:**
- Consumes: `LissafiComponents` (Task 3), `AuthViewModel` (existant)
- Produces: `AuthScreen` — login/signup refondu

- [ ] **Step 1: Réécrire AuthScreen.kt**

Design épuré centré :
- Logo « ✦ LISSAFI ✦ » + tagline « Ta caisse, simplement »
- Champs plus grands, bien espacés
- Bouton « Se connecter » / « Créer mon compte »
- Bouton démo distinct : « 🎬 Tester avec la démo »
- Lien « Mot de passe oublié ? » → bottom sheet
- Messages d'erreur inline sous les champs (pas de Toast)
- Validation email en temps réel

- [ ] **Step 2: Build de vérification**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lissafi/app/ui/screen/AuthScreen.kt
git commit -m "feat(ui): refonte écran Auth — branding épuré, validation inline"
```

---

### Task 10: Copywriting — passe finale

**Files:**
- Modify: tous les fichiers d'écran modifiés précédemment

**Interfaces:**
- Consumes: tous les écrans refondus (Tasks 5-9)

- [ ] **Step 1: Revue systématique de tous les textes**

Parcourir chaque écran et vérifier chaque chaîne de caractères contre le mapping copywriting du spec (section 5.2) :

| Écran | Vérifications |
|-------|--------------|
| Caisse | header, search, empty, total, modes, button, help, scan, credit, receipt |
| Activité | title, periods, KPIs, sections, empty |
| Produits | filters, empty, add/edit, delete confirm, field labels |
| Clients | filters, empty, add, detail, debt, history, field labels |
| Réglages | sections, labels, version, terms |
| Admin | code, demo, sync |
| Auth | brand, tagline, buttons, fields, errors, forgot, demo |

- [ ] **Step 2: Build de vérification finale**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add -u app/src/main/java/com/lissafi/app/ui/screen/
git commit -m "feat(ui): copywriting — texte concis UI, chaleureux empty states"
```

---

### Task 11: Revue finale et ajustements visuels

**Files:**
- Tous les fichiers UI modifiés

- [ ] **Step 1: Vérifier la cohérence visuelle**

Checklist rapide :
- Tous les écrans utilisent-ils `Background` comme fond ?
- Toutes les cartes utilisent-elles `LissafiCard` (border 1px, pas d'ombre) ?
- Toutes les icônes viennent-elles de `LissafiIcons` (Lucide) ?
- Les espacements respectent-ils la grille 4dp (`LissafiSpacing`) ?
- La typographie Inter est-elle appliquée partout via `MaterialTheme.typography` ?
- Les couleurs sont-elles cohérentes (Primary pour actions/succès, Secondary pour crédit/alertes) ?

- [ ] **Step 2: Dernier build complet**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit final**

```bash
git add -A app/src/main/java/com/lissafi/app/ui/
git commit -m "feat(ui): revue finale et ajustements visuels de la refonte"
```
