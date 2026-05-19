package com.example.uwbtest.presentation.screen.oob

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uwbtest.domain.model.OobParams
import com.example.uwbtest.domain.model.UwbDeviceInfo
import com.example.uwbtest.domain.usecase.GetLocalAddressUseCase
import com.example.uwbtest.presentation.util.QrCodeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * OobExchangeScreen 的 ViewModel。
 *
 * 職責：
 *  1. 從 SavedStateHandle 讀取導航參數 isController
 *  2. 取得本機地址（並快取 UwbScope）
 *  3. 接收使用者輸入的對端參數
 *  4. 驗證輸入後組裝 OobParams，供 RangingViewModel 使用
 */
@HiltViewModel
class OobExchangeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getLocalAddress: GetLocalAddressUseCase,
) : ViewModel() {

    val isController: Boolean = checkNotNull(savedStateHandle["isController"])

    // ── UI State ───────────────────────────────────────────────
    data class UiState(
        val isLoadingAddress: Boolean = true,
        val localDeviceInfo: UwbDeviceInfo? = null,
        val addressError: String? = null,

        // 使用者輸入
        val peerAddressHex: String = "",
        val channelNumber: String = "9",      // Controller 預設值
        val preambleIndex: String = "10",     // Controller 預設值
        val sessionKeyHex: String = "0102030405060708",
        val reverseBytes: Boolean = false,

        // 驗證結果
        val peerAddressError: String? = null,
        val sessionKeyError: String? = null,
    ) {
        val canProceed: Boolean
            get() = localDeviceInfo != null &&
                peerAddressHex.isValidHexAddress() &&
                sessionKeyHex.isValidSessionKey() &&
                channelNumber.toIntOrNull() != null &&
                preambleIndex.toIntOrNull() != null &&
                peerAddressError == null &&
                sessionKeyError == null
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadLocalAddress()
    }

    // ── Public Actions ─────────────────────────────────────────

    fun onPeerAddressChanged(value: String) {
        val cleaned = value.uppercase().filter { it.isHexChar() || it == ':' }
        _uiState.update {
            it.copy(
                peerAddressHex = cleaned,
                peerAddressError = if (cleaned.isEmpty() || cleaned.isValidHexAddress()) null
                    else "格式應為 XX:XX（2 bytes hex）",
            )
        }
    }

    fun onChannelChanged(value: String) {
        _uiState.update { it.copy(channelNumber = value.filter(Char::isDigit)) }
    }

    fun onPreambleChanged(value: String) {
        _uiState.update { it.copy(preambleIndex = value.filter(Char::isDigit)) }
    }

    fun onSessionKeyChanged(value: String) {
        val cleaned = value.uppercase().filter(Char::isHexChar)
        _uiState.update {
            it.copy(
                sessionKeyHex = cleaned,
                sessionKeyError = when {
                    cleaned.isEmpty() -> null
                    cleaned.length != 16 -> "Session Key 必須為 16 個十六進位字元（= 8 bytes）"
                    else -> null
                },
            )
        }
    }

    fun onReverseBytesChanged(value: Boolean) {
        _uiState.update { it.copy(reverseBytes = value) }
    }

    fun onQrScanned(raw: String) {
        val map = QrCodeUtils.parseQrContent(raw) ?: return
        map["addr"]?.let { onPeerAddressChanged(it) }
        map["ch"]?.let { onChannelChanged(it) }
        map["pr"]?.let { onPreambleChanged(it) }
        map["key"]?.let { onSessionKeyChanged(it) }
    }

    /** 組裝 OobParams 並儲存到 SavedStateHandle（供 RangingViewModel 取用） */
    fun buildOobParams(): OobParams {
        val state = _uiState.value
        val peerAddress = state.peerAddressHex
            .split(":")
            .map { it.toInt(16).toByte() }
            .toByteArray()
        return OobParams(
            peerAddress = peerAddress,
            channelNumber = state.channelNumber.toInt(),
            preambleIndex = state.preambleIndex.toInt(),
            sessionKeyHex = state.sessionKeyHex,
            reverseBytes = state.reverseBytes,
        )
    }

    // ── Private ────────────────────────────────────────────────

    private fun loadLocalAddress() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAddress = true) }
            getLocalAddress(isController = isController)
                .onSuccess { info ->
                    _uiState.update {
                        it.copy(
                            isLoadingAddress = false,
                            localDeviceInfo = info,
                            // Controller：用 scope 提供的 channel/preamble 覆蓋預設值
                            channelNumber = info.channelNumber?.toString() ?: it.channelNumber,
                            preambleIndex = info.preambleIndex?.toString() ?: it.preambleIndex,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingAddress = false,
                            addressError = "無法取得本機地址：${e.message}",
                        )
                    }
                }
        }
    }
}

// ── Extension helpers ──────────────────────────────────────────

private fun Char.isHexChar() = this in '0'..'9' || this in 'A'..'F' || this in 'a'..'f'

/**
 * 驗證 UWB 地址格式：
 *   有效：2 bytes，格式 "XX" 或 "XX:XX"（1-2 bytes for UwbAddress）
 */
private fun String.isValidHexAddress(): Boolean {
    val parts = split(":")
    return parts.size in 1..2 && parts.all { it.length == 2 && it.all(Char::isHexChar) }
}

private fun String.isValidSessionKey(): Boolean = length == 16 && all(Char::isHexChar)
