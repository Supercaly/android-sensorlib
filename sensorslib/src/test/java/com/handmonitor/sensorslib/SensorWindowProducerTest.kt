package com.handmonitor.sensorslib

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.HandlerThread
import android.os.Looper
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SensorWindowProducerTest {
    companion object {
        private const val samplingMs = 10L
        private const val windowSize = 200
    }

    @MockK
    private lateinit var mOnNewWindow: (SensorWindow) -> Unit

    @MockK
    private lateinit var mContext: Context

    @MockK
    private lateinit var mSensorManager: SensorManager

    @MockK
    private lateinit var mAccSensor: Sensor

    @MockK
    private lateinit var mGyroSensor: Sensor

    @MockK
    private lateinit var mHandlerThread: HandlerThread

    @MockK
    private lateinit var mLooper: Looper

    @BeforeEach
    fun setup() {
        mockLog()

        every { mContext.getSystemService(Context.SENSOR_SERVICE) } returns mSensorManager

        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mAccSensor
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mGyroSensor
        every { mSensorManager.registerListener(any(), any(), any(), any(), any()) } returns true
        every { mSensorManager.unregisterListener(any<SensorEventListener>()) } just Runs

        every { mHandlerThread.start() } just Runs
        every { mHandlerThread.quit() } returns true
        every { mHandlerThread.looper } returns mLooper

        every { mAccSensor.type } returns Sensor.TYPE_ACCELEROMETER
        every { mGyroSensor.type } returns Sensor.TYPE_GYROSCOPE

        every { mOnNewWindow.invoke(any()) } just Runs
    }

    @Test
    fun `constructor with unsupported sensors throws an exception`() {
        // Accelerometer not supported
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns null
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mGyroSensor
        assertThrows<SensorNotSupportedException> {
            SensorWindowProducer(
                mContext,
                mHandlerThread,
                samplingMs, windowSize, mOnNewWindow
            )
        }

        // Gyroscope not supported
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mAccSensor
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns null
        assertThrows<SensorNotSupportedException> {
            SensorWindowProducer(
                mContext,
                mHandlerThread,
                samplingMs, windowSize, mOnNewWindow
            )
        }
    }

    @Test
    fun `startSensors start the sensors correctly`() {
        val producer = SensorWindowProducer(
            mContext,
            mHandlerThread,
            samplingMs, windowSize, mOnNewWindow
        )
        assertThat(producer.isListening).isFalse()

        producer.startSensors()
        assertThat(producer.isListening).isTrue()
        verify {
            mSensorManager.registerListener(any(), mAccSensor, any(), any(), any())
            mSensorManager.registerListener(any(), mGyroSensor, any(), any(), any())
            mHandlerThread.start()
        }
    }

    @Test
    fun `startSensors throws exception when called multiple times`() {
        val producer = SensorWindowProducer(
            mContext,
            mHandlerThread,
            samplingMs, windowSize, mOnNewWindow
        )
        assertThat(producer.isListening).isFalse()
        producer.startSensors()
        assertThat(producer.isListening).isTrue()

        assertThrows<IllegalThreadStateException> {
            producer.startSensors()
        }
        verify(exactly = 1) {
            mSensorManager.registerListener(any(), mAccSensor, any(), any(), any())
            mSensorManager.registerListener(any(), mGyroSensor, any(), any(), any())
            mHandlerThread.start()
        }
        assertThat(producer.isListening).isTrue()
    }

    @Test
    fun `stopSensors stop the sensors correctly`() {
        val producer = SensorWindowProducer(
            mContext,
            mHandlerThread,
            samplingMs, windowSize, mOnNewWindow
        )
        producer.startSensors()
        assertThat(producer.isListening).isTrue()

        producer.stopSensors()
        assertThat(producer.isListening).isFalse()
        verify {
            mSensorManager.unregisterListener(any<SensorEventListener>())
            mHandlerThread.quit()
        }

        // Call it a second time does nothing bad
        producer.stopSensors()
        assertThat(producer.isListening).isFalse()
        verify {
            mSensorManager.unregisterListener(any<SensorEventListener>())
            mHandlerThread.quit()
        }
    }

    @Test
    fun `onSensorChanged refuse new samples`() {
        mockkConstructor(SensorSampleFilter::class)
        mockkConstructor(SensorWindowBuffer::class)
        every { anyConstructed<SensorSampleFilter>().newSample(any()) } returns false
        every { anyConstructed<SensorWindowBuffer>().pushAccelerometer(any()) } returns false
        every { anyConstructed<SensorWindowBuffer>().pushGyroscope(any()) } returns false

        val producer = SensorWindowProducer(
            mContext,
            mHandlerThread,
            samplingMs, windowSize, mOnNewWindow
        )
        val accEvent = mockSensorEvent(mAccSensor)
        val gyroEvent = mockSensorEvent(mGyroSensor)

        producer.onSensorChanged(accEvent)
        producer.onSensorChanged(gyroEvent)
        verify {
            anyConstructed<SensorSampleFilter>().newSample(accEvent)
            anyConstructed<SensorSampleFilter>().newSample(gyroEvent)
        }
        verify(inverse = true) {
            anyConstructed<SensorWindowBuffer>().pushAccelerometer(any())
            anyConstructed<SensorWindowBuffer>().pushGyroscope(any())
            mOnNewWindow.invoke(any())
        }
    }

    @Test
    fun `onSensorChanged accept new samples but not fill the window`() {
        mockkConstructor(SensorSampleFilter::class)
        mockkConstructor(SensorWindowBuffer::class)
        every { anyConstructed<SensorSampleFilter>().newSample(any()) } returns true
        every { anyConstructed<SensorWindowBuffer>().pushAccelerometer(any()) } returns false
        every { anyConstructed<SensorWindowBuffer>().pushGyroscope(any()) } returns false

        val producer = SensorWindowProducer(
            mContext,
            mHandlerThread,
            samplingMs, windowSize, mOnNewWindow
        )
        val accEvent = mockSensorEvent(mAccSensor)
        val gyroEvent = mockSensorEvent(mGyroSensor)

        producer.onSensorChanged(accEvent)
        producer.onSensorChanged(gyroEvent)
        verify {
            anyConstructed<SensorSampleFilter>().newSample(accEvent)
            anyConstructed<SensorSampleFilter>().newSample(gyroEvent)
            anyConstructed<SensorWindowBuffer>().pushAccelerometer(any())
            anyConstructed<SensorWindowBuffer>().pushGyroscope(any())
        }
        verify(inverse = true) {
            mOnNewWindow.invoke(any())
        }
    }

    @Test
    fun `onSensorChanged calls onNewWindow`() {
        mockkConstructor(SensorSampleFilter::class)
        mockkConstructor(SensorWindowBuffer::class)
        every { anyConstructed<SensorSampleFilter>().newSample(any()) } returns true
        every { anyConstructed<SensorWindowBuffer>().pushAccelerometer(any()) } returns true
        every { anyConstructed<SensorWindowBuffer>().pushGyroscope(any()) } returns true

        val producer = SensorWindowProducer(
            mContext,
            mHandlerThread,
            samplingMs, windowSize, mOnNewWindow
        )
        val accEvent = mockSensorEvent(mAccSensor)
        val gyroEvent = mockSensorEvent(mGyroSensor)

        producer.onSensorChanged(accEvent)
        producer.onSensorChanged(gyroEvent)
        verify {
            anyConstructed<SensorSampleFilter>().newSample(accEvent)
            anyConstructed<SensorSampleFilter>().newSample(gyroEvent)
            anyConstructed<SensorWindowBuffer>().pushAccelerometer(any())
            anyConstructed<SensorWindowBuffer>().pushGyroscope(any())
            mOnNewWindow.invoke(any())
        }
    }
}
