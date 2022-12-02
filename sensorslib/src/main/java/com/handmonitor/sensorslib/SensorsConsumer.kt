package com.handmonitor.sensorslib

import android.util.Log

/**
 * The [SensorsConsumer] interface should be implemented by any class
 * that want to do something with the data received from the sensors.
 *
 * @see[SensorsData]
 * @see[SensorsConsumerRn]
 * @see[SensorsListener]
 */
interface SensorsConsumer {
    /**
     * This method is called every time a new list of sensors events
     * is produced; the class implementing the [SensorsConsumer] interface
     * will do his work in this method.
     *
     * @param[data] List of sensors values.
     */
    fun onNewData(data: FloatArray)
}

/**
 * A consumer of sensors values in a dedicated thread.
 *
 * This class waits for [SensorsData] to produce a full list of
 * sensors values and then calls [SensorsConsumer] interface with it. All the work
 * is done in a dedicated thread that the user needs to spawn.
 *
 * Example usage:
 * ```
 * // implement the SensorsConsumer interface with user's logic
 * val impl: Do = object : Do {
 *      override fun done(data: FloatArray) {
 *          // do something with data
 *      }
 * }
 * // create a SensorsConsumerRn
 * val c: SensorsConsumerRn = SensorsConsumerRn(data, impl)
 * // create a new Thread preferably using [threadName] as the name of thread
 * val t: Thread = Thread(c, SensorsConsumerRn.threadName)
 * // later start the thread using
 * t.start()
 * // and stop it using
 * t.interrupt()
 * ```
 *
 * @property[mData] A shared [SensorsData] that contains the values needed.
 * @property[mConsumer] A class that implements [SensorsConsumer] to do something with the
 * collected data.
 */
class SensorsConsumerRn(
    private val mData: SensorsData,
    private val mConsumer: SensorsConsumer
) : Runnable {
    companion object {
        /**
         * Name of the thread that should be used to manage [SensorsConsumerRn].
         *
         * @see [SensorsConsumerRn]
         */
        const val threadName = "SensorsConsumerThread"
        private const val TAG = "SensorsConsumerRunnable"
    }

    override fun run() {
        while (!Thread.currentThread().isInterrupted) {
            try {
                // Get the new data and pass it to SensorsConsumer implementation
                mConsumer.onNewData(mData.getData())
            } catch (ie: InterruptedException) {
                Log.d(TAG, "run: $threadName stopped!")
                Thread.currentThread().interrupt()
            }
        }
    }
}
