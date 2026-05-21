package com.example.uwbtest.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.uwbtest.presentation.screen.about.AboutScreen
import com.example.uwbtest.presentation.screen.capability.CapabilityCheckScreen
import com.example.uwbtest.presentation.screen.logviewer.LogViewerScreen
import com.example.uwbtest.presentation.screen.oob.OobExchangeScreen
import com.example.uwbtest.presentation.screen.ranging.RangingScreen
import com.example.uwbtest.presentation.screen.roleselect.RoleSelectScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.CapabilityCheck.route,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) + fadeIn() },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, targetOffset = { it / 3 }) + fadeOut() },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, initialOffset = { it / 3 }) + fadeIn() },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) + fadeOut() },
    ) {
        // ── Screen 1：能力檢查 ─────────────────────────────────
        composable(route = Screen.CapabilityCheck.route) {
            CapabilityCheckScreen(
                onProceed = { navController.navigate(Screen.RoleSelect.route) },
                onViewLogs = { navController.navigate(Screen.LogViewer.route) },
                onAbout = { navController.navigate(Screen.About.route) },
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

        // ── About ──────────────────────────────────────────────
        composable(route = Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
