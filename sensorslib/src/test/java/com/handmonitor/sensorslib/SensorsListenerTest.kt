package com.handmonitor.sensorslib

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.util.Log
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SensorsListenerTest {
    companion object {
        private const val WINDOW_DUR_MS = 2_000
        private const val SAMPLING_P_MS = 20
    }

    @MockK
    private lateinit var mContext: Context

    @MockK(relaxed = true)
    private lateinit var mSensorsData: SensorsData

    @MockK
    private lateinit var mSensorManager: SensorManager

    @MockK
    private lateinit var mAcc: Sensor

    @MockK
    private lateinit var mGyr: Sensor

    @MockK
    private lateinit var mHandler: Handler

    @BeforeEach
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { mContext.getSystemService(Context.SENSOR_SERVICE) } returns mSensorManager
    }

    @Test
    fun `startListening starts supported sensors`() {
        // Start listening with all sensors supported
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mAcc
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mGyr
        every { mSensorManager.registerListener(any(), any(), any(), any(), any()) } returns true

        var listener =
            SensorsListener(mContext, mSensorsData, mHandler, WINDOW_DUR_MS, SAMPLING_P_MS)
        listener.startListening()
        verify {
            mSensorManager.registerListener(any(), mAcc, any(), any(), any())
            mSensorManager.registerListener(any(), mGyr, any(), any(), any())
        }

        // Start listening with only accelerometer supported
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns null
        listener = SensorsListener(mContext, mSensorsData, mHandler, WINDOW_DUR_MS, SAMPLING_P_MS)
        listener.startListening()
        verify {
            mSensorManager.registerListener(any(), mAcc, any(), any(), any())
        }

        // Start listening with only gyroscope supported
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns null
        listener = SensorsListener(mContext, mSensorsData, mHandler, WINDOW_DUR_MS, SAMPLING_P_MS)
        listener.startListening()
        verify {
            mSensorManager.registerListener(any(), mGyr, any(), any(), any())
        }
    }

    @Test
    fun `startListening skips multiple calls without stopListening`() {
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mAcc
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mGyr
        every { mSensorManager.registerListener(any(), any(), any(), any(), any()) } returns true

        val listener =
            SensorsListener(mContext, mSensorsData, mHandler, WINDOW_DUR_MS, SAMPLING_P_MS)
        listener.startListening()
        verify {
            mSensorManager.registerListener(any(), mAcc, any(), any(), any())
            mSensorManager.registerListener(any(), mGyr, any(), any(), any())
        }

        listener.startListening()
        verify(atMost = 1) {
            mSensorManager.registerListener(any(), mAcc, any(), any(), any())
            mSensorManager.registerListener(any(), mGyr, any(), any(), any())
        }
    }

    @Test
    fun `stopListening stops listening to sensors`() {
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mAcc
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mGyr
        every { mSensorManager.unregisterListener(any<SensorEventListener>()) } just Runs

        val listener =
            SensorsListener(mContext, mSensorsData, mHandler, WINDOW_DUR_MS, SAMPLING_P_MS)
        listener.stopListening()
        verify {
            mSensorManager.unregisterListener(any<SensorEventListener>())
        }
    }

    @Test
    fun `startListening, stopListening and then startListening again`() {
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mAcc
        every { mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mGyr
        every { mSensorManager.registerListener(any(), any(), any(), any(), any()) } returns true
        every { mSensorManager.unregisterListener(any<SensorEventListener>()) } just Runs

        val listener =
            SensorsListener(mContext, mSensorsData, mHandler, WINDOW_DUR_MS, SAMPLING_P_MS)

        listener.startListening()
        verify {
            mSensorManager.registerListener(any(), mAcc, any(), any(), any())
            mSensorManager.registerListener(any(), mGyr, any(), any(), any())
        }
        listener.stopListening()
        verify {
            mSensorManager.unregisterListener(any<SensorEventListener>())
        }
        listener.startListening()
        verify {
            mSensorManager.registerListener(any(), mAcc, any(), any(), any())
            mSensorManager.registerListener(any(), mGyr, any(), any(), any())
        }
    }
}
