package com.lissafi.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lissafi.app.LissafiApp
import com.lissafi.app.data.repository.LissafiRepository
import com.lissafi.app.service.PremiumManager
import com.lissafi.app.ui.screen.AdminScreen
import com.lissafi.app.ui.screen.CaisseScreen
import com.lissafi.app.ui.screen.ClientsScreen
import com.lissafi.app.ui.screen.ProductsScreen
import com.lissafi.app.ui.screen.ReportsScreen
import com.lissafi.app.ui.screen.SettingsScreen
import com.lissafi.app.ui.viewmodel.CartViewModel
import com.lissafi.app.ui.viewmodel.ClientViewModel
import com.lissafi.app.ui.viewmodel.ProductViewModel
import com.lissafi.app.ui.viewmodel.ReportViewModel
import com.lissafi.app.ui.viewmodel.SettingsViewModel

object Routes {
    const val CAISSE = "caisse"
    const val PRODUCTS = "products"
    const val CLIENTS = "clients"
    const val CLIENT_DETAIL = "client_detail/{clientId}"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val ADMIN = "admin"

    fun clientDetail(id: String) = "client_detail/$id"
}

@Composable
fun LissafiNavHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = remember { (context.applicationContext as LissafiApp).database }
    val repository = remember { LissafiRepository(db) }
    val premiumManager = remember { PremiumManager(repository) }

    val navController = rememberNavController()

    val cartViewModel = remember { CartViewModel(repository, premiumManager) }
    val productViewModel = remember { ProductViewModel(repository, premiumManager) }
    val clientViewModel = remember { ClientViewModel(repository, premiumManager) }
    val reportViewModel = remember { ReportViewModel(repository) }
    val settingsViewModel = remember { SettingsViewModel(repository) }

    NavHost(
        navController = navController,
        startDestination = Routes.CAISSE,
        modifier = modifier
    ) {
        composable(Routes.CAISSE) {
            CaisseScreen(
                viewModel = cartViewModel,
                onNavigateToProducts = { navController.navigate(Routes.PRODUCTS) },
                onNavigateToClients = { navController.navigate(Routes.CLIENTS) },
                onNavigateToReports = { navController.navigate(Routes.REPORTS) },
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
                onClientClick = { clientId ->
                    navController.navigate(Routes.clientDetail(clientId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CLIENT_DETAIL,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType })
        ) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: return@composable
            com.lissafi.app.ui.screen.ClientDetailScreen(
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
                onBack = { navController.popBackStack() },
                onNavigateToAdmin = { navController.navigate(Routes.ADMIN) }
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
