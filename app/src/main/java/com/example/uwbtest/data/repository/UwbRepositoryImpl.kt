package com.example.uwbtest.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.uwb.RangingParameters
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbControleeSessionScope
import androidx.core.uwb.UwbControllerSessionScope
import androidx.core.uwb.UwbDevice
import com.example.uwbtest.data.uwb.RangingResultMapper
import com.example.uwbtest.data.uwb.UwbManagerWrapper
import com.example.uwbtest.domain.model.OobParams
import com.example.uwbtest.domain.model.RangingState
import com.example.uwbtest.domain.model.UwbCapability
import com.example.uwbtest.domain.model.UwbDeviceInfo
import com.example.uwbtest.domain.model.UwbRole
import com.example.uwbtest.domain.repository.UwbRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UwbRepository"

/**
 * UwbRepository 的實作，位於 data 層。
 *
 * ── Scope 快取策略 ────────────────────────────────────────────────
 * UwbScope（controller 或 controlee）每次建立都會產生新的 localAddress。
 * OobExchangeScreen 顯示的地址和 startRanging 使用的 scope「必須是同一個」，
 * 否則對端使用舊地址但 ranging 用新地址，session 永遠無法建立。
 *
 * 因此：
 *   1. getLocalDeviceInfo() 建立並快取 scope
 *   2. startRanging() 重用該 scope
 *   3. ranging 結束（Flow 取消）後清除快取，下次需重新取得地址
 * ──────────────────────────────────────────────────────────────────
 */
