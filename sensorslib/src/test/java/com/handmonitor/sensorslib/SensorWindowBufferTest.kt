package com.handmonitor.sensorslib

import com.google.common.truth.Truth.assertThat
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SensorWindowBufferTest {
    companion object {
        private const val windowSize = 100
    }

    private val mockAccSample: FloatArray = floatArrayOf(1.1f, 2.2f, 3.3f)
    private val mockGyroSample: FloatArray = floatArrayOf(4.4f, 5.5f, 6.6f)

    @Test
    fun `pushAccelerometer appends accelerometer samples without filling the window`() {
        // Append one accelerometer sample without filling the window
        var buffer = SensorWindowBuffer(windowSize)
        var full = buffer.pushAccelerometer(mockAccSample)
        assertThat(full).isFalse()
        var window = buffer.window.asArray()
        assertThat(window[0]).isEqualTo(mockAccSample[0])
        assertThat(window[1]).isEqualTo(mockAccSample[1])
        assertThat(window[2]).isEqualTo(mockAccSample[2])
        for (i in 3 until window.size) {
            assertThat(window[i]).isZero()
        }

        // Append all accelerometer samples without filling the window
        buffer = SensorWindowBuffer(windowSize)
        for (i in 0 until windowSize) {
            full = buffer.pushAccelerometer(mockAccSample)
        }
        assertThat(full).isFalse()
        window = buffer.window.asArray()
        for (i in window.indices step 6) {
            assertThat(window[0]).isEqualTo(mockAccSample[0])
            assertThat(window[1]).isEqualTo(mockAccSample[1])
            assertThat(window[2]).isEqualTo(mockAccSample[2])
            assertThat(window[3]).isZero()
            assertThat(window[4]).isZero()
            assertThat(window[5]).isZero()
        }
    }

    @Test
    fun `pushGyroscope appends one gyroscope sample without filling the window`() {
        // Append one gyroscope sample without filling the window
        var buffer = SensorWindowBuffer(windowSize)
        var full = buffer.pushGyroscope(mockGyroSample)
        assertThat(full).isFalse()
        var window = buffer.window.asArray()
        assertThat(window[0]).isZero()
        assertThat(window[1]).isZero()
        assertThat(window[2]).isZero()
        assertThat(window[3]).isEqualTo(mockGyroSample[0])
        assertThat(window[4]).isEqualTo(mockGyroSample[1])
        assertThat(window[5]).isEqualTo(mockGyroSample[2])
        for (i in 6 until window.size) {
            assertThat(window[i]).isZero()
        }

        // Append all gyroscope samples without filling the window
        buffer = SensorWindowBuffer(windowSize)
        for (i in 0 until windowSize) {
            full = buffer.pushGyroscope(mockGyroSample)
        }
        assertThat(full).isFalse()
        window = buffer.window.asArray()
        for (i in window.indices step 6) {
            assertThat(window[0]).isZero()
            assertThat(window[1]).isZero()
            assertThat(window[2]).isZero()
            assertThat(window[3]).isEqualTo(mockGyroSample[0])
            assertThat(window[4]).isEqualTo(mockGyroSample[1])
            assertThat(window[5]).isEqualTo(mockGyroSample[2])
        }
    }

    @Test
    fun `pushAccelerometer discards samples if window is full of accelerometer but not gyroscope`() {
        val buffer = SensorWindowBuffer(windowSize)
        var full = false
        for (i in 0 until windowSize) {
            full = buffer.pushAccelerometer(mockAccSample)
        }
        assertThat(full).isFalse()

        val mockAcc = floatArrayOf(1.2f, 2.3f, 3.4f)
        full = buffer.pushAccelerometer(mockAccSample)
        assertThat(full).isFalse()
        // Verify the element was not appended to the window
        val window = buffer.window.asArray()
        assertThat(window.any { it == mockAcc[0] }).isFalse()
        assertThat(window.any { it == mockAcc[1] }).isFalse()
        assertThat(window.any { it == mockAcc[2] }).isFalse()
    }

    @Test
    fun `pushGyroscope discards samples if window is full of gyroscope but not accelerometer`() {
        val buffer = SensorWindowBuffer(windowSize)
        var full = false
        for (i in 0 until windowSize) {
            full = buffer.pushGyroscope(mockGyroSample)
        }
        assertThat(full).isFalse()

        val mockGyro = floatArrayOf(4.5f, 5.6f, 6.7f)
        full = buffer.pushGyroscope(mockGyroSample)
        assertThat(full).isFalse()
        // Verify the element was not appended to the window
        val window = buffer.window.asArray()
        assertThat(window.any { it == mockGyro[0] }).isFalse()
        assertThat(window.any { it == mockGyro[1] }).isFalse()
        assertThat(window.any { it == mockGyro[2] }).isFalse()
    }

    @Test
    fun `pushAccelerometer fills the window`() {
        val buffer = SensorWindowBuffer(windowSize)
        var full = false
        for (i in 0 until windowSize) {
            full = buffer.pushGyroscope(mockGyroSample)
        }
        assertThat(full).isFalse()

        for (i in 0 until windowSize - 1) {
            full = buffer.pushAccelerometer(mockAccSample)
        }
        assertThat(full).isFalse()

        full = buffer.pushAccelerometer(mockAccSample)
        assertThat(full).isTrue()

        val window = buffer.window.asArray()
        for (i in window.indices step 6) {
            assertThat(window[0]).isEqualTo(mockAccSample[0])
            assertThat(window[1]).isEqualTo(mockAccSample[1])
            assertThat(window[2]).isEqualTo(mockAccSample[2])
            assertThat(window[3]).isEqualTo(mockGyroSample[0])
            assertThat(window[4]).isEqualTo(mockGyroSample[1])
            assertThat(window[5]).isEqualTo(mockGyroSample[2])
        }
    }

    @Test
    fun `pushGyroscope fills the window`() {
        val buffer = SensorWindowBuffer(windowSize)
        var full = false
        for (i in 0 until windowSize) {
            full = buffer.pushAccelerometer(mockAccSample)
        }
        assertThat(full).isFalse()

        for (i in 0 until windowSize - 1) {
            full = buffer.pushGyroscope(mockGyroSample)
        }
        assertThat(full).isFalse()

        full = buffer.pushGyroscope(mockGyroSample)
        assertThat(full).isTrue()

        val window = buffer.window.asArray()
        for (i in window.indices step 6) {
            assertThat(window[0]).isEqualTo(mockAccSample[0])
            assertThat(window[1]).isEqualTo(mockAccSample[1])
            assertThat(window[2]).isEqualTo(mockAccSample[2])
            assertThat(window[3]).isEqualTo(mockGyroSample[0])
            assertThat(window[4]).isEqualTo(mockGyroSample[1])
            assertThat(window[5]).isEqualTo(mockGyroSample[2])
        }
    }
}
