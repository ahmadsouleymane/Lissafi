package com.lissafi.app.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.messaging.FirebaseMessaging
import com.lissafi.app.LissafiApp
import com.lissafi.app.data.auth.AuthManager
import com.lissafi.app.data.repository.LissafiRepository
import com.lissafi.app.service.PremiumManager
import com.lissafi.app.ui.components.LissafiIcons
import com.lissafi.app.data.OnboardingManager
import com.lissafi.app.ui.screen.*
import com.lissafi.app.ui.theme.*
import com.lissafi.app.ui.viewmodel.*
import kotlinx.coroutines.launch

object Routes {
    const val AUTH          = "auth"
    const val ONBOARDING    = "onboarding"
    const val CAISSE        = "caisse"
    const val PRODUCTS      = "products"
    const val CLIENTS       = "clients"
    const val CLIENT_DETAIL = "client_detail/{clientId}"
    const val ACTIVITY      = "activity"
    const val SETTINGS      = "settings"
    const val PAYWALL       = "paywall"

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
    val composeScope   = rememberCoroutineScope()
    val authViewModel  = remember {
        AuthViewModel(
            authManager,
            onShopNameSaved = { shopName ->
                if (shopName.isNotBlank()) {
                    composeScope.launch {
                        try {
                            app.database.setSetting("shop_name", shopName)
                        } catch (e: Exception) {
                            Log.w("LissafiNavHost", "Échec sauvegarde nom boutique", e)
                        }
                    }
                }
            }
        )
    }
    val authState      by authViewModel.state.collectAsState()

    val isLoggedIn = authState.isLoggedIn || app.authManager.isLoggedIn()
    val userId = app.authManager.currentUserId() ?: ""
    val repository = remember(userId) {
        LissafiRepository(db, api, onDataChanged = { app.syncManager.syncInBackground() })
            .withUserId(userId)
    }
    val premiumManager = remember(userId) { PremiumManager(repository, api) }

