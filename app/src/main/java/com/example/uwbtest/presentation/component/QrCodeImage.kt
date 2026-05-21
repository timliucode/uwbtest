package com.example.uwbtest.presentation.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.uwbtest.presentation.util.QrCodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun QrCodeImage(content: String, modifier: Modifier = Modifier) {
    val fg = MaterialTheme.colorScheme.onBackground
    val bg = MaterialTheme.colorScheme.background
    val colorFilter = remember(fg, bg) { themeColorFilter(fg, bg) }

    val bitmap by produceState<Bitmap?>(initialValue = null, content) {
        value = withContext(Dispatchers.Default) {
            runCatching { QrCodeUtils.generateQrBitmap(content) }.getOrNull()
        }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "QR Code",
            colorFilter = colorFilter,
            modifier = modifier.size(200.dp),
        )
    }
}

// 把 QR code 的黑模組 (0) 映射到 onBackground，白背景 (255) 映射到 background
private fun themeColorFilter(fg: Color, bg: Color): ColorFilter =
    ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        bg.red - fg.red,         0f, 0f, 0f, fg.red   * 255f,
        0f, bg.green - fg.green, 0f, 0f, fg.green * 255f,
        0f, 0f, bg.blue - fg.blue, 0f, fg.blue  * 255f,
        0f, 0f, 0f,              1f, 0f,
    )))
