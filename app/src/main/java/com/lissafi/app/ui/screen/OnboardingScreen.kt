package com.lissafi.app.ui.screen

import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lissafi.app.R
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.ui.theme.Background
import com.lissafi.app.ui.theme.Border
import com.lissafi.app.ui.theme.Primary
import com.lissafi.app.ui.theme.PrimaryContainer
import com.lissafi.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

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
fun OnboardingScreen(onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { OnboardingSlides.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── PASSER (haut droite) ──
        TextButton(
            onClick = onFinish,
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

            Spacer(Modifier.height(24.dp))

            // ── BOUTON PRINCIPAL ──
            Button(
                onClick = {
                    if (pagerState.currentPage < OnboardingSlides.lastIndex) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(52.dp)
            ) {
                Text(
                    if (pagerState.currentPage == OnboardingSlides.lastIndex) "Commencer" else "Continuer"
                )
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
