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
 * RangingScreen의 ViewModel.
 *
 * Ranging is handled by [UwbRangingService] (foreground service).
 * This ViewModel:
 *  1. Starts the service via Context (which calls StartRangingUseCase internally)
 *  2. Collects [RangingState] from [RangingServiceBridge] (shared flow)
 *  3. Maintains the last 50 Active measurements as history
 *  4. Stops the service on explicit stop or onCleared
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
        context.startForegroundService(UwbRangingService.startIntent(context))
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
