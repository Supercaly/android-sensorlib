package com.handmonitor.sensorslib

/**
 * The class [SensorWindowBuffer] is a buffer for sensor
 * data of size determined by windowSize.
 *
 * The class has two methods for adding data to the buffer:
 * [pushAccelerometer] and [pushGyroscope].
 *
 * @param[windowSize] The size of the processing window; the
 * internal buffer will have a size of windowSize * 6.
 */
class SensorWindowBuffer(windowSize: Int) {
    private val mBufferSize: Int = windowSize * 6
    private val mBuffer: FloatArray = FloatArray(mBufferSize) { 0.0f }
    private var mAccIndex: Int = 0
    private var mGyroIndex: Int = 0

    /**
     * Returns the underlying window as a [SensorWindow].
     */
    val window: SensorWindow
        get() = SensorWindow.fromArray(mBuffer)

    /**
     * Method that takes a [FloatArray] as an argument, representing
     * the sensor data, and add it to the window in the correct location.
     * If the window is full true is returned, otherwise, false is returned.
     *
     * @param[values] Triple of accelerometer values.
     * @return true if the window is full, false otherwise.
     */
    fun pushAccelerometer(values: FloatArray): Boolean {
        mBuffer[mAccIndex + 0] = values[0]
        mBuffer[mAccIndex + 1] = values[1]
        mBuffer[mAccIndex + 2] = values[2]
        mAccIndex += 6
        if (mAccIndex >= mBufferSize || mGyroIndex >= mBufferSize) {
            mAccIndex = 0
            mGyroIndex = 0
            return true
        }
        return false
    }

    /**
     * Method that takes a [FloatArray] as an argument, representing
     * the sensor data, and add it to the window in the correct location.
     * If the window is full true is returned, otherwise, false is returned.
     *
     * @param[values] Triple of gyroscope values.
     * @return true if the window is full, false otherwise.
     */
    fun pushGyroscope(values: FloatArray): Boolean {
        mBuffer[mGyroIndex + 3] = values[0]
        mBuffer[mGyroIndex + 4] = values[1]
        mBuffer[mGyroIndex + 5] = values[2]
        mGyroIndex += 6
        if (mAccIndex >= mBufferSize || mGyroIndex >= mBufferSize) {
            mAccIndex = 0
            mGyroIndex = 0
            return true
        }
        return false
    }
}
