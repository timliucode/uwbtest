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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uwbtest.R
import com.example.uwbtest.presentation.component.QrCodeImage
import com.example.uwbtest.presentation.util.QrCodeUtils
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode

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
                        if (viewModel.isController) stringResource(R.string.oob_title_controller)
                        else stringResource(R.string.oob_title_controlee)
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
                text = stringResource(R.string.step_3),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.oob_description),
                style = MaterialTheme.typography.bodySmall,
            )

            HorizontalDivider()

            Text(stringResource(R.string.oob_my_address_title), style = MaterialTheme.typography.titleSmall)

            if (uiState.isLoadingAddress) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.oob_loading_address))
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
                                    text = stringResource(
                                        R.string.oob_channel_preamble_value,
                                        uiState.channelNumber,
                                        uiState.preambleIndex,
                                    ),
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
                            Icon(Icons.Default.ContentCopy, stringResource(R.string.cd_copy_address))
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

            Text(stringResource(R.string.oob_peer_address_title), style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = uiState.peerAddressHex,
                onValueChange = viewModel::onPeerAddressChanged,
                label = { Text(stringResource(R.string.oob_peer_address_label)) },
                isError = uiState.peerAddressError != null,
                supportingText = if (uiState.peerAddressError != null) {
                    { Text(uiState.peerAddressError!!) }
                } else {
                    { Text(stringResource(R.string.oob_peer_address_hint)) }
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
                Text(stringResource(R.string.oob_scan_qr))
            }

            if (!viewModel.isController) {
                Text(stringResource(R.string.oob_channel_params_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.oob_channel_params_hint),
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
                        label = { Text(stringResource(R.string.oob_channel_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = uiState.preambleIndex,
                        onValueChange = viewModel::onPreambleChanged,
                        label = { Text(stringResource(R.string.oob_preamble_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
            }

            HorizontalDivider()

            Text(stringResource(R.string.oob_session_key_title), style = MaterialTheme.typography.titleSmall)
            Text(
                if (viewModel.isController) stringResource(R.string.oob_session_key_hint_controller)
                else stringResource(R.string.oob_session_key_hint_controlee),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = uiState.sessionKeyHex,
                onValueChange = viewModel::onSessionKeyChanged,
                label = { Text(stringResource(R.string.oob_session_key_label)) },
                isError = uiState.sessionKeyError != null,
                supportingText = uiState.sessionKeyError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(stringResource(R.string.oob_reverse_bytes_title), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.oob_reverse_bytes_desc),
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
                    OobParamsHolder.params = viewModel.buildOobParams()
                    onStartRanging()
                },
                enabled = uiState.canProceed,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.oob_start_ranging))
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
