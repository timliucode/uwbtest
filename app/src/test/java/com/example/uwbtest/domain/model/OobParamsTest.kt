package com.example.uwbtest.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OobParamsTest {

    private fun params(
        address: ByteArray = byteArrayOf(0xA1.toByte(), 0xB2.toByte()),
        channel: Int = 9,
        preamble: Int = 10,
        key: String = "0102030405060708",
        reverseBytes: Boolean = false,
    ) = OobParams(address, channel, preamble, key, reverseBytes)

    @Test
    fun `equals - same content all fields returns true`() {
        val a = params()
        val b = params()
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `equals - different peer address returns false`() {
        val a = params(address = byteArrayOf(0x01, 0x02))
        val b = params(address = byteArrayOf(0x01, 0x03))
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `equals - different session key returns false`() {
        val a = params(key = "0102030405060708")
        val b = params(key = "0807060504030201")
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `equals - different reverseBytes returns false`() {
        val a = params(reverseBytes = true)
        val b = params(reverseBytes = false)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `equals - different channel returns false`() {
        val a = params(channel = 9)
        val b = params(channel = 10)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `hashCode - consistent with equals`() {
        val a = params()
        val b = params()
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `reverseBytes - defaults to false`() {
        val p = OobParams(byteArrayOf(0x01, 0x02), 9, 10, "0102030405060708")
        assertThat(p.reverseBytes).isFalse()
    }

    @Test
    fun `equals returns false for non-OobParams type`() {
        val a = params()
        @Suppress("ReplaceCallWithBinaryOperator")
        assertThat(a.equals("other")).isFalse()
    }

    @Test
    fun `equals is reflexive`() {
        val a = params()
        @Suppress("ReplaceCallWithBinaryOperator")
        assertThat(a.equals(a)).isTrue()
    }
}
