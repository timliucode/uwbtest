package com.example.uwbtest.domain.model

/**
 * UWB 裝置能力描述。
 *
 * @property hardwarePresent      PackageManager 層：裝置是否具備 UWB 硬體晶片
 * @property isAvailable          UwbManager 層：軟體/韌體層是否開放 UWB 功能
 * @property unavailableReason    若 isAvailable = false，描述原因（供 UI 顯示）
 * @property isAndroid13OrLower   是否為 Android 13 (API 33) 或更舊版本。
 *                                Android 13 存在 UWB 地址 byte-order 已知問題，
 *                                UI 需顯示「Reverse Bytes」debug 提示。
 *                                與裝置型號、韌體版本（BRI/CHC）無關。
 */
data class UwbCapability(
    val hardwarePresent: Boolean,
    val isAvailable: Boolean,
    val unavailableReason: String? = null,
    val isAndroid13OrLower: Boolean = false,
) {
    /** 兩層檢查都通過，才能繼續進行 ranging */
    val canProceed: Boolean get() = hardwarePresent && isAvailable
}
