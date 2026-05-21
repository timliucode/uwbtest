package com.example.uwbtest.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.uwbtest.presentation.navigation.AppNavGraph
import com.example.uwbtest.presentation.navigation.Screen
import com.example.uwbtest.presentation.theme.UwbTestTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NAVIGATE_TO = "navigate_to"
        const val ROUTE_RANGING = "ranging"
    }

    private var pendingNavRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingNavRoute = intent.getStringExtra(EXTRA_NAVIGATE_TO)
        enableEdgeToEdge()
        setContent {
            UwbTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()

                    LaunchedEffect(pendingNavRoute) {
                        val route = pendingNavRoute ?: return@LaunchedEffect
                        pendingNavRoute = null
                        if (route == ROUTE_RANGING) {
                            navController.navigate(Screen.Ranging.route) {
                                launchSingleTop = true
                            }
                        }
                    }

                    AppNavGraph(navController = navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNavRoute = intent.getStringExtra(EXTRA_NAVIGATE_TO)
    }
}
