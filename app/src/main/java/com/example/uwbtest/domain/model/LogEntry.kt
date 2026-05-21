package com.example.uwbtest.domain.model

/**
 * Single log entry captured by [com.example.uwbtest.util.AppLogger].
 *
 * @property timestampMs System.currentTimeMillis() at capture time
 * @property level       Android log level constant (Log.DEBUG, Log.INFO, etc.)
 * @property tag         Log tag string
 * @property message     Log message
 * @property throwable   Optional throwable for error entries
 */
data class LogEntry(
    val timestampMs: Long,
    val level: Int,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
)