@Singleton
class UwbRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wrapper: UwbManagerWrapper,
    private val mapper: RangingResultMapper,
) : UwbRepository {

    // 快取的 scope（兩者互斥，同時只有一個非 null）
    private var controllerScope: UwbControllerSessionScope? = null
    private var controleeScope: UwbControleeSessionScope? = null

    // ─────────────────────────────────────────────────────────────
    // checkCapability
    // ─────────────────────────────────────────────────────────────

    override suspend fun checkCapability(): UwbCapability {
        val hardwarePresent = context.packageManager
            .hasSystemFeature(PackageManager.FEATURE_UWB)

        val isN9860 = Build.MODEL.contains("N9860", ignoreCase = true)

        if (!hardwarePresent) {
            return UwbCapability(
                hardwarePresent = false,
                isAvailable = false,
                unavailableReason = "No UWB hardware detected on this device.",
                isN9860 = isN9860,
            )
        }

        // ── 軟體層檢查 ───────────────────────────────────────────
        // 中國版 Note20 Ultra (N9860) 可能因國碼政策拋出例外，
        // 需用 try/catch 包裹所有 UwbManager 呼叫
        val isAvailable: Boolean
        val reason: String?

        try {
            isAvailable = wrapper.isAvailable()
            reason = if (!isAvailable) {
                buildString {
                    append("UWB hardware present but software layer unavailable. ")
                    if (isN9860) append("Possible cause: region firmware lock (CHC CSC). ")
                    append("Try: Settings → Connections → Ultra Wideband (UWB).")
                }
            } else null
        } catch (e: UnsupportedOperationException) {
            Log.w(TAG, "checkCapability: UnsupportedOperationException (likely CN firmware lock)", e)
            return UwbCapability(
                hardwarePresent = true,
                isAvailable = false,
                unavailableReason = "UWB is disabled at firmware level. " +
                    "This may be a country code policy on Chinese firmware (CHC CSC).",
                isN9860 = isN9860,
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "checkCapability: SecurityException — UWB_RANGING permission not granted", e)
            return UwbCapability(
                hardwarePresent = true,
                isAvailable = false,
                unavailableReason = "UWB_RANGING permission not granted.",
                isN9860 = isN9860,
            )
        } catch (e: Exception) {
            Log.e(TAG, "checkCapability: Unexpected exception", e)
            return UwbCapability(
                hardwarePresent = true,
                isAvailable = false,
                unavailableReason = "Unexpected error: ${e.message}",
                isN9860 = isN9860,
            )
        }

        return UwbCapability(
            hardwarePresent = true,
            isAvailable = isAvailable,
            unavailableReason = reason,
            isN9860 = isN9860,
        )
    }

    // ─────────────────────────────────────────────────────────────
    // getLocalDeviceInfo — 建立並快取 scope
    // ─────────────────────────────────────────────────────────────

    override suspend fun getLocalDeviceInfo(isController: Boolean): Result<UwbDeviceInfo> =
        runCatching {
            // 清除舊快取，確保每次 OOB 流程使用新 scope
            controllerScope = null
            controleeScope = null

            if (isController) {
                val scope = wrapper.createControllerScope()
                controllerScope = scope
                Log.d(TAG, "Controller scope created. address=${scope.localAddress.address.toHex()}, " +
                    "channel=${scope.uwbComplexChannel.channel}, preamble=${scope.uwbComplexChannel.preambleIndex}")
                UwbDeviceInfo(
                    localAddress = scope.localAddress.address,
                    role = UwbRole.Controller,
                    channelNumber = scope.uwbComplexChannel.channel,
                    preambleIndex = scope.uwbComplexChannel.preambleIndex,
                )
            } else {
                val scope = wrapper.createControleeScope()
                controleeScope = scope
                Log.d(TAG, "Controlee scope created. address=${scope.localAddress.address.toHex()}")
                UwbDeviceInfo(
                    localAddress = scope.localAddress.address,
                    role = UwbRole.Controlee,
                )
            }
        }

    // ─────────────────────────────────────────────────────────────
    // startRanging — 重用快取 scope，建立 Flow<RangingState>
    // ─────────────────────────────────────────────────────────────

    override fun startRanging(oobParams: OobParams): Flow<RangingState> {
        // 決定 peer address（可選 reverse bytes，用於 Android 13 byte-order debug）
        val rawPeerAddress = if (oobParams.reverseBytes) {
            oobParams.peerAddress.reversedArray()
        } else {
            oobParams.peerAddress
        }

        // 解析 Session Key（16 hex chars → 8 bytes）
        val sessionKey = oobParams.sessionKeyHex
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

        val peerDevice = UwbDevice.createForAddress(UwbAddress(rawPeerAddress))
        val complexChannel = UwbComplexChannel(oobParams.channelNumber, oobParams.preambleIndex)

        val params = RangingParameters(
            uwbConfigType = RangingParameters.CONFIG_UNICAST_DS_TWR,
            sessionId = 0,  // 0 = auto-generate
            sessionKeyInfo = sessionKey,
            complexChannel = complexChannel,
            peerDevices = listOf(peerDevice),
            updateRateType = RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC,
        )

        Log.d(TAG, "startRanging: peer=${rawPeerAddress.toHex()}, channel=${oobParams.channelNumber}, " +
            "preamble=${oobParams.preambleIndex}, sessionKey=${oobParams.sessionKeyHex}")

        // 重用快取的 scope；若無快取則拋出例外（應在呼叫 getLocalDeviceInfo 後才呼叫此方法）
        val rangingFlow = controllerScope?.let { scope ->
            scope.prepareSession(params).execute()
        } ?: controleeScope?.let { scope ->
            scope.prepareSession(params).execute()
        } ?: throw IllegalStateException(
            "No cached UwbScope found. Call getLocalDeviceInfo() before startRanging()."
        )

        return rangingFlow
            .map { result ->
                Log.v(TAG, "RangingResult: ${result::class.simpleName}")
                mapper.map(result)
            }
            .catch { e ->
                Log.e(TAG, "Ranging flow error", e)
                emit(RangingState.Failure("Ranging error: ${e.message}"))
            }
    }

    // ─────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────

    private fun ByteArray.toHex(): String = joinToString(":") { "%02X".format(it) }
}
