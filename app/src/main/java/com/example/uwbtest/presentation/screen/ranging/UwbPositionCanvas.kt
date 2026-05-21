package com.example.uwbtest.presentation.screen.ranging

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.uwbtest.domain.model.RangingState
import kotlin.math.min

private val COLOR_X_AXIS = Color(0xFFEF5350)
private val COLOR_Y_AXIS = Color(0xFF66BB6A)
private val COLOR_Z_AXIS = Color(0xFF42A5F5)
private val COLOR_GRID = Color(0x33AAAAAA)
private val COLOR_DEVICE = Color(0xFF90CAF9)
private val COLOR_PEER = Color(0xFFFFD54F)
private const val MAX_AXIS_M = 5f
private val RING_RADII = listOf(0.5f, 1f, 2f, 5f)

@Composable
fun UwbPositionCanvas(
    currentActive: RangingState.Active?,
    trail: List<RangingState.Active>,
    modifier: Modifier = Modifier,
) {
    var rotationRad by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, _, rotation ->
                    rotationRad += (rotation * Math.PI / 180f).toFloat()
                }
            },
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = min(size.width, size.height) / 2f / MAX_AXIS_M

        fun WorldPosition.toOffset(): Offset {
            val sp = toScreenPoint(rotationRad, scale)
            return Offset(cx + sp.x, cy + sp.y)
        }

        drawFloorGrid(rotationRad, scale, cx, cy)
        drawAxes(rotationRad, scale, cx, cy)

        // Trail
        trail.takeLast(15).forEachIndexed { idx, entry ->
            val pos = toWorldPosition(
                entry.distanceMeters ?: return@forEachIndexed,
                entry.azimuthDegrees ?: return@forEachIndexed,
                entry.elevationDegrees,
            )
            val alpha = (idx + 1) / 15f * 0.5f
            drawCircle(COLOR_PEER.copy(alpha = alpha), radius = 6f, center = pos.toOffset())
        }

        // Peer dot
        if (currentActive?.distanceMeters != null && currentActive.azimuthDegrees != null) {
            val pos = toWorldPosition(
                currentActive.distanceMeters,
                currentActive.azimuthDegrees,
                currentActive.elevationDegrees,
            )
            val peerOffset = pos.toOffset()
            drawCircle(COLOR_PEER, radius = 10f, center = peerOffset)
            drawCircle(COLOR_PEER.copy(alpha = 0.3f), radius = 20f, center = peerOffset, style = Stroke(2f))

            // distance label
            val label = "%.2fm".format(currentActive.distanceMeters)
            drawLabel(label, peerOffset + Offset(14f, -14f))
        }

        // Device at origin
        drawCircle(COLOR_DEVICE, radius = 10f, center = Offset(cx, cy))
        drawCircle(COLOR_DEVICE.copy(alpha = 0.3f), radius = 18f, center = Offset(cx, cy), style = Stroke(2f))
    }
}

private fun DrawScope.drawFloorGrid(rotationRad: Float, scale: Float, cx: Float, cy: Float) {
    // Concentric rings on XZ plane (y=0)
    val steps = 36
    RING_RADII.forEach { r ->
        val points = (0..steps).map { i ->
            val angle = i * 2 * Math.PI / steps
            val pos = WorldPosition(r * kotlin.math.sin(angle).toFloat(), 0f, r * kotlin.math.cos(angle).toFloat())
            val sp = pos.toScreenPoint(rotationRad, scale)
            Offset(cx + sp.x, cy + sp.y)
        }
        for (i in 0 until steps) {
            drawLine(COLOR_GRID, points[i], points[i + 1], strokeWidth = 1f)
        }
        drawLabel("${r}m", Offset(cx + points[steps / 4].x - cx + cx, cy + points[steps / 4].y - cy + cy), alpha = 0.5f)
    }
}

private fun DrawScope.drawAxes(rotationRad: Float, scale: Float, cx: Float, cy: Float) {
    fun axis(end: WorldPosition, color: Color, label: String) {
        val sp = end.toScreenPoint(rotationRad, scale)
        val endOffset = Offset(cx + sp.x, cy + sp.y)
        drawLine(color, Offset(cx, cy), endOffset, strokeWidth = 2f)
        drawCircle(color, radius = 4f, center = endOffset)
        drawLabel(label, endOffset + Offset(6f, -6f), color)
    }
    axis(WorldPosition(MAX_AXIS_M, 0f, 0f), COLOR_X_AXIS, "X")
    axis(WorldPosition(0f, MAX_AXIS_M, 0f), COLOR_Y_AXIS, "Y")
    axis(WorldPosition(0f, 0f, MAX_AXIS_M), COLOR_Z_AXIS, "Z")
}

private fun DrawScope.drawLabel(
    text: String,
    offset: Offset,
    color: Color = Color.White,
    alpha: Float = 0.8f,
) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.argb(
                (alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt(),
            )
            textSize = 28f
            isAntiAlias = true
        }
        canvas.nativeCanvas.drawText(text, offset.x, offset.y, paint)
    }
}
