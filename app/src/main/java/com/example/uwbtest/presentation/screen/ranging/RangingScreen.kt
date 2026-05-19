package com.example.uwbtest.presentation.screen.ranging

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uwbtest.domain.model.RangingState
import com.example.uwbtest.presentation.component.UwbStatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen 4：UWB Ranging 結果顯示畫面。
 *
 * 顯示內容：
 *  ● 大字體即時距離（公尺）
 *  ● 方位角 + 仰角（若裝置支援 AoA）
 *  ● UwbStatusBadge（顏色狀態徽章）
 *  ● 最近 50 筆 Active 記錄（LazyColumn）
 *  ● Stop / Retry 按鈕
 *
 * 使用 collectAsStateWithLifecycle()（非 collectAsState()）
 * 確保 App 進入背景時暫停收集，避免不必要的 ranging 耗電。
 */
@Composable
fun RangingScreen(
    onStop: () -> Unit,
    viewModel: RangingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // 每次新增歷史記錄時自動捲動到最新
    LaunchedEffect(uiState.history.size) {
        if (uiState.history.isNotEmpty()) {
            listState.animateScrollToItem(uiState.history.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("UWB Ranging") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── 狀態徽章 ───────────────────────────────────────
            UwbStatusBadge(state = uiState.currentState)

            Spacer(modifier = Modifier.height(24.dp))

            // ── 大字體距離 ─────────────────────────────────────
            val distance = (uiState.currentState as? RangingState.Active)?.distanceMeters
            Text(
                text = if (distance != null) "%.2f m".format(distance) else "--",
                fontSize = 64.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
            )

            // ── AoA（方位角 + 仰角）──────────────────────────
            val active = uiState.currentState as? RangingState.Active
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                AoaText(
                    label = "Azimuth",
                    value = active?.azimuthDegrees,
                    unit = "°",
                )
                AoaText(
                    label = "Elevation",
                    value = active?.elevationDegrees,
                    unit = "°",
                )
            }

            // ── 錯誤訊息 ───────────────────────────────────────
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // ── 歷史記錄 ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("測距歷史 / History", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${uiState.history.size} / 50",
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

            // ── 按鈕列 ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Retry：僅在 Disconnected 或 Failure 時顯示
                if (uiState.currentState is RangingState.Disconnected ||
                    uiState.currentState is RangingState.Failure
                ) {
                    OutlinedButton(
                        onClick = { viewModel.start() },
                        modifier = Modifier.weight(1f),
                    ) { Text("Retry") }
                }

                Button(
                    onClick = {
                        viewModel.stop()
                        onStop()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("停止 / Stop") }
            }
        }
    }
}

@Composable
private fun AoaText(label: String, value: Float?, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (value != null) "%.1f%s".format(value, unit) else "N/A",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
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
