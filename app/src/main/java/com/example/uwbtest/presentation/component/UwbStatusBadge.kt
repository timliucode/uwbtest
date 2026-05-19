package com.example.uwbtest.presentation.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.uwbtest.domain.model.RangingState

/**
 * 顯示目前 UWB Ranging 狀態的顏色徽章。
 *
 * 顏色對應（與業界 UWB demo app 慣例一致）：
 *   Idle          → 灰色
 *   Initializing  → 藍色
 *   Active        → 綠色
 *   Disconnected  → 橙色
 *   Failure       → 紅色
 */
@Composable
fun UwbStatusBadge(
    state: RangingState,
    modifier: Modifier = Modifier,
) {
    val (label, containerColor) = when (state) {
        is RangingState.Idle          -> "Idle"          to Color(0xFF9E9E9E)
        is RangingState.Initializing  -> "Initializing"  to Color(0xFF2196F3)
        is RangingState.Active        -> "Active"        to Color(0xFF4CAF50)
        is RangingState.Disconnected  -> "Disconnected"  to Color(0xFFFF9800)
        is RangingState.Failure       -> "Failure"       to Color(0xFFF44336)
    }

    AssistChip(
        onClick = {},
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        },
        colors = AssistChipDefaults.assistChipColors(containerColor = containerColor),
        modifier = modifier,
    )
}
