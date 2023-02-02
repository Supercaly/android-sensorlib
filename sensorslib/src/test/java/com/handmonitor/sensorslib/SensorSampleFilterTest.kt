package com.handmonitor.sensorslib

import com.google.common.truth.Truth.assertThat
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SensorSampleFilterTest {
    companion object {
        private const val samplingMs = 20L
        private const val mockTimestamp = 314159265358979000L
    }

    @BeforeEach
    fun setup() {
        mockLog()
    }

    @Test
    fun `newSample discards the first event but set lastTimeNs`() {
        val sampler = SensorSampleFilter(samplingMs)
        assertThat(sampler.lastTimeNs).isNull()

        val accept = sampler.newSample(mockSensorEvent(timestamp = mockTimestamp))
        assertThat(accept).isFalse()
        assertThat(sampler.lastTimeNs).isEqualTo(mockTimestamp)
    }

    @Test
    fun `newSample accepts events`() {
        val sampler = SensorSampleFilter(samplingMs)
        sampler.newSample(mockSensorEvent(timestamp = mockTimestamp))
        assertThat(sampler.lastTimeNs).isNotNull()

        // Accepts event with timestamp in range
        var ts = mockTimestamp + sampler.minRangeNs + 1L.msToNs()
        var accepted = sampler.newSample(mockSensorEvent(timestamp = ts))
        assertThat(accepted).isTrue()
        assertThat(sampler.lastTimeNs).isEqualTo(ts)

        // Accepts event with timestamp equal to maxRange
        ts += sampler.maxRangeNs
        accepted = sampler.newSample(mockSensorEvent(timestamp = ts))
        assertThat(accepted).isTrue()
        assertThat(sampler.lastTimeNs).isEqualTo(ts)

        // Accepts event with timestamp equal to maxRange
        ts += sampler.maxRangeNs + 10L.msToNs()
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
        val sampler = SensorSampleFilter(samplingMs)
        sampler.newSample(mockSensorEvent(timestamp = mockTimestamp))
        assertThat(sampler.lastTimeNs).isNotNull()

        val ts = mockTimestamp + sampler.minRangeNs - 2L.msToNs()
        val accepted = sampler.newSample(mockSensorEvent(timestamp = ts))
        assertThat(accepted).isFalse()
        assertThat(sampler.lastTimeNs).isEqualTo(mockTimestamp)
    }

    @Test
    fun `newSample discards event with same timestamp as the last`() {
        val sampler = SensorSampleFilter(samplingMs)
        sampler.newSample(mockSensorEvent(timestamp = mockTimestamp))
        assertThat(sampler.lastTimeNs).isNotNull()

        val accepted = sampler.newSample(mockSensorEvent(timestamp = mockTimestamp))
        assertThat(accepted).isFalse()
        assertThat(sampler.lastTimeNs).isEqualTo(mockTimestamp)
    }

    @Test
    fun `newSample discards event with timestamp older than the last`() {
        val sampler = SensorSampleFilter(samplingMs)
        sampler.newSample(mockSensorEvent(timestamp = mockTimestamp))
        assertThat(sampler.lastTimeNs).isNotNull()

        val ts = mockTimestamp - 10L.msToNs()
        val accepted = sampler.newSample(mockSensorEvent(timestamp = ts))
        assertThat(accepted).isFalse()
        assertThat(sampler.lastTimeNs).isEqualTo(mockTimestamp)
    }
}
