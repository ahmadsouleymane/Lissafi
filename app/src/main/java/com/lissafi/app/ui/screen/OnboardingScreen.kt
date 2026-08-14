package com.lissafi.app.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lissafi.app.R
import com.lissafi.app.data.ApkAttribution
import com.lissafi.app.data.remote.SupabaseApi
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.Border
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.PrimaryContainer
import com.lissafi.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Format des codes partenaire générés par le back-office (ex. PTN-K2M7Q). */
private val PARTNER_CODE_REGEX = Regex("^PTN-[A-Z0-9]{3,}$")

private data class OnboardingSlide(
    val title: String,
    val description: String,
    val icon: ImageVector? = null,
    val imageRes: Int? = null
)

private val OnboardingSlides = listOf(
    OnboardingSlide(
        title = "Ta boutique dans ta poche",
        description = "Vends, encaisse et suis tes clients, tout simplement.",
        imageRes = R.drawable.logo_auth
    ),
    OnboardingSlide(
        title = "Encaisser vite",
        description = "Panier en 2 touches, paiement et ticket imprimé ou envoyé par WhatsApp.",
        icon = LissafiIcons.Caisse
    ),
    OnboardingSlide(
        title = "Clients & crédits",
        description = "Vente à crédit, suivi des dettes, chaque client dans ta liste.",
        icon = LissafiIcons.Clients
    ),
    OnboardingSlide(
        title = "Tes données en sécurité",
        description = "Lissafi marche même hors-ligne, et tes données sont sauvegardées dans le cloud.",
        icon = LissafiIcons.Sync
    )
)

@Composable
fun OnboardingScreen(api: SupabaseApi, onFinish: (partnerCode: String) -> Unit) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val pagerState = rememberPagerState(pageCount = { OnboardingSlides.size })
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Pré-remplissage depuis le presse-papiers (filet secondaire) : la landing
    // y copie le code au clic sur "Télécharger". Lu une seule fois, à l'ouverture
    // de l'app (au premier plan, donc autorisé).
    var partnerCode by remember {
        val clip = clipboardManager.getText()?.text?.trim()?.uppercase().orEmpty()
        mutableStateOf(if (PARTNER_CODE_REGEX.matches(clip)) clip else "")
    }
    val partnerCodeDetected = remember { partnerCode.isNotEmpty() }

    // Source principale : le code EMBARQUÉ dans le commentaire ZIP de l'APK.
    // Plus fiable que le presse-papiers (survit au partage du fichier), il
    // l'emporte si présent. Lecture fichier hors du thread UI.
    LaunchedEffect(Unit) {
        val fromApk = withContext(Dispatchers.IO) { ApkAttribution.readPartnerCode(context) }
        if (fromApk != null) partnerCode = fromApk
    }

    // Résolution du nom du partenaire pour l'affichage ("Tu viens de la part
    // de <Nom>" plutôt que le code brut). Best-effort : si hors ligne ou en
    // échec, on retombe simplement sur le code.
    var partnerName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(partnerCode) {
        if (partnerCode.isNotEmpty()) {
            partnerName = api.getPartnerName(partnerCode)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // ── PASSER (haut droite) ──
        TextButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onFinish(partnerCode.trim().uppercase())
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 16.dp)
        ) {
            Text("Passer", color = TextSecondary)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingSlideView(OnboardingSlides[page])
            }

            // ── INDICATEURS ──
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(OnboardingSlides.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(width = if (selected) 22.dp else 8.dp, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (selected) Primary else Border)
                    )
                }
            }

            // ── CODE PARTENAIRE (visible seulement si détecté depuis le
            // presse-papiers — aucun ajout visuel pour une installation
            // organique, mais confirmation claire quand un partenaire a
            // référé ce client) ──
            if (partnerCodeDetected) {
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Tu viens de la part de ${partnerName ?: partnerCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = { partnerCode = "" }) {
                        Text("Ce n'est pas mon code", color = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── BOUTON PRINCIPAL ──
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (pagerState.currentPage < OnboardingSlides.lastIndex) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinish(partnerCode.trim().uppercase())
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
        }
    }
}

@Composable
private fun OnboardingSlideView(slide: OnboardingSlide) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (slide.imageRes != null) {
            // Slide 1 : logo pleine largeur (comme sur l'écran d'auth)
            Image(
                painter = painterResource(slide.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
            )
        } else {
            // Slides 2-4 : icône dans une pastille ronde flottante
            Surface(
                modifier = Modifier.size(140.dp),
                shape = CircleShape,
                color = PrimaryContainer,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = slide.icon ?: LissafiIcons.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Primary
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = slide.title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = slide.description,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
