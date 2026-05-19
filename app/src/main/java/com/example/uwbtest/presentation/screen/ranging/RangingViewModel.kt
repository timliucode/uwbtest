package com.example.uwbtest.presentation.screen.ranging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uwbtest.domain.model.RangingState
import com.example.uwbtest.domain.usecase.StartRangingUseCase
import com.example.uwbtest.presentation.screen.oob.OobParamsHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * RangingScreen 的 ViewModel。
 *
 * 職責：
 *  1. 從 OobParamsHolder 取得 OobParams（由 OobExchangeScreen 設定）
 *  2. 呼叫 StartRangingUseCase 取得 Flow<RangingState>
 *  3. 將每次 Active 狀態加入歷史記錄（最多 50 筆）
 *  4. 提供「停止」功能（取消 coroutine）
 *  5. onCleared() 確保 ranging 被停止
 */
@HiltViewModel
class RangingViewModel @Inject constructor(
    private val startRanging: StartRangingUseCase,
) : ViewModel() {

    data class UiState(
        val currentState: RangingState = RangingState.Idle,
        val history: List<RangingState.Active> = emptyList(),
        val errorMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var rangingJob: Job? = null

    init {
        start()
    }

    fun start() {
        val params = OobParamsHolder.params ?: run {
            _uiState.update {
                it.copy(
                    currentState = RangingState.Failure("OobParams not found. Please complete OOB exchange first."),
                )
            }
            return
        }

        rangingJob?.cancel()
        rangingJob = viewModelScope.launch {
            _uiState.update { it.copy(currentState = RangingState.Initializing, errorMessage = null) }

            startRanging(params).collect { state ->
                _uiState.update { current ->
                    val newHistory = if (state is RangingState.Active) {
                        (current.history + state).takeLast(50)  // 最多保留 50 筆
                    } else {
                        current.history
                    }
                    current.copy(
                        currentState = state,
                        history = newHistory,
                        errorMessage = if (state is RangingState.Failure) state.reason else null,
                    )
                }
            }
        }
    }

    fun stop() {
        rangingJob?.cancel()
        rangingJob = null
        _uiState.update { it.copy(currentState = RangingState.Idle) }
    }

    override fun onCleared() {
        super.onCleared()
        rangingJob?.cancel()  // Activity/Fragment 銷毀時確保停止 ranging
    }
}
