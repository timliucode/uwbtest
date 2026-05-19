package com.example.uwbtest.presentation.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeUtils {

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565).also { bmp ->
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bmp.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }
    }

    /**
     * 組裝 QR Code 內容字串（JSON 格式）。
     * Controller 生成時包含 ch + pr；Controlee 生成時只包含 addr。
     * 掃描後 [parseQrContent] 解析並自動填入對應欄位。
     */
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
