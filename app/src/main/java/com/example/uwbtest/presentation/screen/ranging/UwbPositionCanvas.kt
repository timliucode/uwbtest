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
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.uwbtest.domain.model.RangingState
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val COLOR_X_AXIS = Color(0xFFEF5350)
private val COLOR_Y_AXIS = Color(0xFF66BB6A)
private val COLOR_Z_AXIS = Color(0xFF42A5F5)
private val COLOR_GRID = Color(0x33AAAAAA)
private val COLOR_DEVICE = Color(0xFF90CAF9)
private val COLOR_PEER = Color(0xFFFFD54F)
private const val MAX_AXIS_M = 5f
private val RING_RADII = listOf(0.5f, 1f, 2f, 5f)
private const val GRID_STEPS = 36

/**
 * Pre-computed ring segments in unit-space (scale=1, centred at origin).
 * Stored as flat [start, end, start, end, …] pairs for use with drawPoints(PointMode.Lines).
 */
private data class RingCache(
    val segmentsByRing: List<List<Offset>>,   // unit-space; apply *scale + (cx,cy) before drawing
    val labelPosByRing: List<Offset>,          // unit-space label positions
)

private fun buildRingCache(rotationRad: Float): RingCache {
    val segments = RING_RADII.map { r ->
        buildList {
            repeat(GRID_STEPS) { i ->
                val a0 = i * 2 * Math.PI / GRID_STEPS
                val a1 = (i + 1) * 2 * Math.PI / GRID_STEPS
                val p0 = WorldPosition(r * sin(a0).toFloat(), 0f, r * cos(a0).toFloat())
                    .toScreenPoint(rotationRad, 1f)
                val p1 = WorldPosition(r * sin(a1).toFloat(), 0f, r * cos(a1).toFloat())
                    .toScreenPoint(rotationRad, 1f)
                add(Offset(p0.x, p0.y))
                add(Offset(p1.x, p1.y))
            }
        }
    }
    val labelPositions = RING_RADII.map { r ->
        val sp = WorldPosition(r * sin(Math.PI / 2).toFloat(), 0f, r * cos(Math.PI / 2).toFloat())
            .toScreenPoint(rotationRad, 1f)
        Offset(sp.x, sp.y)
    }
    return RingCache(segments, labelPositions)
}

@Composable
fun UwbPositionCanvas(
    currentActive: RangingState.Active?,
    trail: List<RangingState.Active>,
    modifier: Modifier = Modifier,
) {
    var rotationRad by remember { mutableFloatStateOf(0f) }
    val labelPaint = remember {
        android.graphics.Paint().apply { textSize = 28f; isAntiAlias = true }
    }

    // Quantise to 2° buckets — cache only invalidates when rotation changes noticeably.
    val quantizedRot = remember(rotationRad) {
        ((rotationRad * 180f / Math.PI.toFloat()).roundToInt() / 2 * 2) *
            Math.PI.toFloat() / 180f
    }
    val ringCache = remember(quantizedRot) { buildRingCache(quantizedRot) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, _, rotation ->
                    rotationRad = (rotationRad + (rotation * Math.PI / 180f).toFloat())
                        .mod(2f * Math.PI.toFloat())
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

        drawFloorGrid(ringCache, scale, cx, cy, labelPaint)
        drawAxes(rotationRad, scale, cx, cy, labelPaint)

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

        // Peer dot + label
        if (currentActive?.distanceMeters != null && currentActive.azimuthDegrees != null) {
            val pos = toWorldPosition(
                currentActive.distanceMeters,
                currentActive.azimuthDegrees,
                currentActive.elevationDegrees,
            )
            val peerOffset = pos.toOffset()
            drawCircle(COLOR_PEER, radius = 10f, center = peerOffset)
            drawCircle(COLOR_PEER.copy(alpha = 0.3f), radius = 20f, center = peerOffset, style = Stroke(2f))

            drawIntoCanvas { canvas ->
                labelPaint.color = android.graphics.Color.argb(204, 255, 213, 79)
                canvas.nativeCanvas.drawText(
                    "%.2fm".format(currentActive.distanceMeters),
                    peerOffset.x + 14f,
                    peerOffset.y - 14f,
                    labelPaint,
                )
            }
        }

        // Device at origin
        drawCircle(COLOR_DEVICE, radius = 10f, center = Offset(cx, cy))
        drawCircle(COLOR_DEVICE.copy(alpha = 0.3f), radius = 18f, center = Offset(cx, cy), style = Stroke(2f))
    }
}

private fun DrawScope.drawFloorGrid(
    cache: RingCache,
    scale: Float,
    cx: Float,
    cy: Float,
    paint: android.graphics.Paint,
) {
    drawIntoCanvas { canvas ->
        paint.color = android.graphics.Color.argb(128, 170, 170, 170)

        cache.segmentsByRing.forEachIndexed { i, segments ->
            // Scale and translate unit-space points to screen-space
            val screenPoints = segments.map { Offset(it.x * scale + cx, it.y * scale + cy) }
            drawPoints(screenPoints, PointMode.Lines, COLOR_GRID, strokeWidth = 1f)

            // Label
            val lp = cache.labelPosByRing[i]
            canvas.nativeCanvas.drawText(
                "${RING_RADII[i]}m",
                lp.x * scale + cx + 4f,
                lp.y * scale + cy - 4f,
                paint,
            )
        }
    }
}

private fun DrawScope.drawAxes(
    rotationRad: Float,
    scale: Float,
    cx: Float,
    cy: Float,
    paint: android.graphics.Paint,
) {
    data class AxisDef(val end: WorldPosition, val color: Color, val label: String)

    val axes = listOf(
        AxisDef(WorldPosition(MAX_AXIS_M, 0f, 0f), COLOR_X_AXIS, "X"),
        AxisDef(WorldPosition(0f, MAX_AXIS_M, 0f), COLOR_Y_AXIS, "Y"),
        AxisDef(WorldPosition(0f, 0f, MAX_AXIS_M), COLOR_Z_AXIS, "Z"),
    )

    drawIntoCanvas { canvas ->
        axes.forEach { (end, color, label) ->
            val sp = end.toScreenPoint(rotationRad, scale)
            val endOffset = Offset(cx + sp.x, cy + sp.y)
            drawLine(color, Offset(cx, cy), endOffset, strokeWidth = 2f)
            drawCircle(color, radius = 4f, center = endOffset)
            paint.color = android.graphics.Color.argb(
                204,
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt(),
            )
            canvas.nativeCanvas.drawText(label, endOffset.x + 6f, endOffset.y - 6f, paint)
        }
    }
}
