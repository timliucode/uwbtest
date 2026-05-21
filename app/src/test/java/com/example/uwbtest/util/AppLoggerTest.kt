package com.example.uwbtest.util

import android.util.Log
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AppLoggerTest {

    @BeforeEach
    fun setUp() {
        AppLogger.clear()
    }

    @Test
    fun `d - adds DEBUG entry to buffer`() {
        AppLogger.d("TAG", "debug message")

        val entries = AppLogger.getEntries()
        assertThat(entries).hasSize(1)
        assertThat(entries[0].level).isEqualTo(Log.DEBUG)
        assertThat(entries[0].tag).isEqualTo("TAG")
        assertThat(entries[0].message).isEqualTo("debug message")
    }

    @Test
    fun `e - stores throwable reference`() {
        val error = RuntimeException("oops")
        AppLogger.e("TAG", "error", error)

        val entry = AppLogger.getEntries().first()
        assertThat(entry.throwable).isSameInstanceAs(error)
    }

    @Test
    fun `buffer is capped at 500 entries`() {
        repeat(520) { i -> AppLogger.d("T", "msg $i") }

        assertThat(AppLogger.getEntries()).hasSize(500)
    }

    @Test
    fun `buffer retains the most recent entries when full`() {
        repeat(520) { i -> AppLogger.d("T", "msg $i") }

        val first = AppLogger.getEntries().first().message
        assertThat(first).isEqualTo("msg 20") // oldest 20 are dropped
    }

    @Test
    fun `clear - empties the buffer`() {
        AppLogger.d("T", "hello")
        AppLogger.clear()

        assertThat(AppLogger.getEntries()).isEmpty()
    }

    @Test
    fun `getFormattedReport - includes all entries`() {
        AppLogger.i("UWB", "session started")
        AppLogger.e("UWB", "session failed")

        val report = AppLogger.getFormattedReport()
        assertThat(report).contains("session started")
        assertThat(report).contains("session failed")
    }

    @Test
    fun `getEntries - returns snapshot not live reference`() {
        AppLogger.d("T", "before")
        val snapshot = AppLogger.getEntries()
        AppLogger.d("T", "after")

        assertThat(snapshot).hasSize(1) // snapshot is frozen
    }
}
