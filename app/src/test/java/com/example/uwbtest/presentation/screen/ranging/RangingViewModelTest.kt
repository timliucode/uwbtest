package com.example.uwbtest.presentation.screen.ranging

import android.content.Context
import com.example.uwbtest.domain.model.RangingState
import com.example.uwbtest.service.RangingServiceBridge
import com.example.uwbtest.util.MainDispatcherExtension
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock

/**
 * RangingViewModel tests.
 *
 * The ViewModel now delegates ranging to [com.example.uwbtest.service.UwbRangingService]
 * and subscribes to states via [RangingServiceBridge]. Tests inject states directly
 * into the bridge to verify ViewModel behavior without starting a real service.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RangingViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherExtension()

    private lateinit var bridge: RangingServiceBridge
    private lateinit var mockContext: Context

    @BeforeEach
    fun setUp() {
        bridge = RangingServiceBridge()
        mockContext = mock()
    }

    private fun buildViewModel() = RangingViewModel(mockContext, bridge)

    // ── State collection from bridge ────────────────────────────

    @Test
    fun `initial state is Idle`() {
        val vm = buildViewModel()
        assertThat(vm.uiState.value.currentState).isEqualTo(RangingState.Idle)
    }

    @Test
    fun `bridge Active emission - updates currentState and history`() = runTest {
        val active = RangingState.Active(1.5f, 30f, null, 0L)
        val vm = buildViewModel()

        bridge.emit(active)
        advanceUntilIdle()

        assertThat(vm.uiState.value.currentState).isEqualTo(active)
        assertThat(vm.uiState.value.history).containsExactly(active)
    }

    @Test
    fun `bridge Disconnected emission - updates currentState, no history entry`() = runTest {
        val vm = buildViewModel()

        bridge.emit(RangingState.Disconnected)
        advanceUntilIdle()

        assertThat(vm.uiState.value.currentState).isEqualTo(RangingState.Disconnected)
        assertThat(vm.uiState.value.history).isEmpty()
    }

    @Test
    fun `bridge Failure emission - sets errorMessage`() = runTest {
        val vm = buildViewModel()

        bridge.emit(RangingState.Failure("stopped by policy (code: 3)"))
        advanceUntilIdle()

        assertThat(vm.uiState.value.errorMessage).isEqualTo("stopped by policy (code: 3)")
    }

    @Test
    fun `Active emission after Failure - clears errorMessage`() = runTest {
        val active = RangingState.Active(2.0f, null, null, 1L)
        val vm = buildViewModel()

        bridge.emit(RangingState.Failure("err"))
        advanceUntilIdle()
        bridge.emit(active)
        advanceUntilIdle()

        assertThat(vm.uiState.value.errorMessage).isNull()
    }

    // ── History tracking ────────────────────────────────────────

    @Test
    fun `history is capped at 50 items`() = runTest {
        val vm = buildViewModel()

        repeat(55) { i ->
            bridge.emit(RangingState.Active(i.toFloat(), null, null, i.toLong()))
        }
        advanceUntilIdle()

        assertThat(vm.uiState.value.history).hasSize(50)
    }

    @Test
    fun `non-Active states are not added to history`() = runTest {
        val vm = buildViewModel()

        bridge.emit(RangingState.Initializing)
        bridge.emit(RangingState.Disconnected)
        advanceUntilIdle()

        assertThat(vm.uiState.value.history).isEmpty()
    }

    // ── Stop ───────────────────────────────────────────────────

    @Test
    fun `stop - sets currentState to Idle immediately`() = runTest {
        val vm = buildViewModel()
        bridge.emit(RangingState.Active(1.0f, null, null, 0L))
        advanceUntilIdle()

        vm.stop()

        assertThat(vm.uiState.value.currentState).isEqualTo(RangingState.Idle)
    }

    @Test
    fun `stop - can be called multiple times without exception`() = runTest {
        val vm = buildViewModel()

        vm.stop()
        vm.stop()

        assertThat(vm.uiState.value.currentState).isEqualTo(RangingState.Idle)
    }
}
