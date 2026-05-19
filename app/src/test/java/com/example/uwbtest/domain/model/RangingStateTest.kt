package com.example.uwbtest.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RangingStateTest {

    @Test
    fun `Idle is a singleton data object`() {
        assertThat(RangingState.Idle).isSameInstanceAs(RangingState.Idle)
    }

    @Test
    fun `Initializing is a singleton data object`() {
        assertThat(RangingState.Initializing).isSameInstanceAs(RangingState.Initializing)
    }

    @Test
    fun `Disconnected is a singleton data object`() {
        assertThat(RangingState.Disconnected).isSameInstanceAs(RangingState.Disconnected)
    }

    @Test
    fun `Active - equal when all fields match`() {
        val a = RangingState.Active(1.5f, 45.0f, 10.0f, 123L)
        val b = RangingState.Active(1.5f, 45.0f, 10.0f, 123L)
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `Active - not equal when distance differs`() {
        val a = RangingState.Active(1.5f, 45.0f, 10.0f, 123L)
        val b = RangingState.Active(2.0f, 45.0f, 10.0f, 123L)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `Active - not equal when azimuth differs`() {
        val a = RangingState.Active(1.5f, 30.0f, 10.0f, 123L)
        val b = RangingState.Active(1.5f, 90.0f, 10.0f, 123L)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `Active - null distance is allowed`() {
        val a = RangingState.Active(distanceMeters = null, azimuthDegrees = null, elevationDegrees = null, timestampMs = 0L)
        assertThat(a.distanceMeters).isNull()
        assertThat(a.azimuthDegrees).isNull()
        assertThat(a.elevationDegrees).isNull()
    }

    @Test
    fun `Failure - stores reason string`() {
        val f = RangingState.Failure("reason code 3")
        assertThat(f.reason).isEqualTo("reason code 3")
    }

    @Test
    fun `Failure - equal when reason matches`() {
        val a = RangingState.Failure("x")
        val b = RangingState.Failure("x")
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `Failure - not equal when reason differs`() {
        val a = RangingState.Failure("x")
        val b = RangingState.Failure("y")
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `sealed when expression covers all subtypes without else`() {
        val states: List<RangingState> = listOf(
            RangingState.Idle,
            RangingState.Initializing,
            RangingState.Active(1.0f, null, null, 0L),
            RangingState.Disconnected,
            RangingState.Failure("err"),
        )
        val labels = states.map { state ->
            when (state) {
                RangingState.Idle -> "idle"
                RangingState.Initializing -> "init"
                is RangingState.Active -> "active"
                RangingState.Disconnected -> "disconnected"
                is RangingState.Failure -> "failure"
            }
        }
        assertThat(labels).containsExactly("idle", "init", "active", "disconnected", "failure").inOrder()
    }
}
