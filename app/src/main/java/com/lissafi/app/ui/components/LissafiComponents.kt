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
    leadingIcon: ImageVector? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
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
