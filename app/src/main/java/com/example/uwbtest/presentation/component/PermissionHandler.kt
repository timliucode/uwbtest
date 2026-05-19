package com.example.uwbtest.presentation.component

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * 可重用的 UWB_RANGING 執行期權限申請 Composable。
 *
 * 使用方式：
 * ```kotlin
 * PermissionHandler(
 *     onGranted = { /* 繼續 UWB 操作 */ },
 *     onDenied  = { /* 顯示說明 UI */ },
 * )
 * ```
 *
 * 此 Composable 在進入 Composition 時自動發起權限申請。
 * 若使用者之前已授予，回調 onGranted 會立即被呼叫（透過 LaunchedEffect）。
 */
@Composable
fun PermissionHandler(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) onGranted() else onDenied()
        },
    )

    // Composable 進入 Composition 時自動發起申請
    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.UWB_RANGING)
    }
}
