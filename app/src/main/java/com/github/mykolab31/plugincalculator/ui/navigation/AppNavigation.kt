package com.github.mykolab31.plugincalculator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.mykolab31.plugincalculator.data.repository.PluginRepository
import com.github.mykolab31.plugincalculator.ui.calculator.CalculatorScreen
import com.github.mykolab31.plugincalculator.ui.plugins.PluginDetailScreen
import com.github.mykolab31.plugincalculator.ui.plugins.PluginManagerScreen

sealed class Screen(val route: String) {
    data object Calculator : Screen("calculator")
    data object PluginManager : Screen("plugin_manager")
    data object PluginDetail : Screen("plugin_detail/{pluginId}") {
        fun createRoute(pluginId: String) = "plugin_detail/$pluginId"
    }
}

@Composable
fun AppNavigation(
    repository: PluginRepository,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Calculator.route,
        modifier = modifier
    ) {
        composable(Screen.Calculator.route) {
            CalculatorScreen(
                repository = repository,
                onNavigateToPlugins = {
                    navController.navigate(Screen.PluginManager.route)
                }
            )
        }

        composable(Screen.PluginManager.route) {
            PluginManagerScreen(
                repository = repository,
                onNavigateToDetail = { pluginId ->
                    navController.navigate(Screen.PluginDetail.createRoute(pluginId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.PluginDetail.route) { backStackEntry ->
            val pluginId = backStackEntry.arguments?.getString("pluginId") ?: return@composable
            PluginDetailScreen(
                pluginId = pluginId,
                repository = repository,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}