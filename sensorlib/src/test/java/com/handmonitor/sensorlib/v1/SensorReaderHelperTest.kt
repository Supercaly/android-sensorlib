package com.handmonitor.sensorlib.v1

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.HandlerThread
import com.google.common.truth.Truth.assertThat
import com.handmonitor.sensorlib.mockLog
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
class SensorReaderHelperTest {
    @MockK
    private lateinit var mContext: Context

    @MockK
    private lateinit var mSensorManager: SensorManager

    @MockK
    private lateinit var mGenericSensor: Sensor

    private val mConsumer = object : SensorDataHandler {
        override fun onNewData(data: FloatArray) {}
    }

    private lateinit var mReaderHelper: SensorReaderHelper

    @BeforeEach
    fun setup() {
        mockLog()
        mockkConstructor(HandlerThread::class)
        mockkConstructor(Thread::class)
        mockkConstructor(SensorEventConsumerRn::class)
        mockkConstructor(SensorEventProducer::class)
        mockkConstructor(SensorSharedData::class)

        every { mContext.getSystemService(Context.SENSOR_SERVICE) } returns mSensorManager
        every { mSensorManager.getDefaultSensor(any()) } returns mGenericSensor

        every { anyConstructed<SensorSharedData>().putGyro(any()) } just Runs
        every { anyConstructed<SensorSharedData>().putAcc(any()) } just Runs
        every { anyConstructed<SensorSharedData>().getData() } returns FloatArray(100) { 0.0f }

        every { anyConstructed<SensorEventProducer>().startListening() } just Runs
        every { anyConstructed<SensorEventProducer>().stopListening() } just Runs

        every { anyConstructed<Thread>().start() } just Runs

        every { anyConstructed<HandlerThread>().start() } just Runs
        every { anyConstructed<HandlerThread>().looper } returns null
        every { anyConstructed<HandlerThread>().quit() } returns true

        mReaderHelper = SensorReaderHelper(
            mContext,
            mConsumer,
            100,
            20
        )
    }

    @Test
    fun `calling start one time starts the internals`() {
        verify(inverse = true) {
            anyConstructed<HandlerThread>().start()
            anyConstructed<Thread>().start()
            anyConstructed<SensorEventProducer>().startListening()
            anyConstructed<SensorEventConsumerRn>().run()
            anyConstructed<SensorSharedData>().putAcc(any())
            anyConstructed<SensorSharedData>().putGyro(any())
            anyConstructed<SensorSharedData>().getData()
        }
        assertThat(mReaderHelper.isStarted).isFalse()

        mReaderHelper.start()
        verify {
            anyConstructed<HandlerThread>().start()
            anyConstructed<Thread>().start()
            anyConstructed<SensorEventProducer>().startListening()
        }
        assertThat(mReaderHelper.isStarted).isTrue()
    }

    @Test
    fun `calling start multiple times result in an exception being thrown`() {
        verify(inverse = true) {
            anyConstructed<HandlerThread>().start()
            anyConstructed<Thread>().start()
            anyConstructed<SensorEventProducer>().startListening()
            anyConstructed<SensorEventConsumerRn>().run()
            anyConstructed<SensorSharedData>().putAcc(any())
            anyConstructed<SensorSharedData>().putGyro(any())
            anyConstructed<SensorSharedData>().getData()
        }
        assertThat(mReaderHelper.isStarted).isFalse()

        mReaderHelper.start()
        verify {
            anyConstructed<HandlerThread>().start()
            anyConstructed<Thread>().start()
            anyConstructed<SensorEventProducer>().startListening()
        }
        assertThat(mReaderHelper.isStarted).isTrue()

        assertThrows<IllegalThreadStateException> {
            mReaderHelper.start()
        }
        assertThat(mReaderHelper.isStarted).isTrue()
    }

    @Test
    fun `calling stop stops the internals`() {
        mReaderHelper.start()
        verify {
            anyConstructed<HandlerThread>().start()
            anyConstructed<Thread>().start()
            anyConstructed<SensorEventProducer>().startListening()
        }
        assertThat(mReaderHelper.isStarted).isTrue()

        mReaderHelper.stop()
        verify {
            anyConstructed<HandlerThread>().quit()
            anyConstructed<Thread>().interrupt()
            anyConstructed<SensorEventProducer>().stopListening()
        }
        assertThat(mReaderHelper.isStarted).isFalse()
    }

    @Test
    fun `calling stop when it's not started does nothing`() {
        assertThat(mReaderHelper.isStarted).isFalse()

        mReaderHelper.stop()
        assertThat(mReaderHelper.isStarted).isFalse()
    }
}
