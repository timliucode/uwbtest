package com.example.uwbtest.presentation.screen.ranging

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uwbtest.domain.model.RangingState
import com.example.uwbtest.domain.model.UwbCapabilityStore
import com.example.uwbtest.service.RangingServiceBridge
import com.example.uwbtest.service.UwbBackgroundRangingService
import com.example.uwbtest.service.UwbRangingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RangingMode { FOREGROUND_SERVICE, BACKGROUND }

@HiltViewModel
class RangingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bridge: RangingServiceBridge,
    private val capabilityStore: UwbCapabilityStore,
) : ViewModel() {

    data class UiState(
        val currentState: RangingState = RangingState.Idle,
        val history: List<RangingState.Active> = emptyList(),
        val errorMessage: String? = null,
        val rangingMode: RangingMode = RangingMode.FOREGROUND_SERVICE,
        val backgroundRangingAvailable: Boolean = false,
    )

    private val _uiState = MutableStateFlow(
        UiState(
            backgroundRangingAvailable =
                capabilityStore.lastCapability?.rangingCapabilities?.isBackgroundRangingSupported
                    ?: false,
        ),
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        start()
        collectBridgeState()
    }

    fun setRangingMode(mode: RangingMode) {
        if (_uiState.value.rangingMode == mode) return
        stop()
        _uiState.update { it.copy(rangingMode = mode) }
        start()
    }

    fun start() {
        if (bridge.isRunning) return
        when (_uiState.value.rangingMode) {
            RangingMode.FOREGROUND_SERVICE ->
                context.startForegroundService(UwbRangingService.startIntent(context))
            RangingMode.BACKGROUND ->
                context.startService(UwbBackgroundRangingService.startIntent(context))
        }
    }

    fun stop() {
        context.startService(UwbRangingService.stopIntent(context))
        context.startService(UwbBackgroundRangingService.stopIntent(context))
        _uiState.update { it.copy(currentState = RangingState.Idle) }
    }

    override fun onCleared() {
        super.onCleared()
        if (bridge.isRunning) {
            stop()
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
