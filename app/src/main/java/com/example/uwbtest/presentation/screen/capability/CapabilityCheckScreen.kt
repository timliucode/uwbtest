package com.example.uwbtest.presentation.screen.capability

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uwbtest.domain.model.UwbCapability
import com.example.uwbtest.presentation.component.PermissionHandler

/**
 * Screen 1：UWB 能力檢查畫面。
 *
 * 顯示內容：
 *  1. 裝置基本資訊（型號、OS 版本、韌體 Build）+ 一鍵複製
 *  2. 申請 UWB_RANGING 執行期權限
 *  3. UWB 硬體層 + 軟體層能力檢查結果
 *  4. Android 13 byte-order 提示（若適用）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapabilityCheckScreen(
    onProceed: () -> Unit,
    viewModel: CapabilityCheckViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "步驟 1 / Step 1",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            // ── 裝置資訊 + UWB 狀態 + Ranging Capabilities（合一卡）──
            val info = viewModel.deviceInfo
            val capability = (uiState as? CapabilityCheckViewModel.UiState.Success)?.capability

            DeviceInfoCard(
                info = info,
                capability = capability,
                onCopy = {
                    context.copyToClipboard(
                        "UWB Capability Report",
                        info.toClipboardText(capability),
                    )
                },
            )

            // ── UWB 能力狀態 ──────────────────────────────────────
            when (val state = uiState) {
                is CapabilityCheckViewModel.UiState.Idle -> {
                    Text(
                        "等待權限申請… / Waiting for permission…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                    // DeviceInfoCard 已顯示 UWB Hardware / UWB Available，
                    // 此處只補充「不可用時的原因說明」
                    if (!state.capability.isAvailable && state.capability.unavailableReason != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    text = state.capability.unavailableReason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }

                    // Android 13 byte-order 提示
                    if (state.capability.isAndroid13OrLower && state.capability.isAvailable) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.tertiary)
                                Text(
                                    text = "💡 Android 13 偵測到\n" +
                                        "此 OS 版本存在 UWB 地址 byte-order 已知問題。\n" +
                                        "若 ranging 無法建立，請在下一步開啟「Reverse Bytes」開關。\n\n" +
                                        "Android 13 detected. If ranging fails, try toggling 'Reverse Bytes' on the OOB screen.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.check() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Retry") }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── 裝置資訊 + UWB 狀態 + Ranging Capabilities 合一卡 ──────────

@Composable
private fun DeviceInfoCard(
    info: CapabilityCheckViewModel.DeviceInfo,
    capability: UwbCapability?,
    onCopy: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

            // ── 卡片標題列 ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("裝置資訊 / Device Info", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "複製完整報告",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // ── 裝置基本資訊 ──────────────────────────────────────
            InfoRow("Manufacturer", info.manufacturer)
            InfoRow("Model", info.model)
            InfoRow("Android", "${info.androidVersion}  (API ${info.sdkLevel})")
            if (info.oneUiVersion != null) {
                InfoRow("OneUI", info.oneUiVersion)
            }
            InfoRow("Build", info.buildDisplay)

            // ── UWB 狀態（檢查完成後才顯示）─────────────────────
            if (capability != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SectionLabel("UWB 狀態 / Status")
                InfoRow(
                    label = "Hardware",
                    value = if (capability.hardwarePresent) "Present ✓" else "Not found ✗",
                    valueColor = if (capability.hardwarePresent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                InfoRow(
                    label = "Available",
                    value = if (capability.isAvailable) "Yes ✓" else "No ✗",
                    valueColor = if (capability.isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }

            // ── Ranging Capabilities（isAvailable == true 時才有）
            capability?.rangingCapabilities?.let { caps ->
                fun Boolean.toSupportText() = if (this) "Supported ✓" else "Not supported ✗"

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SectionLabel("Ranging Capabilities")

                InfoRow("Distance Ranging", caps.isDistanceSupported.toSupportText())
                InfoRow("Azimuthal Angle (AoA)", caps.isAzimuthalAngleSupported.toSupportText())
                InfoRow("Elevation Angle (3D)", caps.isElevationAngleSupported.toSupportText())
                InfoRow("Background Ranging", caps.isBackgroundRangingSupported.toSupportText())
                InfoRow("Interval Reconfigure", caps.isRangingIntervalReconfigureSupported.toSupportText())

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                InfoRow("Min Ranging Interval", "${caps.minRangingInterval} ms")
                InfoRow("Channels", caps.supportedChannels.sorted().joinToString(", ").ifEmpty { "—" })
                InfoRow("Config IDs", caps.supportedConfigIds.sorted().joinToString(", ").ifEmpty { "—" })
                InfoRow("NTF Configs", caps.supportedNtfConfigs.sorted().joinToString(", ").ifEmpty { "—" })
                InfoRow("Slot Durations", caps.supportedSlotDurations.sorted().joinToString(", ").ifEmpty { "—" })
                InfoRow("Update Rates", caps.supportedRangingUpdateRates.sorted().joinToString(", ").ifEmpty { "—" })
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            modifier = Modifier.weight(2f),
        )
    }
}

// ── Clipboard helper ────────────────────────────────────────────

private fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
