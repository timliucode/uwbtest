package com.example.uwbtest.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

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
}
