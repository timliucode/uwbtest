package com.example.uwbtest.presentation.screen.capability

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uwbtest.domain.model.UwbCapability
import com.example.uwbtest.domain.model.UwbCapabilityStore
import com.example.uwbtest.domain.usecase.CheckUwbCapabilityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * CapabilityCheckViewModel：UWB 能力檢查畫面的 ViewModel。
 *
 * 同時提供靜態的 [DeviceInfo]（從 Build.* 讀取，無需非同步），
 * 讓 Screen 在畫面載入時立即顯示裝置基本資訊。
 */
@HiltViewModel
class CapabilityCheckViewModel @Inject constructor(
    private val checkCapability: CheckUwbCapabilityUseCase,
    private val capabilityStore: UwbCapabilityStore,
) : ViewModel() {

    // ── 裝置資訊（靜態，畫面載入即可用）────────────────────────

    /**
     * 從 [Build] 讀取的裝置資訊，供 Screen 顯示與複製。
     *
     * @property manufacturer  製造商（e.g. "samsung"）
     * @property model         型號（e.g. "SM-S9180"）
     * @property androidVersion Android 版本字串（e.g. "14"）
     * @property sdkLevel      API Level（e.g. 34）
     * @property oneUiVersion  Samsung OneUI 版本（e.g. "8.5"）；非 Samsung 為 null
     * @property buildDisplay  Build ID / 韌體識別碼（e.g. "S9180ZTU1AWD1"）
     * @property formattedText 供複製的完整文字（含 UWB 狀態由 Screen 補入）
     */
    data class DeviceInfo(
        val manufacturer: String,
        val model: String,
        val androidVersion: String,
        val sdkLevel: Int,
        val oneUiVersion: String?,
        val buildDisplay: String,
    ) {
        /**
         * 格式化為多行文字，方便複製分享。
         * 傳入 [capability] 後會自動附加 UWB 狀態與 Ranging Capabilities 區段；
         * 傳入 null（預設）則只輸出裝置基本資訊。
         */
        fun toClipboardText(capability: UwbCapability? = null): String {
            fun Boolean.toText() = if (this) "Supported" else "Not supported"
            return buildString {
                appendLine("=== UWB Capability Report ===")
                appendLine("Manufacturer : $manufacturer")
                appendLine("Model        : $model")
                appendLine("Android      : $androidVersion (API $sdkLevel)")
                if (oneUiVersion != null) appendLine("OneUI        : $oneUiVersion")
                appendLine("Build        : $buildDisplay")

                if (capability != null) {
                    appendLine()
                    appendLine("[UWB Status]")
                    appendLine("Hardware  : ${if (capability.hardwarePresent) "Present" else "Not found"}")
                    appendLine("Available : ${if (capability.isAvailable) "Yes" else "No"}")
                }

                capability?.rangingCapabilities?.let { caps ->
                    appendLine()
                    appendLine("[Ranging Capabilities]")
                    appendLine("Distance Ranging      : ${caps.isDistanceSupported.toText()}")
                    appendLine("Azimuthal Angle (AoA) : ${caps.isAzimuthalAngleSupported.toText()}")
                    appendLine("Elevation Angle (3D)  : ${caps.isElevationAngleSupported.toText()}")
                    appendLine("Background Ranging    : ${caps.isBackgroundRangingSupported.toText()}")
                    appendLine("Interval Reconfigure  : ${caps.isRangingIntervalReconfigureSupported.toText()}")
                    appendLine("Min Ranging Interval  : ${caps.minRangingInterval} ms")
                    appendLine("Channels              : ${caps.supportedChannels.sorted().joinToString(", ").ifEmpty { "—" }}")
                    appendLine("Config IDs            : ${caps.supportedConfigIds.sorted().joinToString(", ").ifEmpty { "—" }}")
                    appendLine("NTF Configs           : ${caps.supportedNtfConfigs.sorted().joinToString(", ").ifEmpty { "—" }}")
                    appendLine("Slot Durations        : ${caps.supportedSlotDurations.sorted().joinToString(", ").ifEmpty { "—" }}")
                    appendLine("Update Rates          : ${caps.supportedRangingUpdateRates.sorted().joinToString(", ").ifEmpty { "—" }}")
                }

                append("=============================")
            }
        }
    }

    /** 進入畫面即可讀取，不需等待 UWB 檢查 */
    val deviceInfo: DeviceInfo = buildDeviceInfo()

    // ── UWB 能力 UiState ────────────────────────────────────────

    sealed interface UiState {
        data object Idle : UiState
        data object Loading : UiState
        data object PermissionDenied : UiState
        data class Success(val capability: UwbCapability) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** 由 PermissionHandler 確認權限授予後呼叫 */
    fun check() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val capability = checkCapability()
                capabilityStore.lastCapability = capability
                _uiState.value = UiState.Success(capability)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /** 使用者拒絕權限後呼叫 */
    fun onPermissionDenied() {
        _uiState.value = UiState.PermissionDenied
    }

    // ── Private helpers ─────────────────────────────────────────

    private fun buildDeviceInfo(): DeviceInfo {
        val oneUi = readOneUiVersion()
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER ?: "",
            model = Build.MODEL ?: "",
            androidVersion = Build.VERSION.RELEASE ?: "",
            sdkLevel = Build.VERSION.SDK_INT,
            oneUiVersion = oneUi,
            buildDisplay = Build.DISPLAY ?: "",
        )
    }

    /**
     * 讀取 Samsung OneUI 版本。
     *
     * Samsung 將 OneUI 版本儲存在系統屬性 `ro.build.version.oneui` 中，
     * 格式為整數：80500 = 8.5.0、80100 = 8.1.0。
     * 透過反射讀取 SystemProperties（無需特殊權限）。
     *
     * 非 Samsung 裝置或讀取失敗時回傳 null。
     */
    private fun readOneUiVersion(): String? {
        if (Build.MANUFACTURER?.equals("samsung", ignoreCase = true) != true) return null
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java, String::class.java)
            val raw = method.invoke(null, "ro.build.version.oneui", "") as? String
            if (raw.isNullOrBlank()) return null
            // 解析格式：80500 → "8.5" / 80100 → "8.1" / 80000 → "8.0"
            val num = raw.toIntOrNull() ?: return raw
            val major = num / 10000
            val minor = (num % 10000) / 100
            if (minor == 0) "$major.0" else "$major.$minor"
        } catch (e: Exception) {
            null
        }
    }
}
