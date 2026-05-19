package com.example.uwbtest.data.uwb

import androidx.core.uwb.RangingMeasurement
import androidx.core.uwb.RangingPosition
import androidx.core.uwb.RangingResult
import androidx.core.uwb.UwbDevice
import com.example.uwbtest.domain.model.RangingState
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class RangingResultMapperTest {

    private lateinit var mapper: RangingResultMapper

    private val dummyDevice = UwbDevice.createForAddress(byteArrayOf(0x01, 0x02))

    @Before
    fun setUp() {
        mapper = RangingResultMapper()
    }

    @Test
    fun `RangingResultInitialized maps to Initializing`() {
        val result = RangingResult.RangingResultInitialized(dummyDevice)
        assertThat(mapper.map(result)).isEqualTo(RangingState.Initializing)
    }

    @Test
    fun `RangingResultPeerDisconnected maps to Disconnected`() {
        val result = RangingResult.RangingResultPeerDisconnected(dummyDevice, reason = 0)
        assertThat(mapper.map(result)).isEqualTo(RangingState.Disconnected)
    }

    @Test
    fun `RangingResultPosition with all values maps to Active`() {
        val position = RangingPosition(
            distance = RangingMeasurement(1.5f),
            azimuth = RangingMeasurement(30.0f),
            elevation = RangingMeasurement(5.0f),
            elapsedRealtimeNanos = 0L,
        )
        val result = RangingResult.RangingResultPosition(dummyDevice, position)

        val state = mapper.map(result, timestampMs = 12345L)

        assertThat(state).isEqualTo(RangingState.Active(1.5f, 30.0f, 5.0f, 12345L))
    }

    @Test
    fun `RangingResultPosition with null distance maps to Active with null distance`() {
        val position = RangingPosition(
            distance = null,
            azimuth = null,
            elevation = null,
            elapsedRealtimeNanos = 0L,
        )
        val result = RangingResult.RangingResultPosition(dummyDevice, position)

        val state = mapper.map(result, timestampMs = 0L) as RangingState.Active

        assertThat(state.distanceMeters).isNull()
        assertThat(state.azimuthDegrees).isNull()
        assertThat(state.elevationDegrees).isNull()
    }

    @Test
    fun `RangingResultPosition with partial measurements maps correctly`() {
        val position = RangingPosition(
            distance = RangingMeasurement(2.5f),
            azimuth = null,
            elevation = null,
            elapsedRealtimeNanos = 0L,
        )
        val result = RangingResult.RangingResultPosition(dummyDevice, position)

        val state = mapper.map(result, timestampMs = 0L) as RangingState.Active

        assertThat(state.distanceMeters).isEqualTo(2.5f)
        assertThat(state.azimuthDegrees).isNull()
        assertThat(state.elevationDegrees).isNull()
    }

    @Test
    fun `RangingResultPosition uses provided timestamp`() {
        val position = RangingPosition(null, null, null, 0L)
        val result = RangingResult.RangingResultPosition(dummyDevice, position)

        val state = mapper.map(result, timestampMs = 99999L) as RangingState.Active

        assertThat(state.timestampMs).isEqualTo(99999L)
    }

    @Test
    fun `RangingResultFailure reason 0 maps to unknown failure message`() {
        val result = RangingResult.RangingResultFailure(dummyDevice, reason = 0)

        val state = mapper.map(result) as RangingState.Failure

        assertThat(state.reason).isEqualTo("Unknown failure (code: 0)")
    }

    @Test
    fun `RangingResultFailure reason 1 maps to stopped by request message`() {
        val result = RangingResult.RangingResultFailure(dummyDevice, reason = 1)

        val state = mapper.map(result) as RangingState.Failure

        assertThat(state.reason).isEqualTo("Stopped by request (code: 1)")
    }

    @Test
    fun `RangingResultFailure reason 3 maps to system policy message`() {
        val result = RangingResult.RangingResultFailure(dummyDevice, reason = 3)

        val state = mapper.map(result) as RangingState.Failure

        assertThat(state.reason).isEqualTo("Stopped by system policy (possible country code restriction) (code: 3)")
    }

    @Test
    fun `RangingResultFailure unknown reason code maps to generic failure message`() {
        val result = RangingResult.RangingResultFailure(dummyDevice, reason = 99)

        val state = mapper.map(result) as RangingState.Failure

        assertThat(state.reason).isEqualTo("Failure (code: 99)")
    }
}
