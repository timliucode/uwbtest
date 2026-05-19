package com.example.uwbtest.domain.model

/**
 * OOB（Out-of-Band）參數交換的輸入資料，
 * 由 OobExchangeScreen 收集後傳入 StartRangingUseCase。
 *
 * @property peerAddress    對端 UWB 地址（2 bytes）
 * @property channelNumber  UWB 信道號（Controller 提供，Controlee 填入從 OOB 收到的值）
 * @property preambleIndex  前導碼索引（Controller 提供，Controlee 填入從 OOB 收到的值）
 * @property sessionKeyHex  Session Key 十六進位字串（16 chars = 8 bytes），兩端必須一致
 * @property reverseBytes   是否將 peerAddress 位元組反轉（Android 13 byte-order debug 工具）
 */
data class OobParams(
    val peerAddress: ByteArray,
    val channelNumber: Int,
    val preambleIndex: Int,
    val sessionKeyHex: String,
    val reverseBytes: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OobParams) return false
        return peerAddress.contentEquals(other.peerAddress) &&
            channelNumber == other.channelNumber &&
            preambleIndex == other.preambleIndex &&
            sessionKeyHex == other.sessionKeyHex &&
            reverseBytes == other.reverseBytes
    }

    override fun hashCode(): Int {
        var result = peerAddress.contentHashCode()
        result = 31 * result + channelNumber
        result = 31 * result + preambleIndex
        result = 31 * result + sessionKeyHex.hashCode()
        result = 31 * result + reverseBytes.hashCode()
        return result
    }
}
