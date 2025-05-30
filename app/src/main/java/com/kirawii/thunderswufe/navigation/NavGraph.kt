package com.kirawii.thunderswufe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel // 导入 viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kirawii.thunderswufe.ThunderApplication // 导入您的 Application 类
import com.kirawii.thunderswufe.data.database.ElectricityRecord // HomeScreen 需要的
import com.kirawii.thunderswufe.ui.screens.HomeScreen
import com.kirawii.thunderswufe.ui.screens.SettingsScreen
import com.kirawii.thunderswufe.ui.screens.UsageAnalysisScreen
// 导入 SettingsViewModel 和其 Factory
import com.kirawii.thunderswufe.ui.viewmodels.SettingsViewModel
import com.kirawii.thunderswufe.ui.viewmodels.SettingsViewModelFactory

// 如果您使用 Hilt，则导入 hiltViewModel
// import androidx.hilt.navigation.compose.hiltViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
    object UsageAnalysis : Screen("usage_analysis")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
    // 如果 HomeScreen 的 records 也来自 ViewModel，可能需要将 ViewModel 或其依赖项传递到这里
    // 或者 NavGraph 本身不处理数据，数据由 Activity/Fragment 层的 ViewModel 提供给 Composable
) {
    // 获取 Application 实例，用于 ViewModelFactory
    // 如果您不使用需要 Application 上下文的 ViewModel Factory，可以移除此行
    val application = LocalContext.current.applicationContext as ThunderApplication

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            // 假设 HomeScreen 暂时直接接收 records
            // 在实际应用中，您可能希望 HomeScreen 也有一个 ViewModel 来提供 records
            // 例如: val homeViewModel: HomeViewModel = viewModel(...)
            // HomeScreen(viewModel = homeViewModel, ...)
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
            // 创建 SettingsViewModel 实例
            // 如果不使用 Hilt 并且 ViewModel 构造函数有参数，需要 Factory
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(application)
            )

            // 如果使用 Hilt (需要添加 'androidx.hilt:hilt-navigation-compose' 依赖)
            // val settingsViewModel: SettingsViewModel = hiltViewModel()

            SettingsScreen(
    viewModel = settingsViewModel,
    onBack = { navController.popBackStack() }
)
        }

        composable(Screen.UsageAnalysis.route) {
            // 假设 UsageAnalysisScreen 不需要特殊的 ViewModel 或参数
            // 如果需要，处理方式与 SettingsScreen 类似
            // 例如: val analysisViewModel: UsageAnalysisViewModel = viewModel(...)
            // UsageAnalysisScreen(viewModel = analysisViewModel)
            UsageAnalysisScreen()
        }
    }
}