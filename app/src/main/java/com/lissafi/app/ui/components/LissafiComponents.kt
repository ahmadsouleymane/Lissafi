package com.lissafi.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.data.sync.SyncStatus
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.theme.Green800
import com.lissafi.app.ui.theme.LissafiGreen
import com.lissafi.app.ui.theme.LissafiOrange
import com.lissafi.app.ui.theme.LissafiWhite
import com.lissafi.app.ui.theme.Neutral200
import com.lissafi.app.ui.theme.Neutral400
import com.lissafi.app.ui.theme.Neutral500
import com.lissafi.app.ui.theme.White
import com.lissafi.app.ui.theme.Warning

// ============================================================
// ESPACEMENT STANDARD — grille 8dp
// ============================================================
object LissafiSpacing {
    val XS = 4.dp
    val SM = 8.dp
    val MD = 12.dp
    val LG = 16.dp
    val XL = 24.dp
    val XXL = 32.dp
    val screen = 16.dp
}

/** Carte blanche arrondie avec ombre douce — l'élément de base de toute la liste. */
@Composable
fun LissafiCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 18,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val base = Modifier
        .fillMaxWidth()
        .clip(shape)
    val clickable = if (onClick != null) base.clickable(onClick = onClick) else base
    Card(
        modifier = clickable,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) { content() }
}

// ============================================================
// EN-TÊTE D'ÉCRAN — barre verte de marque
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
                            tint = LissafiWhite,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = title,
                        color = LissafiWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = LissafiWhite.copy(alpha = 0.8f),
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = LissafiWhite
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Green800,
            titleContentColor = LissafiWhite
        )
    )
}

// ============================================================
// TITRE DE SECTION — petite étiquette verte en capitales
// ============================================================
@Composable
fun SectionHeader(
    text: String,
    icon: ImageVector? = null,
    tint: Color = LissafiGreen,
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
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = tint,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp
        )
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            trailing()
        }
    }
}

// ============================================================
// ÉTAT VIDE — explique quoi faire, avec bouton d'action
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
                .size(88.dp)
                .clip(CircleShape)
                .background(LissafiGreen.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LissafiGreen.copy(alpha = 0.7f),
                modifier = Modifier.size(42.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Neutral500,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = Neutral400,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            PrimaryActionButton(
                text = actionLabel,
                icon = Icons.Filled.Add,
                onClick = onAction,
                modifier = Modifier.width(220.dp)
            )
        }
    }
}

// ============================================================
// CHAMP DE RECHERCHE — barre cohérente
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
                fontSize = 15.sp,
                color = Neutral400
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = LissafiGreen,
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LissafiGreen,
            unfocusedBorderColor = Neutral200,
            focusedContainerColor = White,
            unfocusedContainerColor = White,
            cursorColor = LissafiGreen
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// ============================================================
// BOUTON D'ACTION PRINCIPAL — gros bouton vert
// ============================================================
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Int = 54,
    containerColor: Color = LissafiGreen
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
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
            contentColor = LissafiWhite,
            disabledContainerColor = Neutral200,
            disabledContentColor = Neutral500
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

/** Bouton secondaire — contour vert. */
@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Int = 54
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, LissafiGreen),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = LissafiGreen
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
// TEXTE DE MONTANT — grand, gras, cohérent
// ============================================================
@Composable
fun AmountText(
    amount: Int,
    modifier: Modifier = Modifier,
    fontSize: Int = 20,
    color: Color = LissafiGreen,
    fontWeight: FontWeight = FontWeight.Bold
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
// ASTUCE D'AIDE — explique aux utilisateurs non techniques
// ============================================================
@Composable
fun HelpHint(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = LissafiOrange
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
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
// BADGE DE STATUT — pastille colorée
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
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

// ============================================================
// QUANTITÉ +/− (panier)
// ============================================================
@Composable
fun QuantityStepper(
    quantity: Double,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LissafiGreen
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StepperButton(
            icon = Icons.Filled.Remove,
            color = Color(0xFFEF4444),
            onClick = onDecrease,
            size = 40
        )
        Text(
            text = formatQuantity(quantity),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Neutral500,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center
        )
        StepperButton(
            icon = Icons.Filled.Add,
            color = color,
            onClick = onIncrease,
            size = 40
        )
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    size: Int
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size((size * 0.55f).dp)
        )
    }
}

private fun formatQuantity(q: Double): String =
    if (q == q.toInt().toDouble()) q.toInt().toString() else q.toString()

// ============================================================
// MONTANTS RAPIDES — pour l'encaissement / remboursement
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
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LissafiGreen,
                    selectedLabelColor = White,
                    containerColor = White
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ============================================================
// CHAMP DE MONTANT — grand clavier numérique FCFA
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
                    color = LissafiGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LissafiGreen,
                unfocusedBorderColor = Neutral200,
                focusedContainerColor = White,
                unfocusedContainerColor = White,
                cursorColor = LissafiGreen,
                focusedLabelColor = LissafiGreen,
                unfocusedLabelColor = Neutral400
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (hint != null) {
            Text(
                text = hint,
                fontSize = 12.sp,
                color = Neutral400,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

// ============================================================
// DIALOGUE DE CONFIRMATION — cohérent partout
// ============================================================
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    icon: ImageVector = Icons.Filled.Info,
    destructive: Boolean = false,
    iconTint: Color = LissafiGreen
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                color = Neutral500,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (destructive) Color(0xFFEF4444) else LissafiGreen
                )
            ) {
                Text(
                    text = confirmLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Annuler", color = Neutral500, fontSize = 14.sp)
            }
        }
    )
}

// ============================================================
// INDICATEUR DE SYNCHRONISATION — passif, aucune action requise
// ============================================================
@Composable
fun SyncIndicator(
    status: SyncStatus,
    modifier: Modifier = Modifier
) {
    when (status) {
        SyncStatus.SYNCING -> CircularProgressIndicator(
            modifier = modifier.size(18.dp),
            color = LissafiGreen,
            strokeWidth = 2.dp
        )
        SyncStatus.SUCCESS -> Icon(
            imageVector = Icons.Filled.CloudDone,
            contentDescription = "Données à jour",
            tint = LissafiGreen,
            modifier = modifier.size(20.dp)
        )
        SyncStatus.ERROR -> Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = "Synchronisation impossible",
            tint = LissafiOrange,
            modifier = modifier.size(20.dp)
        )
        else -> Icon(
            imageVector = Icons.Filled.Cloud,
            contentDescription = "Synchronisation automatique",
            tint = Neutral400,
            modifier = modifier.size(20.dp)
        )
    }
}

// ============================================================
// LIGNE D'INFO — icône + label + valeur (cartes de stats/params)
// ============================================================
@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    iconTint: Color = LissafiGreen,
    valueColor: Color = Neutral500,
    valueWeight: FontWeight = FontWeight.Bold
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
                color = Neutral500
            )
        }
        Text(
            text = value,
            fontWeight = valueWeight,
            fontSize = 15.sp,
            color = valueColor
        )
    }
}
