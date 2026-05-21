package com.example.uwbtest.presentation.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import qrcode.QRCode

object QrCodeUtils {

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val raw = QRCode.ofSquares()
            .build(content)
            .render()
            .nativeImage() as Bitmap
        return if (raw.width != sizePx) {
            Bitmap.createScaledBitmap(raw, sizePx, sizePx, true)
        } else raw
    }

    fun buildQrContent(addr: String, ch: Int?, pr: Int?, key: String): String = buildString {
        append("""{"addr":"$addr"""")
        if (ch != null) append(""","ch":$ch""")
        if (pr != null) append(""","pr":$pr""")
        if (key.isNotEmpty()) append(""","key":"$key"""")
        append("}")
    }

    fun parseQrContent(raw: String): Map<String, String>? = runCatching {
        val result = mutableMapOf<String, String>()
        Regex(""""addr"\s*:\s*"([^"]+)"""").find(raw)?.groupValues?.get(1)?.let { result["addr"] = it }
        Regex(""""ch"\s*:\s*(\d+)""").find(raw)?.groupValues?.get(1)?.let { result["ch"] = it }
        Regex(""""pr"\s*:\s*(\d+)""").find(raw)?.groupValues?.get(1)?.let { result["pr"] = it }
        Regex(""""key"\s*:\s*"([^"]+)"""").find(raw)?.groupValues?.get(1)?.let { result["key"] = it }
        result.takeIf { it.containsKey("addr") }
    }.getOrNull()
}
