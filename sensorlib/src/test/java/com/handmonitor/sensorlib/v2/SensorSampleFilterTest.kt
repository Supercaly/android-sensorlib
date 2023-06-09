package com.handmonitor.sensorlib.v2

import com.google.common.truth.Truth.assertThat
import com.handmonitor.sensorlib.mockLog
import com.handmonitor.sensorlib.mockSensorEvent
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.TimeUnit

@ExtendWith(MockKExtension::class)
class SensorSampleFilterTest {
    companion object {
        private const val samplingMs = 20L
        private const val mockTimestamp = 314159265358979000L
        private const val sensorName = "test-sensor"
    }

    @BeforeEach
    fun setup() {
        mockLog()
    }

    @Test
    fun `newSample discards the first event but set lastTimeNs`() {
        val sampler = SensorSampleFilter(samplingMs, sensorName)
        assertThat(sampler.lastTimeNs).isNull()

        val accept = sampler.newSample(mockSensorEvent(timestamp = mockTimestamp))
        assertThat(accept).isFalse()
        assertThat(sampler.lastTimeNs).isEqualTo(mockTimestamp)
    }

    @Test
    fun `newSample accepts events`() {
        val sampler = SensorSampleFilter(samplingMs, sensorName)
        sampler.newSample(mockSensorEvent(timestamp = mockTimestamp))
        assertThat(sampler.lastTimeNs).isNotNull()

        // Accepts event with timestamp in range
        var ts = mockTimestamp + sampler.minRangeNs + TimeUnit.MILLISECONDS.toNanos(1L)
        var accepted = sampler.newSample(mockSensorEvent(timestamp = ts))
        assertThat(accepted).isTrue()
        assertThat(sampler.lastTimeNs).isEqualTo(ts)

        // Accepts event with timestamp equal to maxRange
        ts += sampler.maxRangeNs
        accepted = sampler.newSample(mockSensorEvent(timestamp = ts))
        assertThat(accepted).isTrue()
        assertThat(sampler.lastTimeNs).isEqualTo(ts)

        // Accepts event with timestamp equal to maxRange
        ts += sampler.maxRangeNs + TimeUnit.MILLISECONDS.toNanos(10L)
        accepted = sampler.newSample(mockSensorEvent(timestamp = ts))
        assertThat(accepted).isTrue()
        assertThat(sampler.lastTimeNs).isEqualTo(ts)

        // Accepts event with timestamp equal to minRange
        ts += sampler.minRangeNs
        accepted = sampler.newSample(mockSensorEvent(timestamp = ts))
        assertThat(accepted).isTrue()
        assertThat(sampler.lastTimeNs).isEqualTo(ts)
    }

    @Test
    fun `newSample discards event with timestamp lower than minRange`() {
        val sampler = SensorSampleFilter(samplingMs, sensorName)
        sampler.newSample(mockSensorEvent(timestamp = mockTimestamp))
        assertThat(sampler.lastTimeNs).isNotNull()

        val ts = mockTimestamp + sampler.minRangeNs - TimeUnit.MILLISECONDS.toNanos(2L)
        val accepted = sampler.newSample(mockSensorEvent(timestamp = ts))
        assertThat(accepted).isFalse()
        assertThat(sampler.lastTimeNs).isEqualTo(mockTimestamp)
    }

    @Test
    fun `newSample discards event with same timestamp as the last`() {
        val sampler = SensorSampleFilter(samplingMs, sensorName)
        sampler.newSample(mockSensorEvent(timestamp = mockTimestamp))
        assertThat(sampler.lastTimeNs).isNotNull()

        val accepted = sampler.newSample(mockSensorEvent(timestamp = mockTimestamp))
        assertThat(accepted).isFalse()
        assertThat(sampler.lastTimeNs).isEqualTo(mockTimestamp)
    }

    @Test
    fun `newSample discards event with timestamp older than the last`() {
        val sampler = SensorSampleFilter(samplingMs, sensorName)
        sampler.newSample(mockSensorEvent(timestamp = mockTimestamp))
        assertThat(sampler.lastTimeNs).isNotNull()

        val ts = mockTimestamp - TimeUnit.MILLISECONDS.toNanos(10L)
        val accepted = sampler.newSample(mockSensorEvent(timestamp = ts))
        assertThat(accepted).isFalse()
        assertThat(sampler.lastTimeNs).isEqualTo(mockTimestamp)
    }
}
