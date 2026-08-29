package com.lissafi.app.ui.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.LissafiApp
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.service.PremiumManager
import com.lissafi.app.ui.components.IconCircle
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.components.PrimaryActionButton
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.Border
import com.lissafi.app.ui.theme.Error
import com.lissafi.app.ui.theme.OnBackground
import com.lissafi.app.ui.theme.OnPrimary
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.PrimaryContainer
import com.lissafi.app.ui.theme.Secondary
import com.lissafi.app.ui.theme.Surface
import com.lissafi.app.ui.theme.SurfaceAlt
import com.lissafi.app.ui.theme.TextSecondary

private val WhatsAppGreen = Color(0xFF25D366)

/** Périodes de facturation. */
private enum class Period(val code: String, val label: String, val short: String) {
    MONTHLY("monthly", "Mensuel", "/mois"),
    QUARTERLY("quarterly", "Trimestriel", "/trim."),
    YEARLY("yearly", "Annuel", "/an")
}

private fun plusPrice(p: Period) = when (p) {
    Period.MONTHLY -> PremiumManager.PLUS_MONTHLY
    Period.QUARTERLY -> PremiumManager.PLUS_QUARTERLY
    Period.YEARLY -> PremiumManager.PLUS_YEARLY
}

private fun businessPrice(p: Period) = when (p) {
    Period.MONTHLY -> PremiumManager.BUSINESS_MONTHLY
    Period.QUARTERLY -> PremiumManager.BUSINESS_QUARTERLY
    Period.YEARLY -> PremiumManager.BUSINESS_YEARLY
}

/** Équivalent mensuel affiché sous le prix (argument valeur). */
private fun perMonth(price: Int, p: Period): Int = when (p) {
    Period.MONTHLY -> price
    Period.QUARTERLY -> price / 3
    Period.YEARLY -> price / 12
}

/**
 * Écran paywall — 3 offres (Petite boutique, Commerce/Supermarché, Pack Boutique).
 * Réutilisé en deux modes :
 * - volontaire ([dismissible] = true) : accessible depuis les Réglages, refermable.
 * - bloquant ([locked] = true) : essai terminé, l'app est verrouillée jusqu'à l'abonnement.
 */
