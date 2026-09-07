package com.lissafi.app.ui.screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.data.remote.ShopMemberDto
import com.lissafi.app.service.FormatUtils
import com.lissafi.app.ui.components.CapsuleTextField
import com.lissafi.app.ui.components.LissafiCard
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.PrimaryActionButton
import com.lissafi.app.ui.components.SectionHeader
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.Error
import com.lissafi.app.ui.theme.OnBackground
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.PrimaryContainer
import com.lissafi.app.ui.theme.TextSecondary
import com.lissafi.app.ui.viewmodel.ActivityRow
import com.lissafi.app.ui.viewmodel.ShopViewModel

/**
 * Écran « Mes caisses » (offre Grand boutique). Le patron génère des codes
 * d'appairage et gère les caisses de sa boutique ; n'importe quel compte peut
 * rejoindre une boutique via un code (il devient alors une caisse vendeur).
 */
@Composable
fun ShopCaissesScreen(
    viewModel: ShopViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var codeInput by remember { mutableStateOf("") }
    var countedInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        LissafiHeader(
            title = "Mes caisses",
            subtitle = "Plusieurs caisses, une seule boutique",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            state.error?.let { Banner(it, Error) }
            state.info?.let { Banner(it, Primary) }

            if (state.role == "patron") {
                // ── Ajouter une caisse ──
                LissafiCard(cornerRadius = 20, elevation = 0) {
                    Column(Modifier.padding(4.dp)) {
                        SectionHeader("Ajouter une caisse")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Génère un code puis saisis-le sur le téléphone de la nouvelle caisse. Le code est valable 15 minutes.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        PrimaryActionButton(
                            text = "Générer un code",
                            onClick = { viewModel.generatePairingCode() },
                            enabled = !state.loading
                        )
                        state.pairingCode?.let { code ->
                            Spacer(Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(PrimaryContainer)
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = code,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnBackground,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            TextButton(onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Code pour rejoindre ma boutique Lissafi : $code (valable 15 min)"
                                    )
                                }
                                context.startActivity(Intent.createChooser(send, "Partager le code"))
                            }) {
                                Text("Partager le code", color = Primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Caisses de la boutique ──
                LissafiCard(cornerRadius = 20, elevation = 0) {
                    Column(Modifier.padding(4.dp)) {
                        SectionHeader("Caisses de la boutique (${state.members.size})")
                        Spacer(Modifier.height(8.dp))
                        if (state.members.isEmpty()) {
                            Text(
                                "Ta boutique est seule pour l'instant. Ajoute une caisse ci-dessus.",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        } else {
                            state.members.forEach { member ->
                                MemberRow(
                                    member = member,
                                    onRemove = { viewModel.removeMember(member.memberId) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Journal d'activité des caisses (30 derniers jours) ──
                LissafiCard(cornerRadius = 20, elevation = 0) {
                    Column(Modifier.padding(4.dp)) {
                        SectionHeader("Activité récente")
                        Spacer(Modifier.height(8.dp))
                        if (state.activity.isEmpty()) {
                            Text(
                                "Aucune activité sur les 30 derniers jours.",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        } else {
                            state.activity.take(40).forEach { row -> ActivityRowItem(row) }
                        }
                    }
                }
            } else {
                // ── Caisse vendeur rattachée ──
                LissafiCard(cornerRadius = 20, elevation = 0) {
                    Column(Modifier.padding(4.dp)) {
                        SectionHeader("Caisse rattachée")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Ce téléphone est une caisse de la boutique du patron. Les ventes, le stock et les crédits sont partagés en temps réel.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        PrimaryActionButton(
                            text = "Quitter la boutique",
                            onClick = { viewModel.leaveShop { } },
                            enabled = !state.loading,
                            containerColor = Error
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Clôture de caisse « Z » (chaque caisse clôture la sienne) ──
            LissafiCard(cornerRadius = 20, elevation = 0) {
                Column(Modifier.padding(4.dp)) {
                    SectionHeader("Clôture de caisse")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Espèces attendues depuis la dernière clôture :",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = FormatUtils.formatFCFA(state.expectedCash),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnBackground
                    )
                    Spacer(Modifier.height(12.dp))
                    CapsuleTextField(
                        value = countedInput,
                        onValueChange = { countedInput = it.filter { c -> c.isDigit() } },
                        placeholder = "Montant compté (FCFA)"
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryActionButton(
                        text = "Clôturer la caisse",
                        onClick = {
                            viewModel.saveClosure(countedInput.toIntOrNull() ?: 0, "") { countedInput = "" }
                        },
                        enabled = countedInput.isNotBlank() && !state.loading
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Rejoindre une boutique (accessible à tous) ──
            LissafiCard(cornerRadius = 20, elevation = 0) {
                Column(Modifier.padding(4.dp)) {
                    SectionHeader("Rejoindre une boutique")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tu as reçu un code du patron ? Saisis-le pour transformer ce téléphone en caisse de sa boutique.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    CapsuleTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it.uppercase().trim() },
                        placeholder = "Ex. LSF-AB12CD"
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryActionButton(
                        text = "Rejoindre",
                        onClick = { viewModel.joinWithCode(codeInput) { codeInput = "" } },
                        enabled = codeInput.isNotBlank() && !state.loading
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberRow(member: ShopMemberDto, onRemove: () -> Unit) {
    val isPatron = member.role == "patron"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (isPatron) "Patron" else member.caisseLabel.ifBlank { "Caisse vendeur" },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground
            )
            Text(
                text = "ID " + member.memberId.take(8),
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        if (!isPatron) {
            TextButton(onClick = onRemove) {
                Text("Retirer", color = Error, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun Banner(message: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(12.dp)
    ) {
        Text(message, fontSize = 13.sp, color = color, fontWeight = FontWeight.Medium)
    }
    Spacer(Modifier.height(4.dp))
}

// Ligne du journal d'activité (offre Grand boutique).
@Composable
private fun ActivityRowItem(row: ActivityRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = (if (row.type == "vente") "Vente" else "Crédit") + " · " + row.label,
                fontSize = 14.sp,
                fontWeight = if (row.isMe) FontWeight.SemiBold else FontWeight.Normal,
                color = OnBackground
            )
            Text(
                text = FormatUtils.formatDate(row.date),
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Text(
            text = FormatUtils.formatFCFA(row.amount),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary
        )
    }
}
