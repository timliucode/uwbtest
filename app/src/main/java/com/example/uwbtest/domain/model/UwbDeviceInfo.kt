package com.example.uwbtest.domain.model

/**
 * 本機 UWB 裝置資訊，由 UwbManagerWrapper 取得後傳回給 domain/presentation 層使用。
 *
 * @property localAddress   本機 UWB 地址（2 bytes），需透過 OOB 傳給對端
 * @property role           本機在此 ranging session 中的角色
 * @property channelNumber  只有 Controller 才有；需透過 OOB 傳給 Controlee
 * @property preambleIndex  只有 Controller 才有；需透過 OOB 傳給 Controlee
 */
data class UwbDeviceInfo(
    val localAddress: ByteArray,
    val role: UwbRole,
    val channelNumber: Int? = null,
    val preambleIndex: Int? = null,
) {
    /** 地址的十六進位字串表示，方便顯示與複製（例："A1:B2"） */
    val localAddressHex: String
        get() = localAddress.joinToString(":") { "%02X".format(it) }

    // ByteArray 需要手動實作 equals/hashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UwbDeviceInfo) return false
        return localAddress.contentEquals(other.localAddress) &&
            role == other.role &&
            channelNumber == other.channelNumber &&
            preambleIndex == other.preambleIndex
    }

    override fun hashCode(): Int {
        var result = localAddress.contentHashCode()
        result = 31 * result + role.hashCode()
        result = 31 * result + (channelNumber ?: 0)
        result = 31 * result + (preambleIndex ?: 0)
        return result
    }
}

enum class UwbRole { Controller, Controlee }
