package com.example.uwbtest.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class UwbRangingCapabilitiesTest {

    private fun fullCapabilities() = UwbRangingCapabilities(
        isDistanceSupported                   = true,
        isAzimuthalAngleSupported             = true,
        isElevationAngleSupported             = false,
        minRangingInterval                    = 200,
        supportedChannels                     = setOf(5, 9),
        supportedNtfConfigs                   = setOf(1, 2),
        supportedConfigIds                    = setOf(1),
        supportedSlotDurations                = setOf(1, 2),
        supportedRangingUpdateRates           = setOf(1, 2, 3),
        isRangingIntervalReconfigureSupported = true,
        isBackgroundRangingSupported          = false,
    )

    @Test
    fun `equality - same values are equal`() {
        assertThat(fullCapabilities()).isEqualTo(fullCapabilities())
    }

    @Test
    fun `equality - differs when isDistanceSupported differs`() {
        val a = fullCapabilities()
        val b = fullCapabilities().copy(isDistanceSupported = false)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `equality - differs when minRangingInterval differs`() {
        val a = fullCapabilities()
        val b = fullCapabilities().copy(minRangingInterval = 100)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `equality - differs when supportedChannels differs`() {
        val a = fullCapabilities()
        val b = fullCapabilities().copy(supportedChannels = setOf(9))
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `supportedChannels - empty set is valid`() {
        val caps = fullCapabilities().copy(supportedChannels = emptySet())
        assertThat(caps.supportedChannels).isEmpty()
    }

    @Test
    fun `isBackgroundRangingSupported - stores provided value`() {
        val caps = fullCapabilities().copy(isBackgroundRangingSupported = true)
        assertThat(caps.isBackgroundRangingSupported).isTrue()
    }
}
