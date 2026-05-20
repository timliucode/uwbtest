package com.example.uwbtest.data.uwb

import androidx.core.uwb.RangingCapabilities
import androidx.core.uwb.UwbControleeSessionScope
import androidx.core.uwb.UwbControllerSessionScope
import androidx.core.uwb.UwbManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 封裝 androidx.core.uwb.UwbManager 的 wrapper。
 *
 * ──────────────────────────────────────────────────────────────────
 * 設計原則：
 *   ● 整個 App 中，只有此類別直接 import androidx.core.uwb.*
 *   ● 回傳的型別（UwbControllerSessionScope、UwbControleeSessionScope）
 *     只到 UwbRepositoryImpl 為止；domain 層看不到這些類型
 *   ● 方便單元測試：mock 此 wrapper 即可模擬 UWB 行為
 * ──────────────────────────────────────────────────────────────────
 *
 * @property uwbManager  由 Hilt UwbModule 提供（需要 Context）
 */
@Singleton
class UwbManagerWrapper @Inject constructor(
    private val uwbManager: UwbManager,
) {
    /**
     * 檢查 UWB 是否在軟體/韌體層可用。
     * 此為 suspend fun，可能因韌體查詢而短暫阻塞。
     *
     * @throws UnsupportedOperationException 中國版韌體國碼鎖定時可能拋出
     * @throws SecurityException             缺少 UWB_RANGING 權限時拋出
     */
    suspend fun isAvailable(): Boolean = uwbManager.isAvailable()

    // RangingCapabilities is a property on UwbClientSessionScope (not on UwbManager directly).
    // We create a temporary controlee scope solely to read it — prepareSession() is never called.
    suspend fun getRangingCapabilities(): RangingCapabilities =
        uwbManager.controleeSessionScope().rangingCapabilities

    /**
     * 建立 Controller session scope。
     * ⚠️ 每次呼叫回傳「新 scope + 新地址」，呼叫端必須快取此結果。
     *
     * @throws SecurityException 缺少 UWB_RANGING 權限時拋出
     */
    suspend fun createControllerScope(): UwbControllerSessionScope =
        uwbManager.controllerSessionScope()

    /**
     * 建立 Controlee session scope。
     * ⚠️ 每次呼叫回傳「新 scope + 新地址」，呼叫端必須快取此結果。
     *
     * @throws SecurityException 缺少 UWB_RANGING 權限時拋出
     */
    suspend fun createControleeScope(): UwbControleeSessionScope =
        uwbManager.controleeSessionScope()
}
