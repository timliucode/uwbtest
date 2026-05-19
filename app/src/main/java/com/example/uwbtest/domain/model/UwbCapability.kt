package com.example.uwbtest.domain.model

/**
 * UWB 裝置能力描述。
 *
 * @property hardwarePresent  PackageManager 層：裝置是否具備 UWB 硬體晶片
 * @property isAvailable      UwbManager 層：軟體/韌體層是否開放 UWB 功能
 *                            （中國版韌體可能因國碼政策設為 false）
 * @property unavailableReason 若 isAvailable = false，描述原因（供 UI 顯示）
 * @property isN9860          是否為 SM-N9860（Note20 Ultra 中國版），用於顯示專屬警告
 */
data class UwbCapability(
    val hardwarePresent: Boolean,
    val isAvailable: Boolean,
    val unavailableReason: String? = null,
    val isN9860: Boolean = false,
) {
    /** 兩層檢查都通過，才能繼續進行 ranging */
    val canProceed: Boolean get() = hardwarePresent && isAvailable
}
