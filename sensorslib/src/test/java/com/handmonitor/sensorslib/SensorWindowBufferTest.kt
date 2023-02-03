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
    fun `pushAccelerometer appends new data without filling the window`() {
        val buffer = SensorWindowBuffer(windowSize)
        for (d in buffer.window.asArray()) {
            assertThat(d).isZero()
        }

        val full = buffer.pushAccelerometer(mockAccSample)
        assertThat(full).isFalse()

        val window = buffer.window.asArray()
        assertThat(window[0]).isEqualTo(mockAccSample[0])
        assertThat(window[1]).isEqualTo(mockAccSample[1])
        assertThat(window[2]).isEqualTo(mockAccSample[2])
        for (i in 3 until window.size) {
            assertThat(window[i]).isZero()
        }
    }

    @Test
    fun `pushAccelerometer appends new data filling the window`() {
        val buffer = SensorWindowBuffer(windowSize)
        for (d in buffer.window.asArray()) {
            assertThat(d).isZero()
        }

        // Fill the window with only accelerometer data
        var full = false
        for (i in 0 until windowSize) {
            full = buffer.pushAccelerometer(mockAccSample)
        }
        assertThat(full).isTrue()
        var window = buffer.window.asArray()
        for (i in window.indices step 6) {
            assertThat(window[i + 0]).isEqualTo(mockAccSample[0])
            assertThat(window[i + 1]).isEqualTo(mockAccSample[1])
            assertThat(window[i + 2]).isEqualTo(mockAccSample[2])
            assertThat(window[i + 3]).isZero()
            assertThat(window[i + 4]).isZero()
            assertThat(window[i + 5]).isZero()
        }

        // Append one more sample
        full = buffer.pushAccelerometer(floatArrayOf(0.1f, 0.2f, 0.3f))
        window = buffer.window.asArray()
        assertThat(full).isFalse()
        assertThat(window[0]).isEqualTo(0.1f)
        assertThat(window[1]).isEqualTo(0.2f)
        assertThat(window[2]).isEqualTo(0.3f)
    }

    @Test
    fun `pushGyroscope appends new data without filling the window`() {
        val buffer = SensorWindowBuffer(windowSize)
        for (d in buffer.window.asArray()) {
            assertThat(d).isZero()
        }

        val full = buffer.pushGyroscope(mockGyroSample)
        assertThat(full).isFalse()

        val window = buffer.window.asArray()
        assertThat(window[0]).isZero()
        assertThat(window[1]).isZero()
        assertThat(window[2]).isZero()
        assertThat(window[3]).isEqualTo(mockGyroSample[0])
        assertThat(window[4]).isEqualTo(mockGyroSample[1])
        assertThat(window[5]).isEqualTo(mockGyroSample[2])
        for (i in 6 until window.size) {
            assertThat(window[i]).isZero()
        }
    }

    @Test
    fun `pushGyroscope appends new data filling the window`() {
        val buffer = SensorWindowBuffer(windowSize)
        for (d in buffer.window.asArray()) {
            assertThat(d).isZero()
        }

        // Fill the window with only accelerometer data
        var full = false
        for (i in 0 until windowSize) {
            full = buffer.pushGyroscope(mockGyroSample)
        }
        assertThat(full).isTrue()
        var window = buffer.window.asArray()
        for (i in window.indices step 6) {
            assertThat(window[i + 0]).isZero()
            assertThat(window[i + 1]).isZero()
            assertThat(window[i + 2]).isZero()
            assertThat(window[i + 3]).isEqualTo(mockGyroSample[0])
            assertThat(window[i + 4]).isEqualTo(mockGyroSample[1])
            assertThat(window[i + 5]).isEqualTo(mockGyroSample[2])
        }

        // Append one more sample
        full = buffer.pushGyroscope(floatArrayOf(0.1f, 0.2f, 0.3f))
        window = buffer.window.asArray()
        assertThat(full).isFalse()
        assertThat(window[3]).isEqualTo(0.1f)
        assertThat(window[4]).isEqualTo(0.2f)
        assertThat(window[5]).isEqualTo(0.3f)
    }
}
