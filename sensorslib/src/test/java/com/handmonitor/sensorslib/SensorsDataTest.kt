package com.handmonitor.sensorslib

import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.ReentrantLock

@ExtendWith(MockKExtension::class)
class SensorsDataTest {
    @MockK
    private lateinit var mProducerSemaphore: Semaphore

    @MockK
    private lateinit var mConsumerSemaphore: Semaphore

    @MockK
    private lateinit var mProducerLock: ReentrantLock

    @MockK
    private lateinit var mSensorsDataImpl: SensorsDataImpl

    @InjectMockKs(injectImmutable = true, overrideValues = true)
    private var mData = SensorsData(100)

    private val mMockAccSample = SensorSample(1.0f, 2.0f, 3.0f)
    private val mMockGyroSample = SensorSample(4.0f, 5.0f, 6.0f)

    @BeforeEach
    fun setup() {
        every { mProducerLock.lock() } just Runs
        every { mProducerLock.unlock() } just Runs
        every { mProducerSemaphore.acquire() } just Runs
        every { mProducerSemaphore.release() } just Runs
        every { mConsumerSemaphore.acquire() } just Runs
        every { mConsumerSemaphore.release() } just Runs
    }

    @Test
    fun `putAcc locks and unlocks resources while in use`() {
        every { mSensorsDataImpl.appendAcc(any()) } returns false
        mData.putAcc(mMockAccSample)
        verifyOrder {
            mProducerLock.lock()
            mSensorsDataImpl.appendAcc(any())
            mProducerLock.unlock()
        }

        every { mSensorsDataImpl.appendAcc(any()) } returns true
        mData.putAcc(mMockAccSample)
        verifyOrder {
            mProducerLock.lock()
            mSensorsDataImpl.appendAcc(any())
            mProducerLock.unlock()
        }
    }

    @Test
    fun `putGyro locks and unlocks resources while in use`() {
        every { mSensorsDataImpl.appendGyro(any()) } returns false
        mData.putGyro(mMockGyroSample)
        verifyOrder {
            mProducerLock.lock()
            mSensorsDataImpl.appendGyro(any())
            mProducerLock.unlock()
        }

        every { mSensorsDataImpl.appendGyro(any()) } returns true
        mData.putGyro(mMockGyroSample)
        verifyOrder {
            mProducerLock.lock()
            mSensorsDataImpl.appendGyro(any())
            mProducerLock.unlock()
        }
    }

    @Test
    fun `putAcc wakes consumer only when window is full`() {
        every { mSensorsDataImpl.appendAcc(any()) } returns false
        mData.putAcc(mMockAccSample)
        verify(inverse = true) {
            mConsumerSemaphore.release()
            mProducerSemaphore.acquire()
        }

        every { mSensorsDataImpl.appendAcc(any()) } returns true
        mData.putAcc(mMockAccSample)
        verifyOrder {
            mConsumerSemaphore.release()
            mProducerSemaphore.acquire()
        }
    }

    @Test
    fun `putGyro wakes consumer only when window is full`() {
        every { mSensorsDataImpl.appendGyro(any()) } returns false
        mData.putGyro(mMockGyroSample)
        verify(inverse = true) {
            mConsumerSemaphore.release()
            mProducerSemaphore.acquire()
        }

        every { mSensorsDataImpl.appendGyro(any()) } returns true
        mData.putGyro(mMockGyroSample)
        verifyOrder {
            mConsumerSemaphore.release()
            mProducerSemaphore.acquire()
        }
    }

    @Test
    fun `getData waits for producer to complete the window`() {
        every { mSensorsDataImpl.window } returns floatArrayOf()
        mData.getData()
        verifyOrder {
            mConsumerSemaphore.acquire()
            mProducerSemaphore.release()
        }
    }
}

@ExtendWith(MockKExtension::class)
class SensorsDataImplTest {
    companion object {
        private const val WINDOW_SIZE = 100
        private const val WINDOW_LENGTH = WINDOW_SIZE * 6
    }

