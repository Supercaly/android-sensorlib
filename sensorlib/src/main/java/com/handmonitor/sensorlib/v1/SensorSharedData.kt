package com.handmonitor.sensorlib.v1

import java.util.concurrent.Semaphore
import java.util.concurrent.locks.ReentrantLock

/**
 * Represents a single sample of data obtained from a sensor.
 *
 * This type of data is a [Triple] of [Float]s values representing
 * the sensors' x, y, z axes values.
 *
 * @property[x] Value of the X axis
 * @property[y] Value of the Y axis
 * @property[z] Value of the Z axis
 */
data class SensorSample(
    val x: Float,
    val y: Float,
    val z: Float
) {
    companion object {
        fun fromArray(array: FloatArray): SensorSample =
            SensorSample(array[0], array[1], array[2])
    }
}

/**
 * A shared access to sensors data.
 *
 * This class represents a memory object shared between two threads,
 * one produces real-time values from the device sensors, and
 * the other consumes them.
 *
 * This class implements a sort of producer-consumer pattern, but the
 * data is produced continuously and it's consumed in windows of given size.
 *
 * @param[samplingWindowSize] Size of the sampling window.
 * @constructor Create a new instance of [SensorSharedData] passing a sampling
 * window size usually equals to (sampling_freq * window_size_in_sec)
 */
class SensorSharedData(samplingWindowSize: Int) {
    // Semaphores and locks for thread synchronization
    private val mProducerLock: ReentrantLock = ReentrantLock()
    private val mProducerSemaphore: Semaphore = Semaphore(0)
    private val mConsumerSemaphore: Semaphore = Semaphore(0)

    // Internal data arrays and indexes
    private val mSensorSharedDataImpl: SensorSharedDataImpl =
        SensorSharedDataImpl(samplingWindowSize)

    /**
     * Adds a new accelerometer's [SensorSample] to the window.
     *
     * @param[acc] A new accelerometer [SensorSample].
     * @throws InterruptedException in case the thread waiting is terminated.
     */
    fun putAcc(acc: SensorSample) {
        mProducerLock.lock()
        val full = mSensorSharedDataImpl.appendAcc(acc)
        if (full) {
            // Notify the consumer that the data is ready
            mConsumerSemaphore.release()
            // Wait for the consumer to copy the data
            mProducerSemaphore.acquire()
        }
        mProducerLock.unlock()
    }

    /**
     * Adds a new gyroscope's [SensorSample] to the window.
     *
     * @param[gyro] A new gyroscope [SensorSample].
     * @throws InterruptedException in case the thread waiting is terminated.
     */
    fun putGyro(gyro: SensorSample) {
        mProducerLock.lock()
        val full = mSensorSharedDataImpl.appendGyro(gyro)
        if (full) {
            // Notify the consumer that the data is ready
            mConsumerSemaphore.release()
            // Wait for the consumer to copy the data
            mProducerSemaphore.acquire()
        }
        mProducerLock.unlock()
    }

    /**
     * Wait until a window with sensors data is produced and return it.
     *
     * This method will wait until the producer has filled the processing
     * window with sensors data.
     *
     * @return the window as a [FloatArray].
     * @throws InterruptedException in case the thread waiting is terminated.
     */
    fun getData(): FloatArray {
        // Wait for all data to be produced
        mConsumerSemaphore.acquire()
        // Copy data to a thread-safe buffer
        val buffer = mSensorSharedDataImpl.window.clone()
        // Notify producer that we are done copying
        mProducerSemaphore.release()
        return buffer
    }
}

/**
 * Internal implementation of sensors data.
 *
 * This class implements methods to append accelerometer and gyroscope
 * [SensorSample]s filling a window of given size.
 *
 * @param[samplingWindowSize] Size of the sampling window; this parameter is used
 * to determine the window length.
 * @constructor Create a new instance of [SensorSharedDataImpl] passing a sampling
 * window size usually equals to (sampling_freq * window_size_in_sec)
 */
internal class SensorSharedDataImpl(samplingWindowSize: Int) {
    // The window length equals the samplingWindowSize * 6 channels for acc/gyro
    private val mWindowLength: Int = samplingWindowSize * 6
    private val mWindowData: FloatArray = FloatArray(mWindowLength) { 0.0f }
    private var mAccIdx: Int = 0
    private var mGyroIdx: Int = 0

    /**
     * The window of data created by accelerometer and gyroscope samples
     * represented as a 1D [FloatArray].
     */
    val window: FloatArray
        get() = mWindowData

    /**
     * Appends a new accelerometer [SensorSample].
     * @param[acc] The new accelerometer [SensorSample].
     * @return true if the underlying window is full, false otherwise.
     */
    fun appendAcc(acc: SensorSample): Boolean {
        mWindowData[mAccIdx + 0] = acc.x
        mWindowData[mAccIdx + 1] = acc.y
        mWindowData[mAccIdx + 2] = acc.z
        mAccIdx += 6

        if (mAccIdx >= mWindowLength || mGyroIdx >= mWindowLength) {
            mAccIdx = 0
            mGyroIdx = 0
            return true
        }
        return false
    }

    /**
     * Appends a new gyroscope [SensorSample].
     * @param[gyro] The new gyroscope [SensorSample].
     * @return true if the underlying window is full, false otherwise.
     */
    fun appendGyro(gyro: SensorSample): Boolean {
        mWindowData[mGyroIdx + 3] = gyro.x
        mWindowData[mGyroIdx + 4] = gyro.y
        mWindowData[mGyroIdx + 5] = gyro.z
        mGyroIdx += 6

        if (mAccIdx >= mWindowLength || mGyroIdx >= mWindowLength) {
            mAccIdx = 0
            mGyroIdx = 0
            return true
        }
        return false
    }
}
