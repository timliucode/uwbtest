package com.example.uwbtest.service

import app.cash.turbine.test
import com.example.uwbtest.domain.model.RangingState
import com.example.uwbtest.util.MainDispatcherExtension
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class RangingServiceBridgeTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherExtension()

    private lateinit var bridge: RangingServiceBridge

    @BeforeEach
    fun setUp() {
        bridge = RangingServiceBridge()
    }

    @Test
    fun `isRunning starts false`() {
        assertThat(bridge.isRunning).isFalse()
    }

    @Test
    fun `setRunning true - isRunning becomes true`() {
        bridge.setRunning(true)
        assertThat(bridge.isRunning).isTrue()
    }

    @Test
    fun `setRunning false after true - isRunning becomes false`() {
        bridge.setRunning(true)
        bridge.setRunning(false)
        assertThat(bridge.isRunning).isFalse()
    }

    @Test
    fun `emit - state is collected by subscriber`() = runTest {
        val active = RangingState.Active(1.5f, 30f, null, 0L)

        bridge.state.test {
            bridge.emit(active)
            assertThat(awaitItem()).isEqualTo(active)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `replay - new subscriber receives last emitted state`() = runTest {
        val active = RangingState.Active(2.0f, null, null, 1L)
        bridge.emit(active)

        // New subscriber joins after emission
        bridge.state.test {
            assertThat(awaitItem()).isEqualTo(active)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emit multiple states - all collected in order`() = runTest {
        val states = listOf(
            RangingState.Initializing,
            RangingState.Active(1.0f, 10f, null, 0L),
            RangingState.Active(1.5f, 15f, null, 1L),
            RangingState.Disconnected,
        )

        bridge.state.test {
            states.forEach { bridge.emit(it) }
            // replay=1 means only last state is replayed on subscribe — collect all emitted during test
            val collected = mutableListOf<RangingState>()
            repeat(states.size) { collected.add(awaitItem()) }
            assertThat(collected).isEqualTo(states)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