    private val mSensorsDataImpl = SensorsDataImpl(WINDOW_SIZE)
    private val mMockAcc = SensorSample(1.0f, 2.0f, 3.0f)
    private val mMockGyro = SensorSample(4.0f, 5.0f, 6.0f)

    @Test
    fun `appendAcc appends new data not filling the window`() {
        // Set a single accelerometer SensorSample
        val full = mSensorsDataImpl.appendAcc(mMockAcc)
        val data = mSensorsDataImpl.window
        assertThat(full).isFalse()
        assertThat(data).isNotEmpty()
        assertThat(data).hasLength(WINDOW_LENGTH)
        // Verify the first three data are set and the rest is 0
        assertThat(data[0]).isEqualTo(mMockAcc.x)
        assertThat(data[1]).isEqualTo(mMockAcc.y)
        assertThat(data[2]).isEqualTo(mMockAcc.z)
        for (i in 3 until WINDOW_LENGTH) {
            assertThat(data[i]).isEqualTo(0.0f)
        }
    }

    @Test
    fun `appendGyro appends new data not filling the window`() {
        // Set a single gyroscope SensorSample
        val full = mSensorsDataImpl.appendGyro(mMockGyro)
        val data = mSensorsDataImpl.window
        assertThat(full).isFalse()
        assertThat(data).isNotEmpty()
        assertThat(data).hasLength(WINDOW_LENGTH)
        // Verify the first three data are set (3-5 for gyro) and the rest is 0
        assertThat(data[0]).isEqualTo(0.0f)
        assertThat(data[1]).isEqualTo(0.0f)
        assertThat(data[2]).isEqualTo(0.0f)
        assertThat(data[3]).isEqualTo(mMockGyro.x)
        assertThat(data[4]).isEqualTo(mMockGyro.y)
        assertThat(data[5]).isEqualTo(mMockGyro.z)
        for (i in 6 until WINDOW_LENGTH) {
            assertThat(data[i]).isEqualTo(0.0f)
        }
    }

    @Test
    fun `appendAcc fills the window`() {
        // Send all data to fill the window
        var full = false
        for (i in 0 until WINDOW_SIZE) {
            full = mSensorsDataImpl.appendAcc(mMockAcc)
        }
        val data = mSensorsDataImpl.window
        assertThat(full).isTrue()
        assertThat(data).isNotEmpty()
        assertThat(data).hasLength(WINDOW_LENGTH)
        // Verify all the data are set
        for (i in 0 until WINDOW_LENGTH step 6) {
            assertThat(data[0 + i]).isEqualTo(mMockAcc.x)
            assertThat(data[1 + i]).isEqualTo(mMockAcc.y)
            assertThat(data[2 + i]).isEqualTo(mMockAcc.z)
        }
    }

    @Test
    fun `appendGyro fills the window`() {
        // Send all data to fill the window
        var full = false
        for (i in 0 until WINDOW_SIZE) {
            full = mSensorsDataImpl.appendGyro(mMockGyro)
        }
        val data = mSensorsDataImpl.window
        assertThat(full).isTrue()
        assertThat(data).isNotEmpty()
        assertThat(data).hasLength(WINDOW_LENGTH)
        // Verify all the data are set
        for (i in 0 until WINDOW_LENGTH step 6) {
            assertThat(data[3 + i]).isEqualTo(mMockGyro.x)
            assertThat(data[4 + i]).isEqualTo(mMockGyro.y)
            assertThat(data[5 + i]).isEqualTo(mMockGyro.z)
        }
    }

