package com.example.uwbtest.presentation.screen.roleselect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Screen 2：角色選擇畫面。
 *
 * Controller：發起 ranging session，決定 UWB 信道（initiator）
 * Controlee：回應 ranging session，向 Controller 回報位置（responder）
 *
 * 此畫面無 ViewModel，點擊直接透過回調導航。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectScreen(
    onControllerSelected: () -> Unit,
    onControleeSelected: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Select Role") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "步驟 2 / Step 2",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "選擇此裝置在 UWB Ranging 中扮演的角色。\nSelect the role for this device.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RoleCard(
                    icon = Icons.Default.Router,
                    role = "Controller",
                    roleZh = "控制器",
                    description = "發起 ranging session\n決定 UWB 信道\n(Initiator)",
                    onClick = onControllerSelected,
                    modifier = Modifier.weight(1f),
                )
                RoleCard(
                    icon = Icons.Default.Smartphone,
                    role = "Controlee",
                    roleZh = "受控方",
                    description = "回應 ranging session\n向 Controller 回報位置\n(Responder)",
                    onClick = onControleeSelected,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Text(
                    text = "💡 兩台裝置需在同一 Ranging Session 中分別選擇不同角色。\n" +
                        "Both devices must choose different roles in the same session.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleCard(
    icon: ImageVector,
    role: String,
    roleZh: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = role,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(role, style = MaterialTheme.typography.titleMedium)
            Text(roleZh, style = MaterialTheme.typography.labelMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
