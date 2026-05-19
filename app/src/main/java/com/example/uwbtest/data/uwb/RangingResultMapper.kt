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
                // 1.0.0：RangingResultFailure 是 RangingResult 的中頂層嵌套類別，
                // 常數透過 RangingResult.RangingResultFailure.CONSTANT 存取
                val reasonText = when (result.reason) {
                    RangingResult.RangingResultFailure.RANGING_FAILURE_REASON_UNKNOWN ->
                        "Unknown failure"
                    RangingResult.RangingResultFailure.RANGING_FAILURE_REASON_STOPPED_BY_REQUEST ->
                        "Stopped by request"
                    else ->
                        "Failure (reason code: ${result.reason})"
                }
                RangingState.Failure(reasonText)
            }

            else -> RangingState.Failure("Unhandled RangingResult type: ${result::class.simpleName}")
        }
}
