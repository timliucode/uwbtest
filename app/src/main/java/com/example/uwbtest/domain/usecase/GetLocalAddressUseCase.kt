package com.example.uwbtest.domain.usecase

import com.example.uwbtest.domain.model.UwbDeviceInfo
import com.example.uwbtest.domain.repository.UwbRepository
import javax.inject.Inject

/**
 * 取得本機 UWB 地址的 UseCase。
 *
 * 此 UseCase 在 OobExchangeScreen 載入時被呼叫，
 * 取得的地址顯示給使用者複製，並透過 OOB 傳給對端裝置。
 *
 * ⚠️ 重要：此 UseCase 的呼叫會在 repository 中快取一個 UwbScope。
 *    後續 StartRangingUseCase 必須重用此 scope，
 *    因此兩個 UseCase 在同一個 session 內只能各呼叫一次。
 */
class GetLocalAddressUseCase @Inject constructor(
    private val repository: UwbRepository,
) {
    suspend operator fun invoke(isController: Boolean): Result<UwbDeviceInfo> =
        repository.getLocalDeviceInfo(isController)
}
