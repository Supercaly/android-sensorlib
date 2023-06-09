package com.handmonitor.sensorlib.v3

import android.content.Context
import android.hardware.Sensor
import android.util.Log
import com.handmonitor.sensorlib.v3.internal.SensorData
import com.handmonitor.sensorlib.v3.internal.SensorDataListener
import com.handmonitor.sensorlib.v3.internal.WindowBuffer
import com.handmonitor.sensorlib.v3.internal.filterSamples
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class SensorFlow(
    context: Context,
    samplingMs: Long,
    windowSize: Int
) {
    private val mAccListener = SensorDataListener(
        context,
        Sensor.TYPE_ACCELEROMETER,
        samplingMs,
        windowSize
    )
    private val mGyrListener = SensorDataListener(
        context,
        Sensor.TYPE_GYROSCOPE,
        samplingMs,
        windowSize
    )
    private val mWindowBuffer = WindowBuffer(windowSize)
    private val mSensorChannel = Channel<SensorData>()
    private val mSensorFlowImpl = SensorFlowImpl(
        samplingMs,
        mWindowBuffer,
        mSensorChannel,
        mAccListener,
        mGyrListener
    )

    fun asFlow(dispatcher: CoroutineDispatcher = Dispatchers.Default): Flow<SensorWindow> =
        mSensorFlowImpl.asFlow(dispatcher)
}

internal class SensorFlowImpl(
    private val samplingMs: Long,
    private val windowBuffer: WindowBuffer,
    private val sensorChannel: Channel<SensorData>,
    private val accListener: SensorDataListener,
    private val gyrListener: SensorDataListener
) {
    companion object {
        private const val TAG = "SensorFlowImpl"
    }

    fun asFlow(dispatcher: CoroutineDispatcher): Flow<SensorWindow> {
        return channelFlow {
            coroutineScope {
                Log.i(TAG, "sensorFlow: current thread: ${Thread.currentThread().name}")
                launch {
                    accListener.asFlow()
                        .filterSamples(samplingMs)
                        .collect { sensorChannel.send(it) }
                }
                launch {
                    gyrListener.asFlow()
                        .filterSamples(samplingMs)
                        .collect { sensorChannel.send(it) }
                }
                launch {
                    for (event in sensorChannel) {
                        if (windowBuffer.appendSample(event) == WindowBuffer.Status.Full) {
                            this@channelFlow.send(windowBuffer.window)
                        }
                    }
                }
            }
        }.flowOn(dispatcher)
    }
}
