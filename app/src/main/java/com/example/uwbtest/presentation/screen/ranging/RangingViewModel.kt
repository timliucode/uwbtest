package com.example.uwbtest.presentation.screen.ranging

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uwbtest.domain.model.RangingState
import com.example.uwbtest.service.RangingServiceBridge
import com.example.uwbtest.service.UwbRangingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * RangingScreen 的 ViewModel。
 *
 * 測距由 [UwbRangingService]（前台服務）執行。此 ViewModel：
 *  1. 若服務尚未運行，透過 Context 啟動服務（服務內部呼叫 StartRangingUseCase）
 *  2. 從 [RangingServiceBridge] 收集 [RangingState]
 *  3. 維護最近 50 筆 Active 測量的歷史記錄
 *  4. 在明確停止或 onCleared 時停止服務
 */
@HiltViewModel
class RangingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bridge: RangingServiceBridge,
) : ViewModel() {

    data class UiState(
        val currentState: RangingState = RangingState.Idle,
        val history: List<RangingState.Active> = emptyList(),
        val errorMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        start()
        collectBridgeState()
    }

    fun start() {
        if (!bridge.isRunning) {
            context.startForegroundService(UwbRangingService.startIntent(context))
        }
    }

    fun stop() {
        context.startService(UwbRangingService.stopIntent(context))
        _uiState.update { it.copy(currentState = RangingState.Idle) }
    }

    override fun onCleared() {
        super.onCleared()
        if (bridge.isRunning) {
            context.startService(UwbRangingService.stopIntent(context))
        }
    }

    private fun collectBridgeState() {
        viewModelScope.launch {
            bridge.state.collect { state ->
                _uiState.update { current ->
                    val newHistory = if (state is RangingState.Active) {
                        (current.history + state).takeLast(50)
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
}
