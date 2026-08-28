package com.lissafi.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.ui.components.CapsuleTextField
import com.lissafi.app.ui.components.LissafiHeader
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.components.PrimaryActionButton
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.Error
import com.lissafi.app.ui.theme.OnBackground
import com.lissafi.app.ui.theme.TextSecondary

/**
 * Écran de complétion affiché après une connexion Google : Google ne fournit
 * pas de numéro de téléphone, or le WhatsApp est requis (reçus, suivi, activation).
 * S'affiche tant que `shop_phone` est vide.
 */
@Composable
fun ProfileCompletionScreen(onSubmit: (phone: String, name: String, market: String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var market by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        LissafiHeader(title = "Encore une chose", subtitle = "Pour finaliser ton compte")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Ton numéro WhatsApp nous sert à t'envoyer tes reçus, ton suivi et à activer ton abonnement.",
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(20.dp))
            CapsuleTextField(
                value = phone,
                onValueChange = { phone = it; error = null },
                placeholder = "Ton numéro WhatsApp",
                leadingIcon = LissafiIcons.Telephone
            )
            Spacer(Modifier.height(12.dp))
            CapsuleTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Ton nom (optionnel)",
                leadingIcon = LissafiIcons.Client
            )
            Spacer(Modifier.height(12.dp))
            CapsuleTextField(
                value = market,
                onValueChange = { market = it },
                placeholder = "Ton marché / quartier (optionnel)",
                leadingIcon = LissafiIcons.Lieu
            )

            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error!!, color = Error, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(24.dp))
            PrimaryActionButton(
                text = "Continuer",
                icon = LissafiIcons.Valider,
                onClick = {
                    if (phone.count { it.isDigit() } < 8) {
                        error = "Entre un numéro WhatsApp valide (au moins 8 chiffres)."
                    } else {
                        onSubmit(phone.trim(), name.trim(), market.trim())
                    }
                }
            )
        }
    }
}
