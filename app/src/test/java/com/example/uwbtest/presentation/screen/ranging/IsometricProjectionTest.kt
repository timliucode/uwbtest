package com.example.uwbtest.presentation.screen.ranging

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.sqrt

class IsometricProjectionTest {

    private fun assertNear(actual: Float, expected: Float, delta: Float = 0.01f) {
        assertThat(abs(actual - expected)).isLessThan(delta)
    }

    @Test
    fun `toWorldPosition - forward az0 el0 maps to positive Z`() {
        val pos = toWorldPosition(5f, azimuthDeg = 0f, elevationDeg = 0f)
        assertNear(pos.x, 0f)
        assertNear(pos.y, 0f)
        assertNear(pos.z, 5f)
    }

    @Test
    fun `toWorldPosition - right az90 el0 maps to positive X`() {
        val pos = toWorldPosition(5f, azimuthDeg = 90f, elevationDeg = 0f)
        assertNear(pos.x, 5f)
        assertNear(pos.y, 0f)
        assertNear(pos.z, 0f)
    }

    @Test
    fun `toWorldPosition - up el90 maps to positive Y`() {
        val pos = toWorldPosition(5f, azimuthDeg = 0f, elevationDeg = 90f)
        assertNear(pos.x, 0f)
        assertNear(pos.y, 5f)
        assertNear(pos.z, 0f)
    }

    @Test
    fun `toWorldPosition - null elevation treated as zero elevation`() {
        val withNull = toWorldPosition(3f, azimuthDeg = 45f, elevationDeg = null)
        val withZero = toWorldPosition(3f, azimuthDeg = 45f, elevationDeg = 0f)
        assertNear(withNull.x, withZero.x)
        assertNear(withNull.y, withZero.y)
        assertNear(withNull.z, withZero.z)
    }

    @Test
    fun `toWorldPosition - distance preserved in magnitude`() {
        val dist = 4f
        val pos = toWorldPosition(dist, azimuthDeg = 30f, elevationDeg = 20f)
        val magnitude = sqrt(pos.x * pos.x + pos.y * pos.y + pos.z * pos.z)
        assertNear(magnitude, dist)
    }

    @Test
    fun `toScreenPoint - origin maps to 0,0`() {
        val sp = WorldPosition(0f, 0f, 0f).toScreenPoint()
        assertNear(sp.x, 0f)
        assertNear(sp.y, 0f)
    }

    @Test
    fun `toScreenPoint - positive X maps to right and down in screen space`() {
        val sp = WorldPosition(1f, 0f, 0f).toScreenPoint()
        assertThat(sp.x).isGreaterThan(0f)
    }

    @Test
    fun `toScreenPoint - positive Z maps to left and down in screen space`() {
        val sp = WorldPosition(0f, 0f, 1f).toScreenPoint()
        assertThat(sp.x).isLessThan(0f)
    }

    @Test
    fun `toScreenPoint - positive Y maps upward in screen space`() {
        val sp = WorldPosition(0f, 1f, 0f).toScreenPoint()
        assertThat(sp.y).isLessThan(0f)
    }

    @Test
    fun `toScreenPoint - scale multiplies output linearly`() {
        val sp1 = WorldPosition(1f, 2f, 3f).toScreenPoint(scale = 1f)
        val sp2 = WorldPosition(1f, 2f, 3f).toScreenPoint(scale = 2f)
        assertNear(sp2.x, sp1.x * 2f)
        assertNear(sp2.y, sp1.y * 2f)
    }
}
