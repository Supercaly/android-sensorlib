package com.handmonitor.sensorlib.v3.internal

import android.hardware.Sensor
import com.google.common.truth.Truth.assertThat
import com.handmonitor.sensorlib.mockLog
import com.handmonitor.sensorlib.v3.toNs
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class FilterSamplesTest {
    companion object {
        private const val samplingMs = 20L
        private const val mockTimestamp = 314159265358979000L
        private const val type = Sensor.TYPE_ACCELEROMETER
    }

    private val mMinRangeNs = (samplingMs.toNs() - 5_000_000L)
    private val mBuffer = FloatArray(600) { 0.0f }

    @BeforeEach
    fun setup() {
        mockLog()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `filterSamples accepts events`() = runTest {
        val ts = mockTimestamp
        val ts2 = ts + mMinRangeNs
        val ts3 = ts2 + mMinRangeNs + 1L.toNs()
        val flow = flowOf(
            SensorData(mBuffer, type, ts),
            SensorData(mBuffer, type, ts2),
            SensorData(mBuffer, type, ts3)
        ).filterSamples(samplingMs)
        val collectJob = launch {
            val count = flow.count()
            assertThat(count).isEqualTo(3)
        }
        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `filterSamples discards events earlier than range`() = runTest {
        val ts = mockTimestamp
        val ts2 = mockTimestamp
        val ts3 = mockTimestamp - 10L.toNs()
        val ts4 = ts2 + mMinRangeNs - 1L.toNs()
        val flow = flowOf(
            SensorData(mBuffer, type, ts),
            SensorData(mBuffer, type, ts2),
            SensorData(mBuffer, type, ts3),
            SensorData(mBuffer, type, ts4)
        ).filterSamples(samplingMs)
        val collectJob = launch {
            val count = flow.count()
            assertThat(count).isEqualTo(1)
        }
        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()
    }
}
