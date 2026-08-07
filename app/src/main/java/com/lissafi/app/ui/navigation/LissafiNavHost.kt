package com.lissafi.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            // Le nom de boutique saisi à l'inscription n'était jamais enregistré.
            onShopNameSaved = { shopName ->
                if (shopName.isNotBlank()) {
                    composeScope.launch { app.database.setSetting("shop_name", shopName) }
                }
            }
        )
    }
    val authState      by authViewModel.state.collectAsState()

    val isLoggedIn = authState.isLoggedIn || app.authManager.isLoggedIn()
    val userId = app.authManager.currentUserId() ?: ""
    val repository = remember(userId) {
        // Toutes les écritures passent par SyncManager (source de push unique sous mutex).
        LissafiRepository(db, api, onDataChanged = { app.syncManager.syncInBackground() })
            .withUserId(userId)
    }
    val premiumManager = remember(userId) { PremiumManager(repository) }

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

    // ViewModels — keyés sur le userId : chaque changement de compte recrée
    // les ViewModels avec le bon repository (sinon ils restent figés sur le
    // premier utilisateur et toutes les écritures partent sous le mauvais user_id).
    val cartViewModel: CartViewModel = remember(userId) { CartViewModel(repository, premiumManager) }
    val productViewModel: ProductViewModel = remember(userId) { ProductViewModel(repository, premiumManager) }
    val clientViewModel: ClientViewModel = remember(userId) { ClientViewModel(repository, premiumManager) }
    val reportViewModel: ReportViewModel = remember(userId) { ReportViewModel(repository) }
    val settingsViewModel: SettingsViewModel = remember(userId) { SettingsViewModel(repository) }

    Scaffold(
        modifier = modifier,
        containerColor = Background,
        // safeDrawing inclut les insets du clavier (IME) : le contenu remonte
        // au-dessus du clavier au lieu d'être caché.
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar && isLoggedIn,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                NavigationBar(
                    containerColor = Surface,
                    tonalElevation = 3.dp // léger relief
                ) {
                    bottomNavItems.forEach { item ->
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
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
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
                    onBack = { navController.popBackStack() },
                    onNavigateToUpgrade = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.CLIENTS) {
                ClientsScreen(
                    viewModel = clientViewModel,
                    onClientClick = { clientId -> navController.navigate(Routes.clientDetail(clientId)) },
                    onBack = { navController.popBackStack() },
                    onNavigateToUpgrade = { navController.navigate(Routes.SETTINGS) }
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
                    onSignOut = { authViewModel.signOut() }
                )
            }
        }
    }
}
