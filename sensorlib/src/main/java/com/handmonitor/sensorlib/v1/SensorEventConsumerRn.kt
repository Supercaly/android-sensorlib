package com.handmonitor.sensorlib.v1

import android.util.Log

/**
 * A consumer of sensors values in a dedicated thread.
 *
 * This class waits for [SensorSharedData] to produce a full list of
 * sensors values and then calls [SensorDataHandler] interface with it.
 * All the work is done in a dedicated thread that the user needs to spawn.
 *
 * Example usage:
 * ```
 * // implement the SensorDataHandler interface with user's logic
 * val handler: SensorDataHandler = object : SensorDataHandler {
 *      override fun onNewData(data: FloatArray) {
 *          // do something with data
 *      }
 * }
 * // create a SensorEventConsumerRn
 * val c: SensorEventConsumerRn = SensorEventConsumerRn(data, handler)
 * // create a new Thread
 * val t: Thread = Thread(c, "SensorEventConsumerThread")
 * // later start the thread using
 * t.start()
 * // and stop it using
 * t.interrupt()
 * ```
 *
 * @property[mData] A shared [SensorSharedData] that contains the values needed.
 * @property[mHandler] A class that implements [SensorDataHandler] to do something with the
 * collected data.
 */
class SensorEventConsumerRn(
    private val mData: SensorSharedData,
    private val mHandler: SensorDataHandler
) : Runnable {
    companion object {
        private const val TAG = "SensorsConsumerRunnable"
    }

    override fun run() {
        while (!Thread.currentThread().isInterrupted) {
            try {
                // Get the new data and pass it to SensorsConsumer implementation
                mHandler.onNewData(mData.getData())
            } catch (ie: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        Log.d(TAG, "run: ${Thread.currentThread().name} stopped!")
    }
}
