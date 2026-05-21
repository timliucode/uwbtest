package com.example.uwbtest.service

import com.example.uwbtest.domain.model.RangingState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-process bridge between [UwbRangingService] and [RangingViewModel].
 *
 * The service emits [RangingState] updates here; the ViewModel collects them.
 * Using replay=1 so a new subscriber (e.g., after screen rotation) immediately
 * receives the latest state without waiting for the next emission.
 */
@Singleton
class RangingServiceBridge @Inject constructor() {

    private val _state = MutableSharedFlow<RangingState>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val state: SharedFlow<RangingState> = _state.asSharedFlow()

    var isRunning: Boolean = false
        private set

    internal fun setRunning(running: Boolean) {
        isRunning = running
    }

    internal suspend fun emit(state: RangingState) = _state.emit(state)
}
