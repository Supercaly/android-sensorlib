package com.handmonitor.sensorlib.v3.internal

import android.hardware.Sensor

internal fun sensorTypeToString(type: Int): String = when (type) {
    Sensor.TYPE_ACCELEROMETER -> "accelerometer"
    Sensor.TYPE_GYROSCOPE -> "gyroscope"
    else -> "unknown"
}
