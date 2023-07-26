package com.handmonitor.sensorlib.v3

import android.hardware.Sensor
import com.google.common.truth.Truth.assertThat
import com.handmonitor.sensorlib.mockLog
import com.handmonitor.sensorlib.v3.internal.SensorData
import com.handmonitor.sensorlib.v3.internal.SensorDataListener
import com.handmonitor.sensorlib.v3.internal.WindowBuffer
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SensorFlowTest {
    companion object {
        private const val samplingMs = 20L
        private const val mockTimestamp = 314159265358979000L
    }

    private fun mockSensorData(
        values: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f),
        type: Int = 0,
        timestamp: Long = 0L
    ): SensorData {
        return SensorData(values, type, timestamp)
    }

    @MockK
    private lateinit var mAccListener: SensorDataListener

    @MockK
    private lateinit var mGyroListener: SensorDataListener

    @MockK
    private lateinit var mWindowBuffer: WindowBuffer

    @SpyK
    private var mSensorChannel = Channel<SensorData>()

    @BeforeEach
    fun setup() {
        mockLog()
        every { mAccListener.start() } just Runs
        every { mAccListener.stop() } just Runs
        every { mGyroListener.start() } just Runs
        every { mGyroListener.stop() } just Runs
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `asFlow listens to sensors flows and appends to buffer`() = runTest {
        val sf = SensorFlowImpl(
            samplingMs,
            mWindowBuffer,
            mSensorChannel,
            mAccListener,
            mGyroListener
        )
        every { mAccListener.asFlow() } returns flowOf(
            mockSensorData(
                type = Sensor.TYPE_ACCELEROMETER,
                timestamp = mockTimestamp
            ),
            mockSensorData(
                type = Sensor.TYPE_ACCELEROMETER,
                timestamp = mockTimestamp + 20L.toNs()
            )
        )
        every { mGyroListener.asFlow() } returns flowOf(
            mockSensorData(
                type = Sensor.TYPE_GYROSCOPE,
                timestamp = mockTimestamp
            ),
            mockSensorData(
                type = Sensor.TYPE_GYROSCOPE,
                timestamp = mockTimestamp + 22L.toNs()
            ),
            mockSensorData(
                type = Sensor.TYPE_GYROSCOPE,
                timestamp = mockTimestamp + 43L.toNs()
            )
        )
        every { mWindowBuffer.appendSample(any()) } returns WindowBuffer.Status.NotFull

        val flow = sf.asFlow(UnconfinedTestDispatcher(testScheduler))
        val collectJob = launch { flow.collect() }

        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()

        coVerify(exactly = 5) { mSensorChannel.send(any()) }
        verify(exactly = 5) { mWindowBuffer.appendSample(any()) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `asFlow produces a new window`() = runTest {
        val sf = SensorFlowImpl(
            samplingMs,
            mWindowBuffer,
            mSensorChannel,
            mAccListener,
            mGyroListener
        )
        every { mAccListener.asFlow() } returns flowOf(
            mockSensorData()
        )
        every { mGyroListener.asFlow() } returns flowOf()
        every { mWindowBuffer.appendSample(any()) } returns WindowBuffer.Status.Full
        val mockWindow: SensorWindow = List(600) { 1.0f }
        every { mWindowBuffer.window } returns mockWindow

        val flow = sf.asFlow(UnconfinedTestDispatcher(testScheduler))
        val windows = mutableListOf<SensorWindow>()
        val collectJob = launch {
            flow.onEach {
                windows.add(it)
            }.collect()
        }
        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()

        verify { mWindowBuffer.appendSample(any()) }
        assertThat(windows).isNotEmpty()
        assertThat(windows.size).isEqualTo(1)
        assertThat(windows.first()).isEqualTo(mockWindow)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `asFlow cancels in case of errors`() = runTest {
        val sf = SensorFlowImpl(
            samplingMs,
            mWindowBuffer,
            mSensorChannel,
            mAccListener,
            mGyroListener
        )
        val ex = RuntimeException()
        every { mAccListener.asFlow() } returns flowOf()
        every { mGyroListener.asFlow() } returns flow { throw ex }
        every { mWindowBuffer.appendSample(any()) } returns WindowBuffer.Status.NotFull

        val flow = sf.asFlow(UnconfinedTestDispatcher(testScheduler))
        val collectJob = launch {
            flow.catch {
                assertThat(it).isInstanceOf(RuntimeException::class.java)
            }.collect()
        }
        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()
    }
}
