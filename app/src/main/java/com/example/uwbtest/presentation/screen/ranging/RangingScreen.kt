package com.example.uwbtest.presentation.screen.ranging

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.example.uwbtest.presentation.screen.isExpandedLayout
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uwbtest.R
import com.example.uwbtest.domain.model.RangingState
import com.example.uwbtest.presentation.component.UwbStatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangingScreen(
    onStop: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: RangingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val isExpanded = isExpandedLayout()
    var showSessionExpiredDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.sessionExpiredEvent.collect { showSessionExpiredDialog = true }
    }

    if (showSessionExpiredDialog) {
        AlertDialog(
            onDismissRequest = { showSessionExpiredDialog = false },
            title = { Text(stringResource(R.string.ranging_session_expired_title)) },
            text  = { Text(stringResource(R.string.ranging_session_expired_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showSessionExpiredDialog = false
                    onSessionExpired()
                }) { Text(stringResource(R.string.ranging_session_expired_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showSessionExpiredDialog = false }) {
                    Text(stringResource(R.string.ranging_session_expired_dismiss))
                }
            },
        )
    }

    LaunchedEffect(uiState.history.size) {
        if (uiState.history.isNotEmpty()) {
            listState.animateScrollToItem(uiState.history.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.ranging_title)) })
        },
    ) { innerPadding ->
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    RangingDataPanel(uiState, viewModel, onStop, listState, showCanvas = false)
                }
                Card(modifier = Modifier.weight(1f)) {
                    UwbPositionCanvas(
                        currentActive = uiState.currentState as? RangingState.Active,
                        trail = uiState.history,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RangingDataPanel(uiState, viewModel, onStop, listState)
            }
        }
    }
}

@Composable
private fun ColumnScope.RangingDataPanel(
    uiState: RangingViewModel.UiState,
    viewModel: RangingViewModel,
    onStop: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    showCanvas: Boolean = true,
) {
    var show3d by remember { mutableStateOf(true) }
    val active = uiState.currentState as? RangingState.Active

    UwbStatusBadge(state = uiState.currentState)

    Spacer(modifier = Modifier.height(16.dp))

    val distance = active?.distanceMeters
    Text(
        text = if (distance != null) "%.2f m".format(distance)
               else stringResource(R.string.ranging_distance_placeholder),
        fontSize = 64.sp,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        AoaText(
            label = stringResource(R.string.ranging_azimuth),
            value = active?.azimuthDegrees,
        )
        AoaText(
            label = stringResource(R.string.ranging_elevation),
            value = active?.elevationDegrees,
        )
    }

    if (uiState.errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = uiState.errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }

    // Ranging mode selector — only visible when background ranging is supported
    if (uiState.backgroundRangingAvailable) {
        Spacer(modifier = Modifier.height(4.dp))
        RangingModeSelector(
            selectedMode = uiState.rangingMode,
            onModeSelected = { viewModel.setRangingMode(it) },
        )
        if (uiState.rangingMode == RangingMode.BACKGROUND) {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
            ) {
                Text(
                    text = stringResource(R.string.ranging_mode_background_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 3D visualization card (collapsible, compact mode only)
    if (showCanvas) Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { show3d = !show3d }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.ranging_3d_view_title), style = MaterialTheme.typography.titleSmall)
                Icon(
                    if (show3d) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                )
            }
            AnimatedVisibility(visible = show3d) {
                UwbPositionCanvas(
                    currentActive = active,
                    trail = uiState.history,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.ranging_history_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.ranging_history_count, uiState.history.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(uiState.history, key = { it.timestampMs }) { entry ->
            HistoryRow(entry)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.currentState is RangingState.Disconnected ||
            uiState.currentState is RangingState.Failure
        ) {
            OutlinedButton(
                onClick = { viewModel.start() },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.action_retry)) }
        }

        Button(
            onClick = {
                viewModel.stop()
                onStop()
            },
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.action_stop)) }
    }
}

@Composable
private fun AoaText(label: String, value: Float?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (value != null) "%.1f°".format(value) else stringResource(R.string.ranging_aoa_na),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangingModeSelector(
    selectedMode: RangingMode,
    onModeSelected: (RangingMode) -> Unit,
) {
    val modes = listOf(RangingMode.FOREGROUND_SERVICE, RangingMode.BACKGROUND)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                label = {
                    Text(
                        text = stringResource(
                            if (mode == RangingMode.FOREGROUND_SERVICE)
                                R.string.ranging_mode_foreground
                            else
                                R.string.ranging_mode_background,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
            )
        }
    }
}

private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

@Composable
private fun HistoryRow(entry: RangingState.Active) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = timeFormat.format(Date(entry.timestampMs)),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (entry.distanceMeters != null) "%.2f m".format(entry.distanceMeters) else "--",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = buildString {
                    entry.azimuthDegrees?.let { append("Az:%.1f°".format(it)) }
                    entry.elevationDegrees?.let { if (isNotEmpty()) append("  "); append("El:%.1f°".format(it)) }
                    if (isEmpty()) append("AoA N/A")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
