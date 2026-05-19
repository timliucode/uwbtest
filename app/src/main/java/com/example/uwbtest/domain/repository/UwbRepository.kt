package com.example.uwbtest.domain.repository

import com.example.uwbtest.domain.model.OobParams
import com.example.uwbtest.domain.model.RangingState
import com.example.uwbtest.domain.model.UwbCapability
import com.example.uwbtest.domain.model.UwbDeviceInfo
import kotlinx.coroutines.flow.Flow

/**
 * UWB 功能的抽象介面。
 *
 * ─ 此介面位於 domain 層，不含任何 Android 或 androidx.core.uwb 的 import。
 * ─ 實作由 data 層的 UwbRepositoryImpl 提供，透過 Hilt 注入。
 * ─ 所有 suspend fun 已為 coroutine-safe，呼叫端不需要額外的 try/catch（錯誤以 Result 或 Flow 傳遞）。
 */
interface UwbRepository {

    /**
     * 檢查裝置 UWB 能力。
     * 包含：PackageManager 硬體特性 + UwbManager.isAvailable() 軟體層。
     * 需要已獲得 UWB_RANGING 執行期權限，否則回傳 isAvailable=false。
     */
    suspend fun checkCapability(): UwbCapability

    /**
     * 取得本機 UWB 裝置資訊（地址 + 信道，依角色而定）。
     * ⚠️ 每次呼叫都會建立新的 UwbScope，並快取在 repository 中。
     *    此快取的 scope 將在後續的 startRanging() 中被重用。
     *    請勿重複呼叫此方法，否則之前的 OOB 交換將失效。
     *
     * @param isController  true = 建立 Controller scope；false = 建立 Controlee scope
     */
    suspend fun getLocalDeviceInfo(isController: Boolean): Result<UwbDeviceInfo>

    /**
     * 開始 UWB ranging，回傳 Flow。
     * 取消 Flow 的收集（cancel coroutine）即可停止 ranging。
     *
     * @param oobParams  從 OobExchangeScreen 收集到的對端參數
     */
    fun startRanging(oobParams: OobParams): Flow<RangingState>
}
