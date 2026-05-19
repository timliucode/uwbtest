package com.example.uwbtest.presentation.screen.capability

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uwbtest.domain.model.UwbCapability
import com.example.uwbtest.presentation.component.PermissionHandler

/**
 * Screen 1：UWB 能力檢查畫面。
 *
 * 流程：
 *   1. PermissionHandler 自動發起 UWB_RANGING 申請
 *   2. 授予 → vm.check() → 顯示硬體/軟體兩層狀態
 *   3. 兩層都通過 → 啟用「繼續」按鈕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapabilityCheckScreen(
    onProceed: () -> Unit,
    viewModel: CapabilityCheckViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 申請執行期權限
    PermissionHandler(
        onGranted = { viewModel.check() },
        onDenied  = { viewModel.onPermissionDenied() },
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("UWB Capability Check") })
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
                text = "步驟 1 / Step 1",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "正在檢查此裝置是否支援 UWB 功能。\nChecking UWB support on this device.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                is CapabilityCheckViewModel.UiState.Idle -> {
                    // 等待權限申請結果
                }

                is CapabilityCheckViewModel.UiState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Checking UWB availability…")
                    }
                }

                is CapabilityCheckViewModel.UiState.Success -> {
                    CapabilityResultCard(capability = state.capability)

                    // Android 13 byte-order 提示（OS 版本問題，非韌體地區問題）
                    if (state.capability.isAndroid13OrLower && state.capability.isAvailable) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF3E0),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                )
                                Text(
                                    text = "💡 Android 13 偵測到\n" +
                                        "此 OS 版本存在 UWB 地址 byte-order 已知問題。\n" +
                                        "若 ranging 無法建立，請在 OOB 交換畫面開啟「Reverse Bytes」開關。\n\n" +
                                        "Android 13 detected. Known UWB address byte-order issue may occur. " +
                                        "If ranging fails, toggle 'Reverse Bytes' on the OOB screen.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.check() },
                            modifier = Modifier.weight(1f),
                        ) { Text("Retry") }

                        Button(
                            onClick = onProceed,
                            enabled = state.capability.canProceed,
                            modifier = Modifier.weight(1f),
                        ) { Text("繼續 / Continue") }
                    }
                }

                is CapabilityCheckViewModel.UiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(Icons.Default.Error, null, tint = Color(0xFFF44336))
                            Text(state.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { viewModel.check() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Retry") }
                }
            }
        }
    }
}

@Composable
private fun CapabilityResultCard(capability: UwbCapability) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("裝置能力 / Device Capability", style = MaterialTheme.typography.titleSmall)

            CapabilityRow(
                label = "UWB Hardware",
                ok = capability.hardwarePresent,
                detail = if (capability.hardwarePresent) "Present ✓" else "Not found ✗",
            )
            CapabilityRow(
                label = "UWB API Available",
                ok = capability.isAvailable,
                detail = if (capability.isAvailable) "Available ✓" else "Unavailable ✗",
            )

            if (!capability.isAvailable && capability.unavailableReason != null) {
                Text(
                    text = capability.unavailableReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF44336),
                )
            }
        }
    }
}

@Composable
private fun CapabilityRow(label: String, ok: Boolean, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (ok) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(18.dp),
            )
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}
