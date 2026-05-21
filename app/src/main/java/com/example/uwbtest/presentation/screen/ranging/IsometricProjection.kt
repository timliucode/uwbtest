package com.example.uwbtest.presentation.screen.ranging

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class WorldPosition(val x: Float, val y: Float, val z: Float)
data class ScreenPoint(val x: Float, val y: Float)

private val ISO_COS = cos(30.0 * PI / 180.0).toFloat()
private val ISO_SIN = sin(30.0 * PI / 180.0).toFloat()

fun toWorldPosition(
    distanceM: Float,
    azimuthDeg: Float,
    elevationDeg: Float? = null,
): WorldPosition {
    val az = azimuthDeg * PI.toFloat() / 180f
    val el = (elevationDeg ?: 0f) * PI.toFloat() / 180f
    return WorldPosition(
        x = distanceM * cos(el) * sin(az),
        y = distanceM * sin(el),
        z = distanceM * cos(el) * cos(az),
    )
}

fun WorldPosition.toScreenPoint(
    rotationRad: Float = 0f,
    scale: Float = 1f,
): ScreenPoint {
    val cosR = cos(rotationRad)
    val sinR = sin(rotationRad)
    val rx = (x * cosR - z * sinR)
    val rz = (x * sinR + z * cosR)
    return ScreenPoint(
        x = (rx - rz) * ISO_COS * scale,
        y = (rx + rz) * ISO_SIN * scale - y * scale,
    )
}
