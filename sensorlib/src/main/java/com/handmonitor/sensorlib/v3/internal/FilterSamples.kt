package com.handmonitor.sensorlib.v3.internal

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import java.util.concurrent.TimeUnit

internal fun Flow<SensorData>.filterSamples(samplingMs: Long): Flow<SensorData> {
    val rangeNs = 5_000_000L
    val minRangeNs: Long = TimeUnit.MILLISECONDS.toNanos(samplingMs) - rangeNs
    var lastTimeNs: Long? = null
    return transform { event ->
        if (lastTimeNs != null) {
            val elapsed = event.timestamp - lastTimeNs!!
            if (elapsed < minRangeNs) {
                Log.d(
                    "filterSamples",
                    "filterSamples: discarded ${sensorTypeToString(event.type)} event with time '${elapsed}ns'"
                )
                return@transform
            }
        }
        lastTimeNs = event.timestamp
        this.emit(event)
    }
}
