package com.example.uwbtest.presentation.component

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * 同時申請 UWB_RANGING 與 POST_NOTIFICATIONS 執行期權限。
 *
 * - [onGranted] 在 UWB_RANGING 已授予時呼叫（通知權限為 best-effort，不影響測距）。
 * - [onDenied]  在 UWB_RANGING 被拒絕時呼叫。
 */
@Composable
fun PermissionHandler(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { results ->
            val uwbGranted = results[Manifest.permission.UWB_RANGING] == true
            if (uwbGranted) onGranted() else onDenied()
        },
    )

    LaunchedEffect(Unit) {
        launcher.launch(
            arrayOf(Manifest.permission.UWB_RANGING, Manifest.permission.POST_NOTIFICATIONS),
        )
    }
}
