package com.example.uwbtest.presentation.screen.ranging

import com.example.uwbtest.domain.model.OobParams
import com.example.uwbtest.domain.model.RangingState
import com.example.uwbtest.domain.usecase.StartRangingUseCase
import com.example.uwbtest.presentation.screen.oob.OobParamsHolder
import com.example.uwbtest.util.MainDispatcherExtension
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class RangingViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherExtension()

    private lateinit var startRanging: StartRangingUseCase

    private val testParams = OobParams(
        peerAddress = byteArrayOf(0x01, 0x02),
        channelNumber = 9,
        preambleIndex = 10,
        sessionKeyHex = "0102030405060708",
    )

    @BeforeEach
    fun setUp() {
        startRanging = mock()
        OobParamsHolder.params = testParams
    }

    @AfterEach
    fun tearDown() {
        OobParamsHolder.params = null
    }

    private fun buildViewModel(): RangingViewModel {
        return RangingViewModel(startRanging)
    }

    @Test
    fun `init - starts ranging automatically`() = runTest {
        whenever(startRanging(testParams)).thenReturn(flowOf(RangingState.Initializing))

        buildViewModel()
        advanceUntilIdle()

        verify(startRanging).invoke(testParams)
    }

    @Test
    fun `OobParams null - sets Failure state`() = runTest {
        OobParamsHolder.params = null

        val vm = buildViewModel()

        val state = vm.uiState.value.currentState
        assertThat(state).isInstanceOf(RangingState.Failure::class.java)
        assertThat((state as RangingState.Failure).reason).contains("OobParams not found")
    }

    @Test
    fun `start - updates currentState from flow`() = runTest {
        val active = RangingState.Active(1.5f, null, null, 0L)
        whenever(startRanging(testParams)).thenReturn(flowOf(active))

        val vm = buildViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.currentState).isEqualTo(active)
    }

    @Test
    fun `start - appends Active states to history`() = runTest {
        val active1 = RangingState.Active(1.0f, null, null, 0L)
        val active2 = RangingState.Active(2.0f, null, null, 1L)
        val active3 = RangingState.Active(3.0f, null, null, 2L)
        whenever(startRanging(testParams)).thenReturn(flowOf(active1, active2, active3))

        val vm = buildViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.history).hasSize(3)
    }

    @Test
    fun `start - history is capped at 50 items`() = runTest {
        val states = (0..54).map { i -> RangingState.Active(i.toFloat(), null, null, i.toLong()) }
        whenever(startRanging(testParams)).thenReturn(flow { states.forEach { emit(it) } })

        val vm = buildViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.history).hasSize(50)
    }

    @Test
    fun `start - non-Active states are not added to history`() = runTest {
        whenever(startRanging(testParams)).thenReturn(flowOf(RangingState.Initializing, RangingState.Disconnected))

        val vm = buildViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.history).isEmpty()
    }

    @Test
    fun `start - Failure sets errorMessage`() = runTest {
        whenever(startRanging(testParams)).thenReturn(flowOf(RangingState.Failure("reason 3")))

        val vm = buildViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.errorMessage).isEqualTo("reason 3")
    }

    @Test
    fun `start - Active after Failure clears errorMessage`() = runTest {
        val active = RangingState.Active(1.0f, null, null, 0L)
        whenever(startRanging(testParams)).thenReturn(flow {
            emit(RangingState.Failure("err"))
            emit(active)
        })

        val vm = buildViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.errorMessage).isNull()
    }

    @Test
    fun `stop - cancels job and sets Idle`() = runTest {
        whenever(startRanging(testParams)).thenReturn(flowOf(RangingState.Initializing))

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.stop()

        assertThat(vm.uiState.value.currentState).isEqualTo(RangingState.Idle)
    }

    @Test
    fun `stop - can be called twice without exception`() = runTest {
        whenever(startRanging(testParams)).thenReturn(flowOf())

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.stop()
        vm.stop()

        assertThat(vm.uiState.value.currentState).isEqualTo(RangingState.Idle)
    }

    @Test
    fun `start after stop - calls startRanging again`() = runTest {
        whenever(startRanging(testParams)).thenReturn(flowOf())

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.stop()
        vm.start()
        advanceUntilIdle()

        verify(startRanging, times(2)).invoke(testParams)
    }

    @Test
    fun `Disconnected state - reflected in currentState`() = runTest {
        whenever(startRanging(testParams)).thenReturn(flowOf(RangingState.Disconnected))

        val vm = buildViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.currentState).isEqualTo(RangingState.Disconnected)
    }
}
