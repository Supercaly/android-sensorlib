package com.handmonitor.sensorslib

/**
 * Used for receiving notifications when there is new windowed
 * data from the sensors.
 *
 * This interface needs to be implemented by any class that want
 * to do something with the data received from the sensors.
 *
 * @see[SensorSharedData]
 * @see[SensorEventProducer]
 * @see[SensorEventConsumerRn]
 */
interface SensorDataHandler {
    /**
     * Called when there is a new batch of sensors data.
     *
     * This method is call every time the [SensorEventConsumerRn] is able
     * to consume an entire window of sensors data. The data passed as a
     * parameter is a 2d array flattened into a 1d array in row-major order.
     *
     * @param[data] Array of sensors values.
     */
    fun onNewData(data: FloatArray)
}