@Composable
fun PaywallScreen(
    currentUserEmail: String?,
    dismissible: Boolean,
    trialDaysLeft: Int,
    locked: Boolean,
    onClose: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val app = remember { context.applicationContext as LissafiApp }
    var period by remember { mutableStateOf(Period.YEARLY) }

    LaunchedEffect(Unit) { app.supabaseApi.logEvent("paywall_view", "info", "") }

    fun logClick(plan: String, method: String) {
        app.supabaseApi.logEvent(
            "subscribe_click", "info", "",
            "{\"plan\":\"$plan\",\"period\":\"${period.code}\",\"method\":\"$method\"}"
        )
    }

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(context, "Impossible d'ouvrir le lien.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        LissafiHeader(
            title = if (locked) "Ton essai est terminé" else "Choisis ta formule",
            subtitle = when {
                locked -> "Abonne-toi pour continuer à utiliser Lissafi"
                trialDaysLeft > 0 -> "Il te reste $trialDaysLeft jour${if (trialDaysLeft > 1) "s" else ""} d'essai"
                else -> "Débloque toute la puissance de Lissafi"
            },
            onBack = if (dismissible) onClose else null
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            if (locked) {
                LissafiCard(
                    containerColor = Error.copy(alpha = 0.08f),
                    cornerRadius = 16,
                    elevation = 0
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconCircle(
                            icon = LissafiIcons.Alerte,
                            backgroundColor = Error.copy(alpha = 0.15f),
                            iconTint = Error,
                            size = 40,
                            iconSize = 20
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Tes produits, tes clients et leurs dettes sont bien enregistrés. Abonne-toi pour y accéder à nouveau.",
                            fontSize = 13.sp,
                            color = OnBackground,
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Sélecteur de période ──
            PeriodSelector(selected = period, onSelect = { period = it })

            Spacer(Modifier.height(16.dp))

            // ── Petite boutique (PLUS) ──
            PlanCard(
                title = "Petite boutique",
                subtitle = "Pour la boutique de quartier",
                price = plusPrice(period),
                perMonth = perMonth(plusPrice(period), period),
                period = period,
                highlighted = false,
                features = listOf(
                    "Ventes illimitées",
                    "Jusqu'à ${PremiumManager.MAX_PLUS_PRODUCTS} produits",
                    "Clients & dettes illimités",
                    "Ticket WhatsApp & impression",
                    "Rapports jour / semaine"
                ),
                onPayOnline = {
                    logClick("plus", "card")
                    openUrl(PremiumManager.buildActivationPaymentLink("plus", period.code, currentUserEmail))
                },
                onPayWhatsApp = {
                    logClick("plus", "whatsapp")
                    openUrl(
                        PremiumManager.buildWhatsAppActivationLink(
                            "Petite boutique", period.label, plusPrice(period), currentUserEmail
                        )
                    )
                }
            )

            Spacer(Modifier.height(14.dp))

            // ── Commerce / Supermarché (BUSINESS) ──
            PlanCard(
                title = "Commerce / Supermarché",
                subtitle = "Plusieurs caisses, chiffre global",
                price = businessPrice(period),
                perMonth = perMonth(businessPrice(period), period),
                period = period,
                highlighted = true,
                features = listOf(
                    "Tout illimité",
                    "Multi-caisses (CA global)",
                    "Plusieurs utilisateurs",
                    "Rapports avancés + export CSV",
                    "Support prioritaire"
                ),
                onPayOnline = {
                    logClick("business", "card")
                    openUrl(PremiumManager.buildActivationPaymentLink("business", period.code, currentUserEmail))
                },
                onPayWhatsApp = {
                    logClick("business", "whatsapp")
                    openUrl(
                        PremiumManager.buildWhatsAppActivationLink(
                            "Commerce/Supermarché", period.label, businessPrice(period), currentUserEmail
                        )
                    )
                }
            )

            Spacer(Modifier.height(14.dp))

            // ── Pack Boutique (paiement unique + matériel) ──
            PackCard(
                onOrderWhatsApp = {
                    logClick("pack", "whatsapp")
                    openUrl(
                        PremiumManager.buildWhatsAppActivationLink(
                            "Pack Boutique", "imprimante + 1 an", PremiumManager.PACK_ONE_TIME, currentUserEmail
                        )
                    )
                }
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Paiement par carte, Mobile Money ou espèces (via WhatsApp). Activation sous 24 h.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            if (locked) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Text("Se déconnecter", color = Error, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(selected: Period, onSelect: (Period) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceAlt)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Period.values().forEach { p ->
            val isSel = p == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSel) Surface else Color.Transparent)
                    .clickable { onSelect(p) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = p.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) Primary else TextSecondary
                    )
                    if (p == Period.YEARLY) {
                        Text("−33 %", fontSize = 10.sp, color = if (isSel) Primary else TextSecondary)
                    } else if (p == Period.QUARTERLY) {
                        Text("−17 %", fontSize = 10.sp, color = if (isSel) Primary else TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    subtitle: String,
    price: Int,
    perMonth: Int,
    period: Period,
    highlighted: Boolean,
    features: List<String>,
    onPayOnline: () -> Unit,
    onPayWhatsApp: () -> Unit
) {
    LissafiCard(
        cornerRadius = 20,
        containerColor = if (highlighted) PrimaryContainer else Surface,
        borderColor = if (highlighted) Primary else Border,
        elevation = if (highlighted) 6 else 2
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = OnBackground)
                    Text(subtitle, fontSize = 12.sp, color = TextSecondary)
                }
                if (highlighted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Primary)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("POPULAIRE", color = OnPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = FormatUtils.formatFCFA(price),
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = OnBackground
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = period.short,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            if (period != Period.MONTHLY) {
                Text(
                    text = "soit ${FormatUtils.formatFCFA(perMonth)} / mois",
                    fontSize = 12.sp,
                    color = Primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(14.dp))
            features.forEach { f ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = LissafiIcons.Valider,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(f, fontSize = 13.sp, color = OnBackground)
                }
            }

            Spacer(Modifier.height(16.dp))
            PrimaryActionButton(
                text = "Carte / Mobile Money",
                icon = LissafiIcons.Credit,
                onClick = onPayOnline
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onPayWhatsApp,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, WhatsAppGreen),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreen)
            ) {
                Icon(LissafiIcons.Partager, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Payer via WhatsApp", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PackCard(onOrderWhatsApp: () -> Unit) {
    LissafiCard(cornerRadius = 20, containerColor = SurfaceAlt, elevation = 0) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconCircle(
                    icon = LissafiIcons.Imprimer,
                    backgroundColor = Secondary.copy(alpha = 0.15f),
                    iconTint = Secondary,
                    size = 44,
                    iconSize = 22
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pack Boutique", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = OnBackground)
                    Text("Paiement unique, zéro abonnement", fontSize = 12.sp, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = FormatUtils.formatFCFA(PremiumManager.PACK_ONE_TIME),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = OnBackground
            )
            Spacer(Modifier.height(8.dp))
            listOf(
                "Imprimante Bluetooth 58 mm",
                "2 rouleaux de papier inclus",
                "1 an Petite boutique offert"
            ).forEach { f ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Icon(LissafiIcons.Valider, contentDescription = null, tint = Secondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(f, fontSize = 13.sp, color = OnBackground)
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryActionButton(
                text = "Commander via WhatsApp",
                icon = LissafiIcons.Partager,
                onClick = onOrderWhatsApp,
                containerColor = Secondary
            )
        }
    }
}
