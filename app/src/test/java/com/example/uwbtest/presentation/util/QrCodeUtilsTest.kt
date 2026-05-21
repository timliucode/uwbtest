package com.example.uwbtest.presentation.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class QrCodeUtilsTest {

    @Test
    fun `buildQrContent - controller includes addr, ch, pr, key`() {
        val result = QrCodeUtils.buildQrContent(addr = "A1:B2", ch = 9, pr = 11, key = "DEADBEEFDEADBEEF")
        assertThat(result).contains(""""addr":"A1:B2"""")
        assertThat(result).contains(""""ch":9""")
        assertThat(result).contains(""""pr":11""")
        assertThat(result).contains(""""key":"DEADBEEFDEADBEEF"""")
    }

    @Test
    fun `buildQrContent - controlee omits ch and pr when null`() {
        val result = QrCodeUtils.buildQrContent(addr = "C3:D4", ch = null, pr = null, key = "DEADBEEFDEADBEEF")
        assertThat(result).contains(""""addr":"C3:D4"""")
        assertThat(result).doesNotContain(""""ch"""")
        assertThat(result).doesNotContain(""""pr"""")
    }

    @Test
    fun `buildQrContent - omits key when empty`() {
        val result = QrCodeUtils.buildQrContent(addr = "A1:B2", ch = null, pr = null, key = "")
        assertThat(result).doesNotContain(""""key"""")
    }

    @Test
    fun `parseQrContent - full controller payload returns all fields`() {
        val raw = """{"addr":"A1:B2","ch":9,"pr":11,"key":"DEADBEEFDEADBEEF"}"""
        val map = QrCodeUtils.parseQrContent(raw)
        assertThat(map).isNotNull()
        assertThat(map!!["addr"]).isEqualTo("A1:B2")
        assertThat(map["ch"]).isEqualTo("9")
        assertThat(map["pr"]).isEqualTo("11")
        assertThat(map["key"]).isEqualTo("DEADBEEFDEADBEEF")
    }

    @Test
    fun `parseQrContent - controlee payload with addr only`() {
        val raw = """{"addr":"C3:D4","key":"ABCDEF0123456789"}"""
        val map = QrCodeUtils.parseQrContent(raw)
        assertThat(map).isNotNull()
        assertThat(map!!["addr"]).isEqualTo("C3:D4")
        assertThat(map["ch"]).isNull()
        assertThat(map["pr"]).isNull()
    }

    @Test
    fun `parseQrContent - missing addr returns null`() {
        val raw = """{"ch":9,"pr":11}"""
        val map = QrCodeUtils.parseQrContent(raw)
        assertThat(map).isNull()
    }

    @Test
    fun `parseQrContent - empty string returns null`() {
        val map = QrCodeUtils.parseQrContent("")
        assertThat(map).isNull()
    }

    @Test
    fun `parseQrContent - roundtrip buildQrContent then parse returns same values`() {
        val original = QrCodeUtils.buildQrContent(addr = "A1:B2", ch = 9, pr = 11, key = "0102030405060708")
        val parsed = QrCodeUtils.parseQrContent(original)
        assertThat(parsed).isNotNull()
        assertThat(parsed!!["addr"]).isEqualTo("A1:B2")
        assertThat(parsed["ch"]).isEqualTo("9")
        assertThat(parsed["pr"]).isEqualTo("11")
        assertThat(parsed["key"]).isEqualTo("0102030405060708")
    }
}
