package com.example.uwbtest.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.uwbtest.presentation.screen.capability.CapabilityCheckScreen
import com.example.uwbtest.presentation.screen.logviewer.LogViewerScreen
import com.example.uwbtest.presentation.screen.oob.OobExchangeScreen
import com.example.uwbtest.presentation.screen.ranging.RangingScreen
import com.example.uwbtest.presentation.screen.roleselect.RoleSelectScreen

/**
 * App 的 Navigation Graph。
 * 在 MainActivity 的 Scaffold 內透過 NavHost 組合。
 *
 * 流程：CapabilityCheck → RoleSelect → OobExchange → Ranging
 */
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.CapabilityCheck.route,
    ) {
        // ── Screen 1：能力檢查 ─────────────────────────────────
        composable(route = Screen.CapabilityCheck.route) {
            CapabilityCheckScreen(
                onProceed = { navController.navigate(Screen.RoleSelect.route) },
                onViewLogs = { navController.navigate(Screen.LogViewer.route) },
            )
        }

        // ── Screen 2：角色選擇 ─────────────────────────────────
        composable(route = Screen.RoleSelect.route) {
            RoleSelectScreen(
                onControllerSelected = {
                    navController.navigate(Screen.OobExchange.createRoute(isController = true))
                },
                onControleeSelected = {
                    navController.navigate(Screen.OobExchange.createRoute(isController = false))
                },
            )
        }

        // ── Screen 3：OOB 參數交換 ─────────────────────────────
        composable(
            route = Screen.OobExchange.route,
            arguments = listOf(
                navArgument("isController") { type = NavType.BoolType },
            ),
        ) {
            OobExchangeScreen(
                onStartRanging = { navController.navigate(Screen.Ranging.route) },
            )
        }

        // ── Screen 4：測距畫面 ─────────────────────────────────
        composable(route = Screen.Ranging.route) {
            RangingScreen(
                onStop = { navController.popBackStack(Screen.RoleSelect.route, inclusive = false) },
            )
        }

        // ── Log Viewer（從 CapabilityCheck TopAppBar 進入）────
        composable(route = Screen.LogViewer.route) {
            LogViewerScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
