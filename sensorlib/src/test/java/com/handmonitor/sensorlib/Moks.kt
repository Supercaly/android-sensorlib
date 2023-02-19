package com.handmonitor.sensorlib

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot

/**
 * Function that mocks the [Log] class forwarding the prints to the
 * test console so we can still see them.
 */
fun mockLog() {
    val msg = slot<String>()
    val tag = slot<String>()

    mockkStatic(Log::class)

    every { Log.d(capture(tag), capture(msg)) } answers {
        println("Log.d: ${tag.captured}: ${msg.captured}")
        0
    }
    every { Log.w(capture(tag), capture(msg)) } answers {
        println("Log.w: ${tag.captured}: ${msg.captured}")
        0
    }
    every { Log.e(capture(tag), capture(msg)) } answers {
        println("Log.e: ${tag.captured}: ${msg.captured}")
        0
    }
    every { Log.i(capture(tag), capture(msg)) } answers {
        println("Log.i: ${tag.captured}: ${msg.captured}")
        0
    }
    every { Log.v(capture(tag), capture(msg)) } answers {
        println("Log.v: ${tag.captured}: ${msg.captured}")
        0
    }
    every { Log.wtf(capture(tag), capture(msg)) } answers {
        println("Log.wtf: ${tag.captured}: ${msg.captured}")
        0
    }
}

/**
 * Function that helps create a mock for Android's native [SensorEvent] class.
 * This class is internal to the Android's SDK and can be mocked only using reflection.
 *
 * Inspired by this StackOverflow posts:
 *  - https://stackoverflow.com/questions/2806976/how-can-i-unit-test-an-android-activity-that-acts-on-accelerometer
 *  - https://source.chromium.org/chromium/chromium/src/+/main:services/device/generic_sensor/android/junit/src/org/chromium/device/sensors/PlatformSensorAndProviderTest.java
 *
 * @param[sensor] The mock sensor that generated the event.
 * @param[accuracy] The accuracy of the mocked event.
 * @param[timestamp] The timestamp of the mocked event.
 * @param[values] The values of the mocked event.
 * @return A mocked instance of [SensorEvent]
 */
fun mockSensorEvent(
    sensor: Sensor? = null,
    accuracy: Int = 0,
    timestamp: Long = 0L,
    values: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)
): SensorEvent {
    val eventConstructor = SensorEvent::class.java.getDeclaredConstructor()
    eventConstructor.isAccessible = true
    val sensorEvent = eventConstructor.newInstance()

    sensorEvent.sensor = sensor
    sensorEvent.accuracy = accuracy
    sensorEvent.timestamp = timestamp

    val valuesField = sensorEvent.javaClass.getField("values")
    valuesField.isAccessible = true
    valuesField.set(sensorEvent, values)

    return sensorEvent
}
