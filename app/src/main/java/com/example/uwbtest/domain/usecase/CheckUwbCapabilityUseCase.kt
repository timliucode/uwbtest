package com.example.uwbtest.domain.usecase

import com.example.uwbtest.domain.model.UwbCapability
import com.example.uwbtest.domain.repository.UwbRepository
import javax.inject.Inject

/**
 * 檢查裝置 UWB 能力的 UseCase。
 *
 * 呼叫 UwbRepository.checkCapability() 並回傳結果。
 * UseCase 的存在讓 ViewModel 不直接依賴 Repository，
 * 未來可在此加入業務邏輯（例：快取結果、記錄 analytics）。
 */
class CheckUwbCapabilityUseCase @Inject constructor(
    private val repository: UwbRepository,
) {
    suspend operator fun invoke(): UwbCapability = repository.checkCapability()
}
