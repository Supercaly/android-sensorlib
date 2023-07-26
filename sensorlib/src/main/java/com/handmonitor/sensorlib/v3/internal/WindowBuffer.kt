package com.handmonitor.sensorlib.v3.internal

import android.hardware.Sensor
import android.util.Log
import com.handmonitor.sensorlib.v3.SensorWindow

internal class WindowBuffer(size: Int) {
    companion object {
        private const val TAG = "WindowBuffer"
    }

    enum class Status {
        Full,
        NotFull
    }

    private val mCapacity: Int = size * 6
    private val mBuffer: FloatArray = FloatArray(mCapacity) { 0.0f }
    private var mAccIdx: Int = 0
    private var mGyrIdx: Int = 0

    val window: SensorWindow
        get() = mBuffer.toList()

    fun appendSample(data: SensorData): Status {
        when (data.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                if (mAccIdx < mCapacity) {
                    mBuffer[mAccIdx + 0] = data.values[0]
                    mBuffer[mAccIdx + 1] = data.values[1]
                    mBuffer[mAccIdx + 2] = data.values[2]
                    mAccIdx += 6
                }
            }

            Sensor.TYPE_GYROSCOPE -> {
                if (mGyrIdx < mCapacity) {
                    mBuffer[mGyrIdx + 3] = data.values[0]
                    mBuffer[mGyrIdx + 4] = data.values[1]
                    mBuffer[mGyrIdx + 5] = data.values[2]
                    mGyrIdx += 6
                }
            }

            else -> {
                Log.e(TAG, "newSample: unknown sensor type ${data.type}")
            }
        }
        if (mAccIdx >= mCapacity && mGyrIdx >= mCapacity) {
            mAccIdx = 0
            mGyrIdx = 0
            return Status.Full
        }
        return Status.NotFull
    }
}
