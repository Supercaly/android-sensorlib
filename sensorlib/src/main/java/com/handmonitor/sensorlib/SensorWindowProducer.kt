package com.handmonitor.sensorlib

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.util.concurrent.TimeUnit

/**
 * Callback invoked every time a new [SensorWindow]
 * has been produced.
 */
typealias OnNewWindow = (SensorWindow) -> Unit

/**
 * The class [SensorWindowProducer] implements the [SensorEventListener] callback
 * and listens to events from the accelerometer and gyroscope sensors generating
 * windows of sensor data.
 *
 * NOTE: This class collect the sensor data in his own thread that is managed
 * by this class. Be aware of race conditions!
 * @see[setOnNewWindowListener]
 * @see[startSensors]
 * @see[stopSensors]
 */
class SensorWindowProducer
/**
 * Creates a new instance of [SensorWindowProducer].
 *
 * @param[context] An instance of [Context].
 * @param[handlerThread] An instance of [HandlerThread] to use for sensor collection.
 * @param[samplingMs] The sampling rate in milliseconds as a [Long].
 * @param[windowSize] The processing window size as an [Int].
 * @throws[SensorNotSupportedException] If some sensor is not supported.
 */
internal constructor(
    context: Context,
    private val handlerThread: HandlerThread,
    samplingMs: Long,
    windowSize: Int
) : SensorEventListener {
    companion object {
        /**
         * Constant string representing the default name of the thread used
         * to handle sensor data collection.
         */
        const val SENSOR_HANDLER_THREAD_NAME = "SensorWindowProducerThread"
        private const val TAG = "SensorWindowProducer"
    }

    /**
     * Creates a new instance of [SensorWindowProducer].
     *
     * @param[context] An instance of [Context].
     * @param[samplingMs] The sampling rate in milliseconds as a [Long].
     * @param[windowSize] The processing window size as an [Int].
     * @throws[SensorNotSupportedException] If some sensor is not supported.
     */
    constructor(
        context: Context,
        samplingMs: Long,
        windowSize: Int
    ) : this(
        context,
        HandlerThread(SENSOR_HANDLER_THREAD_NAME),
        samplingMs,
        windowSize
    )

    // On new window listeners
    private var mOnNewWindowListener: OnNewWindow? = null

    // Time constants converted in microseconds
    private val samplingPeriodUs: Int = TimeUnit.MILLISECONDS.toMicros(samplingMs).toInt()
    private val maxLatencyUs: Int =
        TimeUnit.MILLISECONDS.toMicros((samplingMs * windowSize)).toInt()

    // Sensor manager and sensors
    private val mSensorManager: SensorManager
    private var mAccSensor: Sensor? = null
    private var mGyroSensor: Sensor? = null
    private var mIsListening = false

    // Sensor collection stuff
    private val mWindowBuffer = SensorWindowBuffer(windowSize)
    private val mAccFilter = SensorSampleFilter(samplingMs, "accelerometer")
    private val mGyroFilter = SensorSampleFilter(samplingMs, "gyroscope")

    init {
        mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (mAccSensor == null) {
            throw SensorNotSupportedException(Sensor.TYPE_ACCELEROMETER)
        }

        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (mGyroSensor == null) {
            throw SensorNotSupportedException(Sensor.TYPE_GYROSCOPE)
        }
    }

    /**
     * Returns true if we are currently listening to sensors;
     * false otherwise.
     */
    val isListening: Boolean
        get() = mIsListening

    /**
     * Register a callback to be invoked when a new window is
     * produced.
     *
     * @param[listener] Instance of [OnNewWindow]. Pass null to
     * remove the previous one.
     */
    fun setOnNewWindowListener(listener: OnNewWindow?) {
        mOnNewWindowListener = listener
    }

    /**
     * This method starts listening to sensor data in a separate thread
     * producing window of data and calling [OnNewWindow] every time one
     * is ready.
     *
     * Before calling this make sure to set a listener using [setOnNewWindowListener]
     * in order to receive all the windows when produced.
     *
     * @throws[IllegalThreadStateException] If the thread is started and was already running.
     * @see[setOnNewWindowListener]
     */
    fun startSensors() {
        if (mIsListening) {
            throw IllegalThreadStateException("thread is already running")
        }

        mIsListening = true
        handlerThread.start()
        val handler = Handler(handlerThread.looper)

        mSensorManager.registerListener(
            this,
            mAccSensor,
            samplingPeriodUs,
            maxLatencyUs,
            handler
        )
        mSensorManager.registerListener(
            this,
            mGyroSensor,
            samplingPeriodUs,
            maxLatencyUs,
            handler
        )
        Log.d(TAG, "startSensors: start listening to sensors!")
    }

    /**
     * This method stops listening to sensor data stopping the separate
     * thread that handles them.
     *
     * When called without previously calling [startSensors] nothing
     * will happen.
     */
    fun stopSensors() {
        mIsListening = false
        mSensorManager.unregisterListener(this)
        handlerThread.quit()
        Log.d(TAG, "stopSensors: stop listening to sensors!")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                if (mAccFilter.newSample(event) && mWindowBuffer.pushAccelerometer(event.values)) {
                    mOnNewWindowListener?.invoke(mWindowBuffer.window)
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                if (mGyroFilter.newSample(event) && mWindowBuffer.pushGyroscope(event.values)) {
                    mOnNewWindowListener?.invoke(mWindowBuffer.window)
                }
            }
            else -> {
                Log.w(TAG, "onSensorChanged: Unknown sensor type ${event?.sensor?.type}")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.i(TAG, "onAccuracyChanged: Changed accuracy of sensor '${sensor?.name}' to '$accuracy'")
    }
}
