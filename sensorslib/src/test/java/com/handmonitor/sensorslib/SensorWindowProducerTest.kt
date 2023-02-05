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

    private val onNewWindow: (SensorWindow) -> Unit = {}

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
                samplingMs, windowSize, onNewWindow
            )
        }

        // Gyroscope not supported
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mAccSensor
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns null
        assertThrows<SensorNotSupportedException> {
            SensorWindowProducer(
                mContext,
                mHandlerThread,
                samplingMs, windowSize, onNewWindow
            )
        }
    }

    @Test
    fun `startSensors start the sensors correctly`() {
        val producer = SensorWindowProducer(
            mContext,
            mHandlerThread,
            samplingMs, windowSize, onNewWindow
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
            samplingMs, windowSize, onNewWindow
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
            samplingMs, windowSize, onNewWindow
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
}
