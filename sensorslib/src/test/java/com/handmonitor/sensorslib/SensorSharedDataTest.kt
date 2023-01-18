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
class SensorSharedDataTest {
    @MockK
    private lateinit var mProducerSemaphore: Semaphore

    @MockK
    private lateinit var mConsumerSemaphore: Semaphore

    @MockK
    private lateinit var mProducerLock: ReentrantLock

    @MockK
    private lateinit var mSensorSharedDataImpl: SensorSharedDataImpl

    @InjectMockKs(injectImmutable = true, overrideValues = true)
    private var mData = SensorSharedData(100)

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
        every { mSensorSharedDataImpl.appendAcc(any()) } returns false
        mData.putAcc(mMockAccSample)
        verifyOrder {
            mProducerLock.lock()
            mSensorSharedDataImpl.appendAcc(any())
            mProducerLock.unlock()
        }

        every { mSensorSharedDataImpl.appendAcc(any()) } returns true
        mData.putAcc(mMockAccSample)
        verifyOrder {
            mProducerLock.lock()
            mSensorSharedDataImpl.appendAcc(any())
            mProducerLock.unlock()
        }
    }

    @Test
    fun `putGyro locks and unlocks resources while in use`() {
        every { mSensorSharedDataImpl.appendGyro(any()) } returns false
        mData.putGyro(mMockGyroSample)
        verifyOrder {
            mProducerLock.lock()
            mSensorSharedDataImpl.appendGyro(any())
            mProducerLock.unlock()
        }

        every { mSensorSharedDataImpl.appendGyro(any()) } returns true
        mData.putGyro(mMockGyroSample)
        verifyOrder {
            mProducerLock.lock()
            mSensorSharedDataImpl.appendGyro(any())
            mProducerLock.unlock()
        }
    }

    @Test
    fun `putAcc wakes consumer only when window is full`() {
        every { mSensorSharedDataImpl.appendAcc(any()) } returns false
        mData.putAcc(mMockAccSample)
        verify(inverse = true) {
            mConsumerSemaphore.release()
            mProducerSemaphore.acquire()
        }

        every { mSensorSharedDataImpl.appendAcc(any()) } returns true
        mData.putAcc(mMockAccSample)
        verifyOrder {
            mConsumerSemaphore.release()
            mProducerSemaphore.acquire()
        }
    }

    @Test
    fun `putGyro wakes consumer only when window is full`() {
        every { mSensorSharedDataImpl.appendGyro(any()) } returns false
        mData.putGyro(mMockGyroSample)
        verify(inverse = true) {
            mConsumerSemaphore.release()
            mProducerSemaphore.acquire()
        }

        every { mSensorSharedDataImpl.appendGyro(any()) } returns true
        mData.putGyro(mMockGyroSample)
        verifyOrder {
            mConsumerSemaphore.release()
            mProducerSemaphore.acquire()
        }
    }

    @Test
    fun `getData waits for producer to complete the window`() {
        every { mSensorSharedDataImpl.window } returns floatArrayOf()
        mData.getData()
        verifyOrder {
            mConsumerSemaphore.acquire()
            mProducerSemaphore.release()
        }
    }
}

@ExtendWith(MockKExtension::class)
class SensorSharedDataImplTest {
    companion object {
        private const val WINDOW_SIZE = 100
        private const val WINDOW_LENGTH = WINDOW_SIZE * 6
    }

    private val mSensorDataImpl = SensorSharedDataImpl(WINDOW_SIZE)
    private val mMockAcc = SensorSample(1.0f, 2.0f, 3.0f)
    private val mMockGyro = SensorSample(4.0f, 5.0f, 6.0f)

    @Test
    fun `appendAcc appends new data not filling the window`() {
        // Set a single accelerometer SensorSample
        val full = mSensorDataImpl.appendAcc(mMockAcc)
        val data = mSensorDataImpl.window
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
        val full = mSensorDataImpl.appendGyro(mMockGyro)
        val data = mSensorDataImpl.window
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
            full = mSensorDataImpl.appendAcc(mMockAcc)
        }
        val data = mSensorDataImpl.window
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
            full = mSensorDataImpl.appendGyro(mMockGyro)
        }
        val data = mSensorDataImpl.window
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
            full = mSensorDataImpl.appendAcc(SensorSample(1.0f, 1.0f, 1.0f))
        }
        var data = mSensorDataImpl.window
        assertThat(full).isTrue()
        for (i in 0 until WINDOW_LENGTH step 6) {
            assertThat(data[0 + i]).isEqualTo(1.0f)
            assertThat(data[1 + i]).isEqualTo(1.0f)
            assertThat(data[2 + i]).isEqualTo(1.0f)
        }
        // Send some more data expecting to start over again
        full = mSensorDataImpl.appendAcc(SensorSample(2.0f, 2.0f, 2.0f))
        data = mSensorDataImpl.window
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
            full = mSensorDataImpl.appendGyro(SensorSample(1.0f, 1.0f, 1.0f))
        }
        var data = mSensorDataImpl.window
        assertThat(full).isTrue()
        for (i in 0 until WINDOW_LENGTH step 6) {
            assertThat(data[3 + i]).isEqualTo(1.0f)
            assertThat(data[4 + i]).isEqualTo(1.0f)
            assertThat(data[5 + i]).isEqualTo(1.0f)
        }
        // Send some more data expecting to start over again
        full = mSensorDataImpl.appendGyro(SensorSample(2.0f, 2.0f, 2.0f))
        data = mSensorDataImpl.window
        assertThat(full).isFalse()
        assertThat(data[3]).isEqualTo(2.0f)
        assertThat(data[4]).isEqualTo(2.0f)
        assertThat(data[5]).isEqualTo(2.0f)
    }

    @Test
    fun `getData without set returns a window of all zeros`() {
        val data = mSensorDataImpl.window
        assertThat(data).isNotEmpty()
        assertThat(data).hasLength(WINDOW_LENGTH)
        for (i in 0 until WINDOW_LENGTH) {
            assertThat(data[i]).isEqualTo(0.0f)
        }
    }

    @Test
    fun `getData setting only the accelerometer returns correctly`() {
        for (i in 0 until WINDOW_SIZE) {
            mSensorDataImpl.appendAcc(mMockAcc)
        }
        val data = mSensorDataImpl.window
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
            mSensorDataImpl.appendGyro(mMockGyro)
        }
        val data = mSensorDataImpl.window
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
