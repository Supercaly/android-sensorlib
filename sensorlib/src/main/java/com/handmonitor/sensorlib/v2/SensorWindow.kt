package com.handmonitor.sensorlib.v2

import java.nio.FloatBuffer

/**
 * The [SensorWindow] is a data class representing a single window
 * of sensor data.
 *
 * @property[buffer] Underlying data as a [FloatBuffer]
 */
data class SensorWindow(
    val buffer: FloatBuffer
) {
    companion object {
        /**
         * Creates an instance of [SensorWindow] from a given [FloatArray].
         *
         * This method makes a deep-copy of the array before using it, so using
         * this method is thread-safe.
         *
         * @param[array] The [FloatArray] to use as window data.
         * @return A new instance of [SensorWindow].
         */
        fun fromArray(array: FloatArray): SensorWindow {
            return SensorWindow(
                // Deep-copy the array so it's thread safe.
                FloatBuffer.wrap(array.clone())
            )
        }
    }

    /**
     * This method returns the underlying window data as
     * a [FloatArray].
     *
     * @return The data as a [FloatArray]
     */
    fun asArray(): FloatArray = buffer.array()
}
