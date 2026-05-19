package com.example.uwbtest.data.uwb

import androidx.core.uwb.RangingResult
import com.example.uwbtest.domain.model.RangingState
import javax.inject.Inject

/**
 * 將 androidx.core.uwb.RangingResult 轉換為 domain 層的 RangingState。
 *
 * RangingResult 的四種子類型（1.0.0 stable）：
 *   ● RangingResultInitialized      → RangingState.Initializing（握手成功）
 *   ● RangingResultPosition         → RangingState.Active（有效測距資料）
 *   ● RangingResultPeerDisconnected → RangingState.Disconnected
 *   ● RangingResultFailure          → RangingState.Failure（含 reason code）
 */
class RangingResultMapper @Inject constructor() {

    fun map(result: RangingResult, timestampMs: Long = System.currentTimeMillis()): RangingState =
        when (result) {
            is RangingResult.RangingResultInitialized -> {
                // 握手完成，即將開始接收位置資料
                RangingState.Initializing
            }

            is RangingResult.RangingResultPosition -> {
                val position = result.position
                RangingState.Active(
                    distanceMeters = position.distance?.value,
                    azimuthDegrees = position.azimuth?.value,
                    elevationDegrees = position.elevation?.value,
                    timestampMs = timestampMs,
                )
            }

            is RangingResult.RangingResultPeerDisconnected -> {
                RangingState.Disconnected
            }

            is RangingResult.RangingResultFailure -> {
                // 1.0.0 stable：RangingResultFailure.reason 是純 Int，
                // 官方 API 並未在此類別暴露命名常數，直接顯示 reason code 即可。
                // 已知 reason 值參考（來自 AOSP 原始碼，非 public API）：
                //   0 = UNKNOWN
                //   1 = STOPPED_BY_REQUEST
                //   3 = STOPPED_BY_SYSTEM_POLICY (e.g. country code lock on CHC firmware)
                val reasonDescription = when (result.reason) {
                    0 -> "Unknown failure"
                    1 -> "Stopped by request"
                    3 -> "Stopped by system policy (possible country code restriction)"
                    else -> "Failure"
                }
                RangingState.Failure("$reasonDescription (code: ${result.reason})")
            }

            else -> RangingState.Failure("Unhandled RangingResult type: ${result::class.simpleName}")
        }
}