    val navController = rememberNavController()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* refus = pas de notifications, non bloquant */ }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val mainRoutes = setOf(Routes.CAISSE, Routes.PRODUCTS, Routes.CLIENTS, Routes.ACTIVITY)
    val showBottomBar = currentRoute in mainRoutes

    // Verrouillage à la fin de l'essai : réévalué à chaque changement d'écran
    // (couvre le retour depuis la page de paiement web après abonnement).
    var isLocked by remember(userId) { mutableStateOf(false) }
    LaunchedEffect(userId, isLoggedIn, currentRoute) {
        if (isLoggedIn) {
            premiumManager.startTrialIfNeeded()
            isLocked = premiumManager.isLocked()
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            app.syncManager.syncInBackground()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            // Enregistre le token FCM de cet appareil pour le compte connecté
            // (fire-and-forget : onNewToken() le renverra si ça échoue ici).
            FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                api.upsertDeviceToken(fcmToken)
            }

            navController.navigate(Routes.CAISSE) {
                // launchSingleTop : au démarrage déjà connecté, startDestination
                // est déjà CAISSE → évite d'empiler un doublon.
                launchSingleTop = true
                popUpTo(Routes.AUTH) { inclusive = true }
            }
        } else if (OnboardingManager.isCompleted(context)) {
            // Déjà passé par l'onboarding (ou déconnecté) → inscription
            navController.navigate(Routes.AUTH) {
                popUpTo(0) { inclusive = true }
            }
        }
        // Sinon : on reste sur ONBOARDING, ne rien faire (évite d'écraser l'onboarding)
    }

    val cartViewModel: CartViewModel = remember(userId) { CartViewModel(repository, premiumManager) }
    val productViewModel: ProductViewModel = remember(userId) { ProductViewModel(repository, premiumManager) }
    val clientViewModel: ClientViewModel = remember(userId) { ClientViewModel(repository, premiumManager) }
    val reportViewModel: ReportViewModel = remember(userId) { ReportViewModel(repository) }
    val settingsViewModel: SettingsViewModel = remember(userId) { SettingsViewModel(repository, premiumManager) }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Background,
            contentWindowInsets = WindowInsets.safeDrawing
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar && isLoggedIn,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    // Barre de navigation flottante
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Border, RoundedCornerShape(28.dp)),
                            shape = RoundedCornerShape(28.dp),
                            color = Surface,
                            shadowElevation = 14.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                bottomNavItems.forEach { item ->
                                    val selected = currentRoute == item.route
                                    val bgColor by animateColorAsState(
                                        targetValue = if (selected) Primary else Color.Transparent,
                                        label = "navItemBg"
                                    )
                                    val contentColor by animateColorAsState(
                                        targetValue = if (selected) OnPrimary else TextSecondary,
                                        label = "navItemContent"
                                    )
                                    val scale by animateFloatAsState(
                                        targetValue = if (selected) 1f else 0.94f,
                                        label = "navItemScale"
                                    )
                                    Box(
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .scale(scale)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(bgColor)
                                                .clickable {
                                                    navController.navigate(item.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.label,
                                                modifier = Modifier.size(20.dp),
                                                tint = contentColor
                                            )
                                            AnimatedVisibility(visible = selected) {
                                                Row {
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = item.label,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = contentColor,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = when {
                    isLoggedIn -> Routes.CAISSE
                    !OnboardingManager.isCompleted(context) -> Routes.ONBOARDING
                    else -> Routes.AUTH
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        api = api,
                        onFinish = { partnerCode ->
                            OnboardingManager.markCompleted(context)
                            if (partnerCode.isNotEmpty()) {
                                composeScope.launch {
                                    // First-touch : ne jamais écraser un code déjà mémorisé sur
                                    // cet appareil (ex. deuxième passage par l'onboarding).
                                    if (repository.getSetting("partner_code").isNullOrBlank()) {
                                        repository.setSetting("partner_code", partnerCode)
                                    }
                                }
                                // Beacon d'installation : une seule fois par appareil, pour
                                // compter les installs attribués au partenaire (KPI "installés").
                                if (!OnboardingManager.isInstallBeaconSent(context)) {
                                    OnboardingManager.markInstallBeaconSent(context)
                                    api.recordPartnerInstall(partnerCode)
                                }
                            }
                            navController.navigate(Routes.AUTH) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.AUTH) {
                    AuthScreen(viewModel = authViewModel)
                }

                composable(Routes.CAISSE) {
                    CaisseScreen(
                        viewModel = cartViewModel,
                        onNavigateToProducts = { navController.navigate(Routes.PRODUCTS) },
                        onNavigateToClients  = { navController.navigate(Routes.CLIENTS) },
                        onNavigateToReports  = { navController.navigate(Routes.ACTIVITY) },
                        onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
                    )
                }
                composable(Routes.PRODUCTS) {
                    ProductsScreen(
                        viewModel = productViewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToUpgrade = { navController.navigate(Routes.PAYWALL) }
                    )
                }
                composable(Routes.CLIENTS) {
                    ClientsScreen(
                        viewModel = clientViewModel,
                        onClientClick = { clientId -> navController.navigate(Routes.clientDetail(clientId)) },
                        onBack = { navController.popBackStack() },
                        onNavigateToUpgrade = { navController.navigate(Routes.PAYWALL) }
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
                    ReportsScreen(
                        viewModel = reportViewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToProducts = { navController.navigate(Routes.PRODUCTS) }
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        authManager = authManager,
                        onBack = { navController.popBackStack() },
                        onSignOut = { authViewModel.signOut() },
                        onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) }
                    )
                }
                composable(Routes.PAYWALL) {
                    val settingsState by settingsViewModel.state.collectAsState()
                    PaywallScreen(
                        currentUserEmail = authManager.currentUserEmail(),
                        dismissible = true,
                        trialDaysLeft = settingsState.trialDaysLeft,
                        locked = false,
                        onClose = { navController.popBackStack() },
                        onSignOut = { authViewModel.signOut() }
                    )
                }
            }
        }

        // ── Hard-paywall : essai terminé, on couvre toute l'app ──
        if (isLoggedIn && isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                PaywallScreen(
                    currentUserEmail = authManager.currentUserEmail(),
                    dismissible = false,
                    trialDaysLeft = 0,
                    locked = true,
                    onClose = {},
                    onSignOut = { authViewModel.signOut() }
                )
            }
        }
    }
}
