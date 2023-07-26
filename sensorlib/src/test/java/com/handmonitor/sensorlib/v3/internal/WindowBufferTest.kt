package com.handmonitor.sensorlib.v3.internal

import android.hardware.Sensor
import com.google.common.truth.Truth.assertThat
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class WindowBufferTest {
    companion object {
        private const val size = 100
    }

    private val mAccData = SensorData(
        floatArrayOf(1.1f, 2.2f, 3.3f),
        Sensor.TYPE_ACCELEROMETER,
        0L
    )
    private val mGyrData = SensorData(
        floatArrayOf(4.4f, 5.5f, 6.6f),
        Sensor.TYPE_GYROSCOPE,
        0L
    )

    @Test
    fun `appendSample append accelerometer without filling the window`() {
        val buffer = WindowBuffer(size)
        val full = buffer.appendSample(mAccData)
        assertThat(full).isEqualTo(WindowBuffer.Status.NotFull)
        val window = buffer.window
        assertThat(window[0]).isEqualTo(mAccData.values[0])
        assertThat(window[1]).isEqualTo(mAccData.values[1])
        assertThat(window[2]).isEqualTo(mAccData.values[2])
        for (i in 3 until window.size) {
            assertThat(window[i]).isZero()
        }
    }

    @Test
    fun `appendSample append gyroscope without filling the window`() {
        val buffer = WindowBuffer(size)
        val full = buffer.appendSample(mGyrData)
        assertThat(full).isEqualTo(WindowBuffer.Status.NotFull)
        val window = buffer.window
        assertThat(window[0]).isZero()
        assertThat(window[1]).isZero()
        assertThat(window[2]).isZero()
        assertThat(window[3]).isEqualTo(mGyrData.values[0])
        assertThat(window[4]).isEqualTo(mGyrData.values[1])
        assertThat(window[5]).isEqualTo(mGyrData.values[2])
        for (i in 6 until window.size) {
            assertThat(window[i]).isZero()
        }
    }

    @Test
    fun `appendSample discard accelerometer when window is not full`() {
        val buffer = WindowBuffer(size)
        var full = WindowBuffer.Status.NotFull
        for (i in 0 until size) {
            full = buffer.appendSample(mAccData)
        }
        assertThat(full).isEqualTo(WindowBuffer.Status.NotFull)

        full = buffer.appendSample(mAccData)
        assertThat(full).isEqualTo(WindowBuffer.Status.NotFull)
    }

    @Test
    fun `appendSample discard gyroscope when window is not full`() {
        val buffer = WindowBuffer(size)
        var full = WindowBuffer.Status.NotFull
        for (i in 0 until size) {
            full = buffer.appendSample(mGyrData)
        }
        assertThat(full).isEqualTo(WindowBuffer.Status.NotFull)

        full = buffer.appendSample(mGyrData)
        assertThat(full).isEqualTo(WindowBuffer.Status.NotFull)
    }

    @Test
    fun `appendSample append data and fill the window`() {
        val buffer = WindowBuffer(size)
        var full = WindowBuffer.Status.NotFull
        for (i in 0 until size) {
            buffer.appendSample(mAccData)
            full = buffer.appendSample(mGyrData)
        }
        assertThat(full).isEqualTo(WindowBuffer.Status.Full)
        val window = buffer.window
        for (i in window.indices step 6) {
            assertThat(window[0]).isEqualTo(mAccData.values[0])
            assertThat(window[1]).isEqualTo(mAccData.values[1])
            assertThat(window[2]).isEqualTo(mAccData.values[2])
            assertThat(window[3]).isEqualTo(mGyrData.values[0])
            assertThat(window[4]).isEqualTo(mGyrData.values[1])
            assertThat(window[5]).isEqualTo(mGyrData.values[2])
        }
    }
}
