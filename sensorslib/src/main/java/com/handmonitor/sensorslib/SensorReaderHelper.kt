package com.handmonitor.sensorslib

import android.content.Context
import android.os.Handler
import android.os.HandlerThread

/**
 * Class that helps manage all the sensor collecting
 * threads in an easy and secure way.
 *
 * This class is an helper designed to make life easier
 * for the users of this library removing the hassle of
 * creating and managing multiple threads and shared objects.
 *
 * @param[context] The application [Context].
 * @param[handler] Instance of a class that implements [SensorDataHandler].
 * @param[samplingWindowSize] Size of the sampling window.
 * @param[samplingPeriodMs] Period of the sampling window in milliseconds.
 *
 * @see[SensorSharedData]
 * @see[SensorEventProducer]
 * @see[SensorEventConsumerRn]
 */
class SensorReaderHelper(
    private val context: Context,
    handler: SensorDataHandler,
    private val samplingWindowSize: Int,
    private val samplingPeriodMs: Int
) {
    companion object {
        // Thread names
        private const val listenerThreadName = "SensorEventProducerThread"
        private const val consumerThreadName = "SensorEventConsumerThread"
    }

    // Producer/Consumer stuff
    private val mData: SensorSharedData = SensorSharedData(samplingWindowSize)
    private var mListener: SensorEventProducer? = null
    private val mConsumerRn: SensorEventConsumerRn = SensorEventConsumerRn(mData, handler)

    // Threads
    private var mListenerThread: HandlerThread? = null
    private var mConsumerThread: Thread? = null

    private var mStarted: Boolean = false

    /**
     * Indicates whether the current [SensorReaderHelper] instance
     * is started and collecting some data.
     */
    val isStarted
        get() = mStarted

    /**
     * Start the data collection process causing all
     * the managed classes to execute.
     *
     * The result is that a [SensorEventProducer] is producing
     * data from the sensors in it's own thread and a
     * [SensorEventConsumerRn] is transforming that data
     * in windows of given size.
     *
     * It is illegal to call this method multiple times without
     * stopping the process first.
     *
     * @throws IllegalThreadStateException If the process is already
     * started.
     * @see[stop]
     */
    fun start() {
        if (mStarted)
            throw IllegalThreadStateException()

        mListenerThread = HandlerThread(listenerThreadName)
        mListenerThread!!.start()
        mListener = SensorEventProducer(
            context,
            mData,
            Handler(mListenerThread!!.looper),
            samplingWindowSize,
            samplingPeriodMs
        )

        mConsumerThread = Thread(mConsumerRn, consumerThreadName)

        mListener!!.startListening()
        mConsumerThread!!.start()
        mStarted = true
    }

    /**
     * Stop the data collection process causing all
     * the managed classes to stop execution.
     *
     * Stopping an already stopped process does not have
     * ant effect.
     *
     * @see[start]
     */
    fun stop() {
        mStarted = false
        mListener?.stopListening()
        mListenerThread?.quit()
        mConsumerThread?.interrupt()

        mListener = null
        mListenerThread = null
        mConsumerThread = null
    }
}
