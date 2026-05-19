package com.example.uwbtest.domain.usecase

import com.example.uwbtest.domain.model.OobParams
import com.example.uwbtest.domain.model.RangingState
import com.example.uwbtest.domain.repository.UwbRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 開始 UWB Ranging 的 UseCase。
 *
 * 回傳 Flow<RangingState>，ViewModel 透過 collectAsStateWithLifecycle() 訂閱。
 * 取消收集（onCleared / Stop 按鈕）即可停止 ranging。
 */
class StartRangingUseCase @Inject constructor(
    private val repository: UwbRepository,
) {
    operator fun invoke(oobParams: OobParams): Flow<RangingState> =
        repository.startRanging(oobParams)
}
