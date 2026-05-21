package com.example.uwbtest.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class UwbDeviceInfoTest {

    private fun info(
        address: ByteArray = byteArrayOf(0xA1.toByte(), 0xB2.toByte()),
        role: UwbRole = UwbRole.Controller,
        channel: Int? = 9,
        preamble: Int? = 10,
    ) = UwbDeviceInfo(address, role, channel, preamble)

    @Test
    fun `equals - same byte content returns true`() {
        val a = info(byteArrayOf(0xA1.toByte(), 0xB2.toByte()))
        val b = info(byteArrayOf(0xA1.toByte(), 0xB2.toByte()))
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `equals - different byte content returns false`() {
        val a = info(byteArrayOf(0x01))
        val b = info(byteArrayOf(0x02))
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `equals - different role returns false`() {
        val a = info(role = UwbRole.Controller)
        val b = info(role = UwbRole.Controlee)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `equals - different channelNumber returns false`() {
        val a = info(channel = 9)
        val b = info(channel = 10)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `equals - different preambleIndex returns false`() {
        val a = info(preamble = 10)
        val b = info(preamble = 11)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `hashCode - same content produces same hash`() {
        val a = info(byteArrayOf(0xA1.toByte(), 0xB2.toByte()))
        val b = info(byteArrayOf(0xA1.toByte(), 0xB2.toByte()))
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `hashCode - different content produces different hash`() {
        val a = info(byteArrayOf(0x01))
        val b = info(byteArrayOf(0xFF.toByte()))
        assertThat(a.hashCode()).isNotEqualTo(b.hashCode())
    }

    @Test
    fun `localAddressHex - formats two bytes correctly`() {
        val d = info(byteArrayOf(0xA1.toByte(), 0xB2.toByte()))
        assertThat(d.localAddressHex).isEqualTo("A1:B2")
    }

    @Test
    fun `localAddressHex - single byte with leading zero`() {
        val d = info(byteArrayOf(0x0F))
        assertThat(d.localAddressHex).isEqualTo("0F")
    }

    @Test
    fun `controller has non-null channel and preamble`() {
        val d = info(role = UwbRole.Controller, channel = 9, preamble = 10)
        assertThat(d.channelNumber).isEqualTo(9)
        assertThat(d.preambleIndex).isEqualTo(10)
    }

    @Test
    fun `controlee defaults channel and preamble to null`() {
        val d = UwbDeviceInfo(byteArrayOf(0x01, 0x02), UwbRole.Controlee)
        assertThat(d.channelNumber).isNull()
        assertThat(d.preambleIndex).isNull()
    }

    @Test
    fun `equals is reflexive`() {
        val a = info()
        @Suppress("ReplaceCallWithBinaryOperator")
        assertThat(a.equals(a)).isTrue()
    }

    @Test
    fun `equals returns false for non-UwbDeviceInfo type`() {
        val a = info()
        @Suppress("ReplaceCallWithBinaryOperator")
        assertThat(a.equals("string")).isFalse()
    }
}
