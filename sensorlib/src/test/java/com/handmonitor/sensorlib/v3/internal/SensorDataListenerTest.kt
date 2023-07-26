package com.handmonitor.sensorlib.v3.internal

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.google.common.truth.Truth.assertThat
import com.handmonitor.sensorlib.mockLog
import com.handmonitor.sensorlib.mockSensorEvent
import com.handmonitor.sensorlib.v3.SensorNotSupportedException
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SensorDataListenerTest {
    companion object {
        private const val samplingMs = 10L
        private const val windowSize = 200
        private const val type = Sensor.TYPE_ACCELEROMETER
    }

    @MockK
    private lateinit var mContext: Context

    @MockK
    private lateinit var mSensorManager: SensorManager

    @MockK
    private lateinit var mMockAcc: Sensor

    @MockK
    private lateinit var mLooper: Looper

    @BeforeEach
    fun setup() {
        mockLog()

        every { mContext.getSystemService(Context.SENSOR_SERVICE) } returns mSensorManager
        every { mSensorManager.registerListener(any(), any(), any(), any(), any()) } returns true
        every { mSensorManager.unregisterListener(any<SensorEventListener>()) } just Runs

        mockkConstructor(HandlerThread::class)
        every { anyConstructed<HandlerThread>().start() } just Runs
        every { anyConstructed<HandlerThread>().quit() } returns true
        every { anyConstructed<HandlerThread>().looper } returns mLooper
        mockkConstructor(Handler::class)
    }

    @Test
    fun `constructor throws unsupported sensor`() {
        every { mSensorManager.getDefaultSensor(type) } returns null
        assertThrows<SensorNotSupportedException> {
            SensorDataListener(mContext, type, samplingMs, windowSize)
        }
    }

    @Test
    fun `start listen to sensor correctly`() {
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mMockAcc
        val listener = SensorDataListener(mContext, type, samplingMs, windowSize)

        assertThat(listener.isListening).isFalse()
        listener.start()
        assertThat(listener.isListening).isTrue()
        verify { mSensorManager.registerListener(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `start throws error when called multiple times`() {
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mMockAcc
        val listener = SensorDataListener(mContext, type, samplingMs, windowSize)
        listener.start()
        assertThat(listener.isListening).isTrue()
        assertThrows<IllegalThreadStateException> {
            listener.start()
        }
        assertThat(listener.isListening).isTrue()
    }

    @Test
    fun `stop stops listen to sensor`() {
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mMockAcc
        val listener = SensorDataListener(mContext, type, samplingMs, windowSize)
        listener.start()

        assertThat(listener.isListening).isTrue()
        listener.stop()
        assertThat(listener.isListening).isFalse()
        verify { mSensorManager.unregisterListener(any<SensorEventListener>()) }
    }

    @Test
    fun `onSensorChanged invokes callback on new data`() {
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mMockAcc
        val listener = SensorDataListener(mContext, type, samplingMs, windowSize)
        val dataSlot = slot<SensorData>()
        val callback = mockk<SensorDataListenerCallback>()
        every { callback.invoke(capture(dataSlot)) } just Runs
        listener.setCallback(callback)
        val mockSensor = mockk<Sensor>()
        every { mockSensor.type } returns Sensor.TYPE_ACCELEROMETER
        val event = mockSensorEvent(
            sensor = mockSensor,
            timestamp = 1L,
            values = floatArrayOf(1f, 2f, 3f)
        )

        listener.onSensorChanged(event)
        assertThat(dataSlot.isCaptured).isTrue()
        verify { callback(any()) }
        assertThat(dataSlot.captured.type).isEqualTo(event.sensor.type)
        assertThat(dataSlot.captured.values).isEqualTo(event.values)
        assertThat(dataSlot.captured.timestamp).isEqualTo(event.timestamp)
    }

    @Test
    fun `onSensorChanged discards null events`() {
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mMockAcc
        val listener = SensorDataListener(mContext, type, samplingMs, windowSize)
        val dataSlot = slot<SensorData>()
        val callback = mockk<SensorDataListenerCallback>()
        every { callback.invoke(capture(dataSlot)) } just Runs
        listener.setCallback(callback)

        listener.onSensorChanged(null)
        verify(inverse = true) { callback(any()) }
        assertThat(dataSlot.isCaptured).isFalse()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `asFlow returns a flow of data`() = runTest {
        val listener = mockk<SensorDataListener>()
        val callbackSlot = slot<SensorDataListenerCallback>()

        every { listener.start() } just Runs
        every { listener.stop() } just Runs
        every { listener.setCallback(capture(callbackSlot)) } just Runs
        every { listener.asFlow() } answers { callOriginal() }

        var lastCollectedItem: SensorData? = null
        val itemsToSend = listOf(
            SensorData(FloatArray(600) { 0.0f }, type, 1L),
            SensorData(FloatArray(600) { 1.0f }, type, 1L),
            SensorData(FloatArray(600) { 2.0f }, type, 1L)
        )

        val flow = listener.asFlow().onEach { lastCollectedItem = it }
        val collectJob = launch { flow.collect() }
        advanceUntilIdle()
        verify { listener.start() }

        itemsToSend.forEach {
            callbackSlot.captured.invoke(it)
            advanceUntilIdle()
            assertThat(lastCollectedItem).isEqualTo(it)
        }

        collectJob.cancel()
        advanceUntilIdle()
        verify { listener.stop() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `asFlow propagates errors`() = runTest {
        val listener = mockk<SensorDataListener>()

        every { listener.start() } throws IllegalThreadStateException()
        every { listener.stop() } just Runs
        every { listener.setCallback(any()) } just Runs
        every { listener.asFlow() } answers { callOriginal() }

        val flow = listener.asFlow().catch {
            assertThat(it).isInstanceOf(IllegalThreadStateException::class.java)
        }
        val collectJob = launch { flow.collect() }
        advanceUntilIdle()
        verify {
            listener.start()
            collectJob.isCancelled
        }
    }
}
