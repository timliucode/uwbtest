package com.example.uwbtest.presentation.screen.oob

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uwbtest.presentation.component.QrCodeImage
import com.example.uwbtest.presentation.util.QrCodeUtils
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode

/**
 * Screen 3：OOB（Out-of-Band）參數交換畫面。
 * @OptIn 是因為 Scaffold + TopAppBar 在 Material3 中仍標記為 experimental API。
 *
 * 使用說明：
 *   1. 記下「My Address」並複製（點擊複製圖示）
 *   2. 在另一台裝置的「Peer Address」欄位貼上
 *   3. Controller 端會自動填入 Channel + Preamble，Controlee 端需手動輸入
 *   4. 兩端輸入相同的 Session Key（預設已填寫）
 *   5. 點擊「開始測距 / Start Ranging」
 *
 * Reverse Bytes 開關：Android 13 UWB 地址 byte-order 已知 bug 的 debug 工具。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OobExchangeScreen(
    onStartRanging: () -> Unit,
    viewModel: OobExchangeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scanLauncher = rememberLauncherForActivityResult(ScanQRCode()) { result ->
        if (result is QRResult.QRSuccess) {
            val raw = result.content.rawValue ?: return@rememberLauncherForActivityResult
            viewModel.onQrScanned(raw)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.isController) "Controller — OOB Exchange"
                        else "Controlee — OOB Exchange"
                    )
                },
            )
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
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "步驟 3 / Step 3",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "在兩台裝置之間手動交換 UWB 參數（例：複製貼上）。\n" +
                    "Manually exchange UWB parameters between both devices.",
                style = MaterialTheme.typography.bodySmall,
            )

            HorizontalDivider()

            // ── 本機地址 ────────────────────────────────────────
            Text("本機地址 / My Address", style = MaterialTheme.typography.titleSmall)

            if (uiState.isLoadingAddress) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Retrieving local UWB address…")
                }
            } else if (uiState.addressError != null) {
                Text(uiState.addressError!!, color = MaterialTheme.colorScheme.error)
            } else {
                val info = uiState.localDeviceInfo
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = info?.localAddressHex ?: "--",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            if (viewModel.isController) {
                                Text(
                                    text = "Channel: ${uiState.channelNumber}  |  Preamble: ${uiState.preambleIndex}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val copyText = buildString {
                                    append(info?.localAddressHex ?: "")
                                    if (viewModel.isController) {
                                        append("  CH:${uiState.channelNumber}  PR:${uiState.preambleIndex}")
                                    }
                                }
                                context.copyToClipboard("UWB Address", copyText)
                            },
                        ) {
                            Icon(Icons.Default.ContentCopy, "Copy address")
                        }
                    }
                }

                if (info != null) {
                    QrCodeImage(
                        content = QrCodeUtils.buildQrContent(
                            addr = info.localAddressHex,
                            ch = if (viewModel.isController) uiState.channelNumber.toIntOrNull() else null,
                            pr = if (viewModel.isController) uiState.preambleIndex.toIntOrNull() else null,
                            key = uiState.sessionKeyHex,
                        ),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }

            HorizontalDivider()

            // ── 對端地址 ────────────────────────────────────────
            Text("對端地址 / Peer Address", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = uiState.peerAddressHex,
                onValueChange = viewModel::onPeerAddressChanged,
                label = { Text("Peer UWB Address (hex, e.g. A1:B2)") },
                isError = uiState.peerAddressError != null,
                supportingText = if (uiState.peerAddressError != null) {
                    { Text(uiState.peerAddressError!!) }
                } else {
                    { Text("輸入對端 UWB 地址，或掃描對方 QR Code 自動填入") }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedButton(
                onClick = { scanLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("掃描對方 QR Code / Scan QR")
            }

            // ── Channel + Preamble（Controlee 手動填，Controller 自動填）
            if (!viewModel.isController) {
                Text("信道參數 / Channel Parameters", style = MaterialTheme.typography.titleSmall)
                Text(
                    "從 Controller 的 My Address 卡片取得（例：CH:9 → 填 9，PR:11 → 填 11）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.channelNumber,
                        onValueChange = viewModel::onChannelChanged,
                        label = { Text("Channel") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = uiState.preambleIndex,
                        onValueChange = viewModel::onPreambleChanged,
                        label = { Text("Preamble") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
            }

            HorizontalDivider()

            // ── Session Key ─────────────────────────────────────
            Text("Session Key（兩端必須相同）", style = MaterialTheme.typography.titleSmall)
            Text(
                if (viewModel.isController) "已隨機生成，將包含在 QR Code 中自動傳遞給對方"
                else "掃描 Controller 的 QR Code 後自動填入，可手動覆蓋",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = uiState.sessionKeyHex,
                onValueChange = viewModel::onSessionKeyChanged,
                label = { Text("Session Key (16 hex chars = 8 bytes)") },
                isError = uiState.sessionKeyError != null,
                supportingText = uiState.sessionKeyError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            HorizontalDivider()

            // ── Debug: Reverse Bytes ─────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Reverse Peer Bytes", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "對端地址 byte-order 反轉（Android 13 debug 用）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = uiState.reverseBytes,
                    onCheckedChange = viewModel::onReverseBytesChanged,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    // OobParams 儲存後再導航，讓 RangingViewModel 從共用來源取用
                    OobParamsHolder.params = viewModel.buildOobParams()
                    onStartRanging()
                },
                enabled = uiState.canProceed,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("開始測距 / Start Ranging")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

/**
 * 簡單的進程內共用儲存，用於將 OobParams 從 OobExchangeViewModel 傳遞給 RangingViewModel。
 * 在真實 App 中應使用 SavedStateHandle + Navigation args 或 SharedViewModel。
 */
object OobParamsHolder {
    var params: com.example.uwbtest.domain.model.OobParams? = null
}
