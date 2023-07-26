package com.handmonitor.sensorlib.v2

/**
 * Thrown when the requested sensor is not supported by the device.
 *
 * @param[sensorType] Type of the sensor not supported.
 * @see[SensorWindowProducer]
 */
class SensorNotSupportedException(sensorType: Int) :
    Exception("Sensor of type '$sensorType' is not supported!")
