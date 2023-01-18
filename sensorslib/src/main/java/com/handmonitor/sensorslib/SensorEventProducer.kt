package com.handmonitor.sensorslib

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

/**
 * A producer for sensors values in a dedicate thread.
 *
 * This class listen periodically to samples produced by the
 * sensors of the Android device like accelerometer and gyroscope
 * in his own dedicated thread and pass them to a shared [SensorSharedData].
 *
 * To use this class the user must start and stop the separate thread then
 * create a [Handler] that will offload all the work. The internal Android
 * [SensorEventListener] will post a new message for each senor event so it's
 * suggested to use a [HandlerThread] for automatically manage those works.
 *
 * Example usage:
 * ```
 * // Create the HandlerThread and start it
 * val thread = HandlerThread("threadName").apply {
 *      start()
 *      // Create the handler
 *      val handler = Handler(thread.looper)
 *      // Create the listener
 *      listener = SensorEventProducer(context, data, handler, WINDOW_SIZE, SAMPLING_PERIOD)
 * }
 * ```
 *
 * @property[mSensorSharedData] The values produced are passed to a shared [SensorSharedData].
 * @property[mHandler] The [Handler] for the separate [Thread].
 * @param[ctx] The application's [Context].
 * @param[samplingWindowSize] The size of the sampling window.
 * @param[samplingPeriodMs] The sampling period in milliseconds; this equals 1000/freq.
 * @constructor Crates an instance of [SensorEventProducer] with given [Context],
 * [SensorSharedData], [Handler], window size and sampling period.
 */
class SensorEventProducer(
    ctx: Context,
    private val mSensorSharedData: SensorSharedData,
    private val mHandler: Handler,
    samplingWindowSize: Int,
    samplingPeriodMs: Int,
) : SensorEventListener {
    companion object {
        const val SAMPLING_RANGE = 2_000_000
        private const val TAG = "SensorEventProducer"
    }

    // Constants obtained from configuration converted to microseconds
    private val mSamplingPeriodUs = samplingPeriodMs * 1_000
    private val mMaxLatencyUS = mSamplingPeriodUs * samplingWindowSize

    // Sensor manager and sensors
    private val mSensorManager: SensorManager
    private var mAccSensor: Sensor? = null
    private var mGyroSensor: Sensor? = null
    private var mIsListening = false

    private var mLastAccTs: Long? = null
    private var mLastGyroTs: Long? = null

    /**
     * Minimum value for the sampling rate range in nanoseconds.
     */
    private val minSamplingRange
        get() = (mSamplingPeriodUs * 1_000) - SAMPLING_RANGE

    /**
     * Maximum value for the sampling rate range in nanoseconds.
     */
    private val maxSamplingRange
        get() = (mSamplingPeriodUs * 1_000) + SAMPLING_RANGE

    init {
        mSensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (mAccSensor == null) {
            Log.w(TAG, "Accelerometer sensor is not supported!")
        }

        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (mGyroSensor == null) {
            Log.w(TAG, "Gyroscope sensor is not supported!")
        }
    }

    /**
     * Start listening to sensors events.
     *
     * This method starts listening to the accelerometer and gyroscope
     * sensors if they are supported and we are not already listening.
     *
     * @see [stopListening].
     */
    fun startListening() {
        if (!mIsListening) {
            mIsListening = true
            if (mAccSensor != null) {
                mSensorManager.registerListener(
                    this,
                    mAccSensor,
                    mSamplingPeriodUs,
                    mMaxLatencyUS,
                    mHandler
                )
            }
            if (mGyroSensor != null) {
                mSensorManager.registerListener(
                    this,
                    mGyroSensor,
                    mSamplingPeriodUs,
                    mMaxLatencyUS,
                    mHandler
                )
            }
            Log.d(TAG, "startListening: start listening to sensors!")
        }
    }

    /**
     * Stop listening to sensors events.
     *
     * This method is safe to call as many times we want,
     * even before [startListening].
     *
     * @see [startListening].
     */
    fun stopListening() {
        mIsListening = false
        mSensorManager.unregisterListener(this)
        Log.d(TAG, "stopListening: stop listening to sensors!")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // The first time mLastAccTs is null, we accept the sample no matter what
                if (mLastAccTs == null) {
                    mLastAccTs = event.timestamp
                    mSensorSharedData.putAcc(SensorSample.fromArray(event.values))
                } else {
                    // Compute the elapsed time
                    val elapsed = event.timestamp - mLastAccTs!!
                    if (elapsed >= minSamplingRange) {
                        // The elapsed time is >= than the minSamplingRange, so we accept it
                        mLastAccTs = event.timestamp
                        mSensorSharedData.putAcc(SensorSample.fromArray(event.values))
                        if (elapsed > maxSamplingRange) {
                            Log.d(
                                TAG,
                                "onSensorChanged: got a late accelerometer event '${elapsed}ns'"
                            )
                        }
                    }
                    // The elapsed time should not be negative... something strange happened
                    if (elapsed < 0) {
                        Log.wtf(
                            TAG,
                            "onSensorChanged: got an accelerometer event with negative elapsed time '${elapsed}ns'"
                        )
                    }
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                // The first time mLastGyroTs is null, we accept the sample no matter what
                if (mLastGyroTs == null) {
                    mLastGyroTs = event.timestamp
                    mSensorSharedData.putGyro(SensorSample.fromArray(event.values))
                } else {
                    // Compute the elapsed time
                    val elapsed = event.timestamp - mLastGyroTs!!
                    if (elapsed >= minSamplingRange) {
                        // The elapsed time is >= than the minSamplingRange, so we accept it
                        mLastGyroTs = event.timestamp
                        mSensorSharedData.putGyro(SensorSample.fromArray(event.values))
                        if (elapsed > maxSamplingRange) {
                            Log.d(
                                TAG,
                                "onSensorChanged: got a late gyroscope event '${elapsed}ns'"
                            )
                        }
                    }
                    // The elapsed time should not be negative... something strange happened
                    if (elapsed < 0) {
                        Log.wtf(
                            TAG,
                            "onSensorChanged: got an gyroscope event with negative elapsed time '${elapsed}ns'"
                        )
                    }
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
