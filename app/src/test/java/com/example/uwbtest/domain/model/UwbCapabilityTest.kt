package com.example.uwbtest.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

private fun sampleRangingCapabilities() = UwbRangingCapabilities(
    isDistanceSupported                   = true,
    isAzimuthalAngleSupported             = true,
    isElevationAngleSupported             = false,
    minRangingInterval                    = 200,
    supportedChannels                     = setOf(5, 9),
    supportedNtfConfigs                   = setOf(1),
    supportedConfigIds                    = setOf(1),
    supportedSlotDurations                = setOf(1),
    supportedRangingUpdateRates           = setOf(1),
    isRangingIntervalReconfigureSupported = true,
    isBackgroundRangingSupported          = false,
)

class UwbCapabilityTest {

    @Test
    fun `canProceed - true when both hardware and software ready`() {
        val c = UwbCapability(hardwarePresent = true, isAvailable = true)
        assertThat(c.canProceed).isTrue()
    }

    @Test
    fun `canProceed - false when no hardware`() {
        val c = UwbCapability(hardwarePresent = false, isAvailable = true)
        assertThat(c.canProceed).isFalse()
    }

    @Test
    fun `canProceed - false when software unavailable`() {
        val c = UwbCapability(hardwarePresent = true, isAvailable = false)
        assertThat(c.canProceed).isFalse()
    }

    @Test
    fun `canProceed - false when both false`() {
        val c = UwbCapability(hardwarePresent = false, isAvailable = false)
        assertThat(c.canProceed).isFalse()
    }

    @Test
    fun `unavailableReason - null by default`() {
        val c = UwbCapability(hardwarePresent = true, isAvailable = true)
        assertThat(c.unavailableReason).isNull()
    }

    @Test
    fun `unavailableReason - stores provided reason`() {
        val c = UwbCapability(hardwarePresent = true, isAvailable = false, unavailableReason = "country code locked")
        assertThat(c.unavailableReason).isEqualTo("country code locked")
    }

    @Test
    fun `isAndroid13OrLower - defaults to false`() {
        val c = UwbCapability(hardwarePresent = true, isAvailable = true)
        assertThat(c.isAndroid13OrLower).isFalse()
    }

    @Test
    fun `isAndroid13OrLower - stores true when set`() {
        val c = UwbCapability(hardwarePresent = true, isAvailable = true, isAndroid13OrLower = true)
        assertThat(c.isAndroid13OrLower).isTrue()
    }

    @Test
    fun `equality - same values are equal`() {
        val a = UwbCapability(hardwarePresent = true, isAvailable = true, isAndroid13OrLower = false)
        val b = UwbCapability(hardwarePresent = true, isAvailable = true, isAndroid13OrLower = false)
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `rangingCapabilities - null by default`() {
        val c = UwbCapability(hardwarePresent = true, isAvailable = true)
        assertThat(c.rangingCapabilities).isNull()
    }

    @Test
    fun `rangingCapabilities - stores provided value`() {
        val caps = sampleRangingCapabilities()
        val c = UwbCapability(hardwarePresent = true, isAvailable = true, rangingCapabilities = caps)
        assertThat(c.rangingCapabilities).isEqualTo(caps)
    }

    @Test
    fun `canProceed - not affected by rangingCapabilities presence`() {
        val withCaps = UwbCapability(
            hardwarePresent = true,
            isAvailable = true,
            rangingCapabilities = sampleRangingCapabilities(),
        )
        val withoutCaps = UwbCapability(hardwarePresent = true, isAvailable = true)
        assertThat(withCaps.canProceed).isTrue()
        assertThat(withoutCaps.canProceed).isTrue()
    }
}
