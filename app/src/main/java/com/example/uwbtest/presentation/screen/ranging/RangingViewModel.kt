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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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

    private val _sessionExpiredEvent = Channel<Unit>(Channel.BUFFERED)
    val sessionExpiredEvent = _sessionExpiredEvent.receiveAsFlow()

    init {
        start()
        collectBridgeState()
    }

    fun setRangingMode(mode: RangingMode) {
        if (_uiState.value.rangingMode == mode) return
        stop()
        _uiState.update { it.copy(rangingMode = mode) }
        // UwbClientSessionScope is single-use; auto-restart would immediately fail with
        // "Ranging has already started". User must redo OOB exchange for a fresh scope.
    }

    fun start() {
        if (bridge.isRunning) return
        startServiceForMode(_uiState.value.rangingMode)
    }

    fun stop() {
        // Guard: don't send a stop intent to a service that isn't running — doing so would
        // start a new instance just to destroy it, creating a race with startForegroundService.
        if (bridge.isRunning) {
            context.startService(UwbRangingService.stopIntent(context))
            context.startService(UwbBackgroundRangingService.stopIntent(context))
        }
        _uiState.update { it.copy(currentState = RangingState.Idle) }
    }

    private fun startServiceForMode(mode: RangingMode) {
        when (mode) {
            RangingMode.FOREGROUND_SERVICE ->
                context.startForegroundService(UwbRangingService.startIntent(context))
            RangingMode.BACKGROUND ->
                context.startService(UwbBackgroundRangingService.startIntent(context))
        }
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
                if (state is RangingState.Failure && isSessionExpiredFailure(state.reason)) {
                    _sessionExpiredEvent.trySend(Unit)
                }
            }
        }
    }

    private fun isSessionExpiredFailure(reason: String?) =
        reason != null && (
            reason.contains("session expired", ignoreCase = true) ||
            reason.contains("OobParams not found", ignoreCase = true)
        )
}
