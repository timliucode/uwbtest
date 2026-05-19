package com.example.uwbtest.domain.model

/**
 * UWB Ranging 的執行狀態機。
 *
 * 狀態流轉：
 *   Idle → Initializing → Active → (Disconnected | Failure) → Idle
 *
 * UI 對應顏色：
 *   Idle          → 灰色
 *   Initializing  → 藍色（等待 RangingResultInitialized）
 *   Active        → 綠色（持續收到 RangingResultPosition）
 *   Disconnected  → 橙色（對端離開，可重試）
 *   Failure       → 紅色（需要使用者介入）
 */
sealed interface RangingState {

    /** 尚未開始，或已停止 */
    data object Idle : RangingState

    /** 已呼叫 prepareSession().execute()，等待 RangingResultInitialized */
    data object Initializing : RangingState

    /**
     * 收到有效的 RangingResultPosition。
     *
     * @property distanceMeters  到對端的距離（公尺），null 表示此幀無距離資料
     * @property azimuthDegrees  方位角（度，-90 ~ +90），null 表示裝置不支援 AoA
     * @property elevationDegrees 仰角（度），null 表示裝置不支援 3D AoA
     * @property timestampMs     此次 ranging 的系統時間戳（毫秒）
     */
    data class Active(
        val distanceMeters: Float?,
        val azimuthDegrees: Float?,
        val elevationDegrees: Float?,
        val timestampMs: Long,
    ) : RangingState

    /** 對端裝置已離開 ranging session（可能因距離過遠或對端主動停止） */
    data object Disconnected : RangingState

    /**
     * Ranging session 發生錯誤。
     *
     * @property reason  人類可讀的錯誤描述（來自 RangingResultFailure 的 reason code 轉換）
     */
    data class Failure(val reason: String) : RangingState
}