    @Test
    fun `appendAcc fills the window and starts from the beginning the next time`() {
        // Send all data to fill the window
        var full = false
        for (i in 0 until WINDOW_SIZE) {
            full = mSensorsDataImpl.appendAcc(SensorSample(1.0f, 1.0f, 1.0f))
        }
        var data = mSensorsDataImpl.window
        assertThat(full).isTrue()
        for (i in 0 until WINDOW_LENGTH step 6) {
            assertThat(data[0 + i]).isEqualTo(1.0f)
            assertThat(data[1 + i]).isEqualTo(1.0f)
            assertThat(data[2 + i]).isEqualTo(1.0f)
        }
        // Send some more data expecting to start over again
        full = mSensorsDataImpl.appendAcc(SensorSample(2.0f, 2.0f, 2.0f))
        data = mSensorsDataImpl.window
        assertThat(full).isFalse()
        assertThat(data[0]).isEqualTo(2.0f)
        assertThat(data[1]).isEqualTo(2.0f)
        assertThat(data[2]).isEqualTo(2.0f)
    }

    @Test
    fun `appendGyro fills the window and starts from the beginning the next time`() {
        // Send all data to fill the window
        var full = false
        for (i in 0 until WINDOW_SIZE) {
            full = mSensorsDataImpl.appendGyro(SensorSample(1.0f, 1.0f, 1.0f))
        }
        var data = mSensorsDataImpl.window
        assertThat(full).isTrue()
        for (i in 0 until WINDOW_LENGTH step 6) {
            assertThat(data[3 + i]).isEqualTo(1.0f)
            assertThat(data[4 + i]).isEqualTo(1.0f)
            assertThat(data[5 + i]).isEqualTo(1.0f)
        }
        // Send some more data expecting to start over again
        full = mSensorsDataImpl.appendGyro(SensorSample(2.0f, 2.0f, 2.0f))
        data = mSensorsDataImpl.window
        assertThat(full).isFalse()
        assertThat(data[3]).isEqualTo(2.0f)
        assertThat(data[4]).isEqualTo(2.0f)
        assertThat(data[5]).isEqualTo(2.0f)
    }

    @Test
    fun `getData without set returns a window of all zeros`() {
        val data = mSensorsDataImpl.window
        assertThat(data).isNotEmpty()
        assertThat(data).hasLength(WINDOW_LENGTH)
        for (i in 0 until WINDOW_LENGTH) {
            assertThat(data[i]).isEqualTo(0.0f)
        }
    }

    @Test
    fun `getData setting only the accelerometer returns correctly`() {
        for (i in 0 until WINDOW_SIZE) {
            mSensorsDataImpl.appendAcc(mMockAcc)
        }
        val data = mSensorsDataImpl.window
        assertThat(data).isNotEmpty()
        assertThat(data).hasLength(WINDOW_LENGTH)
        for (i in 0 until WINDOW_LENGTH step 6) {
            assertThat(data[i + 0]).isEqualTo(mMockAcc.x)
            assertThat(data[i + 1]).isEqualTo(mMockAcc.y)
            assertThat(data[i + 2]).isEqualTo(mMockAcc.z)
            assertThat(data[i + 3]).isEqualTo(0.0f)
            assertThat(data[i + 4]).isEqualTo(0.0f)
            assertThat(data[i + 5]).isEqualTo(0.0f)
        }
    }

    @Test
    fun `getData setting only the gyroscope returns correctly`() {
        for (i in 0 until WINDOW_SIZE) {
            mSensorsDataImpl.appendGyro(mMockGyro)
        }
        val data = mSensorsDataImpl.window
        assertThat(data).isNotEmpty()
        assertThat(data).hasLength(WINDOW_LENGTH)
        for (i in 0 until WINDOW_LENGTH step 6) {
            assertThat(data[i + 0]).isEqualTo(0.0f)
            assertThat(data[i + 1]).isEqualTo(0.0f)
            assertThat(data[i + 2]).isEqualTo(0.0f)
            assertThat(data[i + 3]).isEqualTo(mMockGyro.x)
            assertThat(data[i + 4]).isEqualTo(mMockGyro.y)
            assertThat(data[i + 5]).isEqualTo(mMockGyro.z)
        }
    }
}
