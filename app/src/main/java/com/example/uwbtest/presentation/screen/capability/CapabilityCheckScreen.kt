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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uwbtest.R
import com.example.uwbtest.domain.model.UwbCapability
import com.example.uwbtest.presentation.component.PermissionHandler

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
            TopAppBar(title = { Text(stringResource(R.string.capability_title)) })
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
                text = stringResource(R.string.step_1),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )

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

            when (val state = uiState) {
                is CapabilityCheckViewModel.UiState.Idle -> {
                    Text(
                        stringResource(R.string.capability_waiting_permission),
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
                        Text(stringResource(R.string.capability_checking))
                    }
                }

                is CapabilityCheckViewModel.UiState.PermissionDenied -> {
                    ErrorCard(message = stringResource(R.string.capability_permission_denied))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.check() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.action_retry)) }
                }

                is CapabilityCheckViewModel.UiState.Success -> {
                    if (!state.capability.isAvailable && state.capability.unavailableReason != null) {
                        ErrorCard(message = state.capability.unavailableReason)
                    }

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
                                    text = stringResource(R.string.capability_android13_warning),
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
                        ) { Text(stringResource(R.string.action_retry)) }

                        Button(
                            onClick = onProceed,
                            enabled = state.capability.canProceed,
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.action_continue)) }
                    }
                }

                is CapabilityCheckViewModel.UiState.Error -> {
                    ErrorCard(message = state.message)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.check() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.action_retry)) }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
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
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.device_info_title), style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.cd_copy_report),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            InfoRow(stringResource(R.string.device_manufacturer), info.manufacturer)
            InfoRow(stringResource(R.string.device_model), info.model)
            InfoRow(
                stringResource(R.string.device_android),
                stringResource(R.string.device_android_value, info.androidVersion, info.sdkLevel),
            )
            if (info.oneUiVersion != null) {
                InfoRow(stringResource(R.string.device_oneui), info.oneUiVersion)
            }
            InfoRow(stringResource(R.string.device_build), info.buildDisplay)

            if (capability != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SectionLabel(stringResource(R.string.uwb_status_section))
                InfoRow(
                    label = stringResource(R.string.uwb_hardware),
                    value = if (capability.hardwarePresent) stringResource(R.string.label_present)
                            else stringResource(R.string.label_not_found),
                    valueColor = if (capability.hardwarePresent) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.error,
                )
                InfoRow(
                    label = stringResource(R.string.uwb_available),
                    value = if (capability.isAvailable) stringResource(R.string.label_yes)
                            else stringResource(R.string.label_no),
                    valueColor = if (capability.isAvailable) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.error,
                )
            }

            val labelSupported = stringResource(R.string.label_supported)
            val labelNotSupported = stringResource(R.string.label_not_supported)

            capability?.rangingCapabilities?.let { caps ->
                fun Boolean.toSupportText() =
                    if (this) labelSupported else labelNotSupported

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SectionLabel(stringResource(R.string.ranging_capabilities_section))

                InfoRow(stringResource(R.string.cap_distance_ranging), caps.isDistanceSupported.toSupportText())
                InfoRow(stringResource(R.string.cap_azimuthal_angle), caps.isAzimuthalAngleSupported.toSupportText())
                InfoRow(stringResource(R.string.cap_elevation_angle), caps.isElevationAngleSupported.toSupportText())
                InfoRow(stringResource(R.string.cap_background_ranging), caps.isBackgroundRangingSupported.toSupportText())
                InfoRow(stringResource(R.string.cap_interval_reconfigure), caps.isRangingIntervalReconfigureSupported.toSupportText())

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                InfoRow(stringResource(R.string.cap_min_interval),
                    stringResource(R.string.cap_min_interval_value, caps.minRangingInterval))
                InfoRow(stringResource(R.string.cap_channels),
                    caps.supportedChannels.sorted().joinToString(", ").ifEmpty { "—" })
                InfoRow(stringResource(R.string.cap_config_ids),
                    caps.supportedConfigIds.sorted().joinToString(", ").ifEmpty { "—" })
                InfoRow(stringResource(R.string.cap_ntf_configs),
                    caps.supportedNtfConfigs.sorted().joinToString(", ").ifEmpty { "—" })
                InfoRow(stringResource(R.string.cap_slot_durations),
                    caps.supportedSlotDurations.sorted().joinToString(", ").ifEmpty { "—" })
                InfoRow(stringResource(R.string.cap_update_rates),
                    caps.supportedRangingUpdateRates.sorted().joinToString(", ").ifEmpty { "—" })
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

private fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
