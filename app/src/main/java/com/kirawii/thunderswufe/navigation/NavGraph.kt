package com.kirawii.thunderswufe.navigation
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kirawii.thunderswufe.ThunderApplication
import com.kirawii.thunderswufe.ui.screens.HomeScreen
import com.kirawii.thunderswufe.ui.screens.SettingsScreen
import com.kirawii.thunderswufe.ui.screens.UsageAnalysisScreen
import com.kirawii.thunderswufe.ui.viewmodels.SettingsViewModel
import com.kirawii.thunderswufe.ui.viewmodels.SettingsViewModelFactory

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
    object UsageAnalysis : Screen("usage_analysis")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    val application = LocalContext.current.applicationContext as ThunderApplication

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(application)
    )

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onAnalysisClick = {
                    navController.navigate(Screen.UsageAnalysis.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.UsageAnalysis.route) {
            UsageAnalysisScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}