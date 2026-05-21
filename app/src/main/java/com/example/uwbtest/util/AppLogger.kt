package com.example.uwbtest.util

import android.os.Process
import android.util.Log
import com.example.uwbtest.domain.model.LogEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * In-process log buffer that wraps [android.util.Log].
 *
 * Keeps the last [MAX_ENTRIES] entries in memory. Replaces direct Log.* calls
 * so the LogViewer screen can display them without requiring ADB access.
 */
object AppLogger {

    private const val MAX_ENTRIES = 500

    private val buffer: ArrayDeque<LogEntry> = ArrayDeque(MAX_ENTRIES + 1)
    private val lock = Any()

    // DateTimeFormatter is immutable and thread-safe (unlike SimpleDateFormat).
    private val timestampFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    fun v(tag: String, message: String) = log(Log.VERBOSE, tag, message)
    fun d(tag: String, message: String) = log(Log.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(Log.INFO, tag, message)
    fun w(tag: String, message: String, throwable: Throwable? = null) = log(Log.WARN, tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(Log.ERROR, tag, message, throwable)

    fun getEntries(): List<LogEntry> = synchronized(lock) { buffer.toList() }

    fun clear() = synchronized(lock) { buffer.clear() }

    /** Formatted text for clipboard / share sheet. */
    fun getFormattedReport(): String = buildString {
        appendLine("=== UWB Tester Log Export ===")
        appendLine("PID: ${Process.myPid()}")
        appendLine()
        getEntries().forEach { entry ->
            val level = levelChar(entry.level)
            val time = Instant.ofEpochMilli(entry.timestampMs)
                .atZone(ZoneId.systemDefault())
                .format(timestampFormatter)
            appendLine("$time $level/${entry.tag}: ${entry.message}")
            entry.throwable?.let { appendLine("  ${it.stackTraceToString()}") }
        }
        append("=============================")
    }

    /**
     * Captures recent logcat output for this process.
     * Safe to call from any coroutine context — internally dispatches to [Dispatchers.IO].
     */
    suspend fun captureLogcat(): String = withContext(Dispatchers.IO) {
        try {
            val pid = Process.myPid().toString()
            val process = ProcessBuilder("logcat", "-d", "-t", "300", "--pid", pid)
                .redirectErrorStream(true)
                .start()
            try {
                process.inputStream.bufferedReader().use { it.readText() }.also {
                    process.waitFor(10, TimeUnit.SECONDS)
                }
            } finally {
                process.destroy()
            }
        } catch (e: Exception) {
            "Failed to capture logcat: ${e.message}"
        }
    }

    private fun log(level: Int, tag: String, message: String, throwable: Throwable? = null) {
        when (level) {
            Log.VERBOSE -> Log.v(tag, message, throwable)
            Log.DEBUG   -> Log.d(tag, message, throwable)
            Log.INFO    -> Log.i(tag, message, throwable)
            Log.WARN    -> Log.w(tag, message, throwable)
            Log.ERROR   -> Log.e(tag, message, throwable)
        }
        val entry = LogEntry(
            timestampMs = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable,
        )
        synchronized(lock) {
            buffer.addLast(entry)
            if (buffer.size > MAX_ENTRIES) buffer.removeFirst()
        }
    }

    private fun levelChar(level: Int) = when (level) {
        Log.VERBOSE -> "V"
        Log.DEBUG   -> "D"
        Log.INFO    -> "I"
        Log.WARN    -> "W"
        Log.ERROR   -> "E"
        else        -> "?"
    }
}
