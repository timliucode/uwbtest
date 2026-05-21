package com.example.uwbtest.presentation.screen.logviewer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uwbtest.domain.model.LogEntry
import com.example.uwbtest.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogViewerViewModel @Inject constructor() : ViewModel() {

    enum class LevelFilter(val minLevel: Int, val labelKey: String) {
        All(Log.VERBOSE, "ALL"),
        Debug(Log.DEBUG, "D+"),
        Info(Log.INFO, "I+"),
        Warn(Log.WARN, "W+"),
        Error(Log.ERROR, "E"),
    }

    data class UiState(
        val entries: List<LogEntry> = emptyList(),
        val filter: LevelFilter = LevelFilter.All,
        val searchQuery: String = "",
        val isCapturingLogcat: Boolean = false,
        val logcatText: String? = null,
    ) {
        val filtered: List<LogEntry>
            get() = entries.filter { entry ->
                entry.level >= filter.minLevel &&
                    (searchQuery.isBlank() ||
                        entry.tag.contains(searchQuery, ignoreCase = true) ||
                        entry.message.contains(searchQuery, ignoreCase = true))
            }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(entries = AppLogger.getEntries()) }
    }

    fun onFilterChanged(filter: LevelFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearLogs() {
        AppLogger.clear()
        _uiState.update { it.copy(entries = emptyList(), logcatText = null) }
    }

    fun captureLogcat() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCapturingLogcat = true) }
            val text = AppLogger.captureLogcat()
            _uiState.update { it.copy(isCapturingLogcat = false, logcatText = text) }
        }
    }

    fun getShareText(): String {
        val logcatSection = _uiState.value.logcatText?.let {
            "\n\n=== Logcat ===\n$it"
        } ?: ""
        return AppLogger.getFormattedReport() + logcatSection
    }
}
