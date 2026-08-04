package com.lissafi.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
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
import com.lissafi.app.ui.screen.AdminScreen
import com.lissafi.app.ui.screen.AuthScreen
import com.lissafi.app.ui.screen.CaisseScreen
import com.lissafi.app.ui.screen.ClientDetailScreen
import com.lissafi.app.ui.screen.ClientsScreen
import com.lissafi.app.ui.screen.ProductsScreen
import com.lissafi.app.ui.screen.ReportsScreen
import com.lissafi.app.ui.screen.SettingsScreen
import com.lissafi.app.ui.viewmodel.AuthViewModel
import com.lissafi.app.ui.viewmodel.CartViewModel
import com.lissafi.app.ui.viewmodel.ClientViewModel
import com.lissafi.app.ui.viewmodel.ProductViewModel
import com.lissafi.app.ui.viewmodel.ReportViewModel
import com.lissafi.app.ui.viewmodel.SettingsViewModel

object Routes {
    const val AUTH          = "auth"
    const val CAISSE        = "caisse"
    const val PRODUCTS      = "products"
    const val CLIENTS       = "clients"
    const val CLIENT_DETAIL = "client_detail/{clientId}"
    const val REPORTS       = "reports"
    const val SETTINGS      = "settings"
    const val ADMIN         = "admin"

    fun clientDetail(id: String) = "client_detail/$id"
}

private val bottomBarRoutes = setOf(
    Routes.CAISSE, Routes.PRODUCTS, Routes.CLIENTS,
    Routes.REPORTS, Routes.SETTINGS
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.CAISSE,   "Caisse",     Icons.Filled.ShoppingCart),
    BottomNavItem(Routes.PRODUCTS, "Produits",   Icons.Filled.Inventory2),
    BottomNavItem(Routes.CLIENTS,  "Clients",    Icons.Filled.People),
    BottomNavItem(Routes.REPORTS,  "Rapports",   Icons.Filled.BarChart),
    BottomNavItem(Routes.SETTINGS, "Paramètres", Icons.Filled.Settings)
)

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun LissafiNavHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app     = remember { context.applicationContext as LissafiApp }
    val db      = remember { app.database }
    val api     = remember { app.supabaseApi }

    val authManager    = remember { AuthManager(context) }
    val authViewModel  = remember { AuthViewModel(authManager) }
    val authState      by authViewModel.state.collectAsState()

    // Vérifie si l'utilisateur est connecté (session persistée via SharedPreferences)
    val isLoggedIn = authState.isLoggedIn || app.authManager.isLoggedIn()

    // Repository avec userId
    val userId = app.authManager.currentUserId() ?: ""
    val repository = remember(userId) {
        LissafiRepository(db, api).withUserId(userId)
    }
    val premiumManager = remember { PremiumManager(repository) }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomBarRoutes

    // Navigation post-login / post-logout
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            // Synchronisation automatique dès la connexion : récupère les données Supabase
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

    // ViewModels
    val syncStatus by app.syncManager.status.collectAsState()

    val cartViewModel: CartViewModel = remember { CartViewModel(repository, premiumManager) }
    val productViewModel: ProductViewModel = remember { ProductViewModel(repository, premiumManager) }
    val clientViewModel: ClientViewModel = remember { ClientViewModel(repository, premiumManager) }
    val reportViewModel: ReportViewModel = remember { ReportViewModel(repository) }
    val settingsViewModel: SettingsViewModel = remember { SettingsViewModel(repository) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar && isLoggedIn,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
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
                            icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
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
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            // Auth
            composable(Routes.AUTH) {
                AuthScreen(viewModel = authViewModel)
            }

            composable(Routes.CAISSE) {
                CaisseScreen(
                    viewModel = cartViewModel,
                    syncStatus = syncStatus,
                    onNavigateToProducts = { navController.navigate(Routes.PRODUCTS) },
                    onNavigateToClients  = { navController.navigate(Routes.CLIENTS) },
                    onNavigateToReports  = { navController.navigate(Routes.REPORTS) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.PRODUCTS) {
                ProductsScreen(
                    viewModel = productViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.CLIENTS) {
                ClientsScreen(
                    viewModel = clientViewModel,
                    onClientClick = { clientId -> navController.navigate(Routes.clientDetail(clientId)) },
                    onBack = { navController.popBackStack() }
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
            composable(Routes.REPORTS) {
                ReportsScreen(
                    viewModel = reportViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    authManager = authManager,
                    onBack = { navController.popBackStack() },
                    onNavigateToAdmin = { navController.navigate(Routes.ADMIN) },
                    onSignOut = {
                        // La navigation est gérée par LaunchedEffect
                    }
                )
            }
            composable(Routes.ADMIN) {
                AdminScreen(
                    viewModel = settingsViewModel,
                    premiumManager = premiumManager,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
