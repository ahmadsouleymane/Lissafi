package com.lissafi.app.ui.components

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.data.sync.SyncStatus
import com.lissafi.app.data.entity.Product
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.theme.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*

// ============================================================
// ICÔNES — Mapping Lucide
// ============================================================
object LissafiIcons {
    // Navigation
    val Caisse     = Lucide.Store
    val Produits   = Lucide.Package
    val Clients    = Lucide.Users
    val Activite   = Lucide.ChartColumnBig
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
    val Email      = Lucide.Mail
    val Motdepasse = Lucide.Lock

    // Statut
    val Sync       = Lucide.Cloud
    val SyncOk     = Lucide.CircleCheck
    val SyncErr    = Lucide.CloudOff
    val Alerte     = Lucide.TriangleAlert
    val Succes     = Lucide.CircleCheck
    val Erreur     = Lucide.CircleAlert
    val Recents    = Lucide.Clock
    val Info       = Lucide.Info
    val Version    = Lucide.FileText
}

// ============================================================
// CARTE — Flottante avec ombre douce, coins 20dp
// ============================================================
@Composable
fun LissafiCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 20,
    onClick: (() -> Unit)? = null,
    borderColor: Color = Color.Transparent,
    containerColor: Color = Surface,
    elevation: Int = 4,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (elevation > 0) Modifier.shadow(
                    elevation = elevation.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.04f),
                    spotColor = Color.Black.copy(alpha = 0.04f)
                ) else Modifier
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (borderColor != Color.Transparent) BorderStroke(1.dp, borderColor) else null
    ) { content() }
}

// ============================================================
// CARTE SANS OMBRE — Pour conteneurs internes
// ============================================================
@Composable
fun LissafiCardFlat(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 16,
    onClick: (() -> Unit)? = null,
    containerColor: Color = SurfaceAlt,
    content: @Composable ColumnScope.() -> Unit
) {
    LissafiCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        onClick = onClick,
        containerColor = containerColor,
        elevation = 0,
        content = content
    )
}

// ============================================================
// EN-TÊTE — Minimal, fond transparent
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LissafiHeader(
    title: String,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    titleLogo: (@Composable () -> Unit)? = null,
    titleFontSize: TextUnit = 20.sp,
    titleFontWeight: FontWeight = FontWeight.SemiBold
) {
    TopAppBar(
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    titleLogo?.let { it() }
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = OnBackground,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = title,
                        color = OnBackground,
                        fontWeight = titleFontWeight,
                        fontSize = titleFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
// TITRE DE SECTION — Subtil
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
            fontWeight = FontWeight.SemiBold,
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
// ÉTAT VIDE — Icone subtile, texte chaleureux
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
                .size(80.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary.copy(alpha = 0.4f),
                modifier = Modifier.size(36.dp)
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
        Spacer(Modifier.height(6.dp))
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
// CHAMP DE RECHERCHE — Fond gris clair, sans bordure, coins 12dp
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
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = SurfaceAlt,
            unfocusedContainerColor = SurfaceAlt,
            cursorColor = Primary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// ============================================================
// CHAMP TEXTE STYLE CAPSULE — Fond gris, sans bordure
// ============================================================
@Composable
fun CapsuleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1
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
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = SurfaceAlt,
            unfocusedContainerColor = SurfaceAlt,
            cursorColor = Primary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// ============================================================
// BOUTON PRINCIPAL — Pleine largeur, 56dp, scale press, pill
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
        shape = RoundedCornerShape(16.dp),
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = OnPrimary,
            disabledContainerColor = Color(0xFF000000).copy(alpha = 0.06f),
            disabledContentColor = Color(0xFF000000).copy(alpha = 0.30f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 0.dp
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
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}

// ============================================================
// BOUTON SECONDAIRE — Contour primary, pill
// ============================================================
@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Int = 52
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        shape = RoundedCornerShape(16.dp),
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
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
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
// ASTUCE — Message informatif sur fond teinté
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
            .clip(RoundedCornerShape(12.dp))
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
// BADGE — Pastille colorée avec point
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
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(10.dp),
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
        shape = RoundedCornerShape(24.dp),
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
                shape = RoundedCornerShape(14.dp),
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
// DIALOGUE LIMITE PREMIUM
// ============================================================
@Composable
fun PremiumLimitDialog(
    message: String,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = LissafiIcons.Boutique,
                contentDescription = null,
                tint = Secondary,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Column {
                Text(text = "Limite atteinte", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(text = message, fontSize = 12.sp, color = TextSecondary)
            }
        },
        confirmButton = {
            Button(
                onClick = onUpgrade,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Secondary)
            ) {
                Text("Voir Premium", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Plus tard", color = TextSecondary, fontSize = 14.sp)
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
            tint = Success,
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
            .padding(vertical = 6.dp),
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontWeight = valueWeight,
                fontSize = 14.sp,
                color = valueColor
            )
            // Indicateur de statut si erreur
            if (value.contains("Erreur")) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Secondary)
                )
            }
        }
    }
}

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

// ============================================================
// SEGMENTED CONTROL — Style iOS, fond gris, sélection blanche
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
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceAlt)
            .padding(3.dp)
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) Surface else Color.Transparent
                    )
                    .then(
                        if (isSelected) Modifier.shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(10.dp),
                            ambientColor = Color.Black.copy(alpha = 0.06f),
                            spotColor = Color.Black.copy(alpha = 0.06f)
                        ) else Modifier
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) OnBackground else TextSecondary
                )
            }
        }
    }
}

// ============================================================
// PASTILLE ICÔNE — Cercle coloré pour listes et cartes
// ============================================================
@Composable
fun IconCircle(
    icon: ImageVector,
    backgroundColor: Color = Primary.copy(alpha = 0.08f),
    iconTint: Color = Primary,
    size: Int = 44,
    iconSize: Int = 22
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}
