package com.handmonitor.sensorlib.v3.internal

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.handmonitor.sensorlib.v3.SensorNotSupportedException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.TimeUnit

internal typealias SensorDataListenerCallback = (SensorData) -> Unit

internal class SensorDataListener(
    context: Context,
    val type: Int,
    private val samplingMs: Long,
    private val windowSize: Int
) : SensorEventListener {
    companion object {
        private const val TAG = "SensorDataListener"
    }

    private val mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val mSensor = mSensorManager.getDefaultSensor(type)
        ?: throw SensorNotSupportedException(type)
    private val mHandlerThread = HandlerThread("${sensorTypeToString(type)}-collection-thread")
    private var mCallback: SensorDataListenerCallback? = null
    private var mIsListening: Boolean = false

    val isListening: Boolean
        get() = mIsListening

    fun setCallback(cb: SensorDataListenerCallback) {
        mCallback = cb
    }

    fun start() {
        if (mIsListening) {
            throw IllegalThreadStateException("thread is already running")
        }

        mHandlerThread.start()
        val handler = Handler(mHandlerThread.looper)
        mSensorManager.registerListener(
            this,
            mSensor,
            TimeUnit.MILLISECONDS.toMicros(samplingMs).toInt(),
            TimeUnit.MILLISECONDS.toMicros((samplingMs * windowSize)).toInt(),
            handler
        )
        mIsListening = true
    }

    fun stop() {
        mSensorManager.unregisterListener(this)
        mHandlerThread.quit()
        mIsListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            val data = SensorData(
                event.values,
                event.sensor.type,
                event.timestamp
            )
            mCallback?.invoke(data)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.i(TAG, "onAccuracyChanged: new accuracy: $accuracy")
    }

    fun asFlow(): Flow<SensorData> {
        return callbackFlow {
            setCallback { data ->
                trySend(data).exceptionOrNull().also { ex ->
                    if (ex != null) {
                        Log.e(
                            "sensorDataFlow",
                            "sensorDataFlow: error in flow of ${sensorTypeToString(type)}: ",
                            ex
                        )
                    }
                }
            }
            start()
            awaitClose { stop() }
        }
    }
}
