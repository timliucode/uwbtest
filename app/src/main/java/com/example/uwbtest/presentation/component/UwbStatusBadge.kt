package com.example.uwbtest.presentation.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uwbtest.R
import com.example.uwbtest.domain.model.RangingState

@Composable
fun UwbStatusBadge(
    state: RangingState,
    modifier: Modifier = Modifier,
) {
    val (labelRes, containerColor) = when (state) {
        is RangingState.Idle         -> R.string.status_idle         to Color(0xFF9E9E9E)
        is RangingState.Initializing -> R.string.status_initializing to Color(0xFF2196F3)
        is RangingState.Active       -> R.string.status_active       to Color(0xFF4CAF50)
        is RangingState.Disconnected -> R.string.status_disconnected to Color(0xFFFF9800)
        is RangingState.Failure      -> R.string.status_failure      to Color(0xFFF44336)
    }

    AssistChip(
        onClick = {},
        label = {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        },
        colors = AssistChipDefaults.assistChipColors(containerColor = containerColor),
        modifier = modifier,
    )
}
