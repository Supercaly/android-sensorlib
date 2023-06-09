package com.handmonitor.sensorlib.v2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn

/**
 * The extension function [SensorWindowProducer.asFlow] returns the
 * windows of sensor data produced by [SensorWindowProducer] as a [Flow].
 *
 * The methods [SensorWindowProducer.startSensors] and
 * [SensorWindowProducer.stopSensors] are called automatically by the [Flow]
 * when it starts and it is cancelled. For this reason if this method is called
 * when the producer is already listening to sensors it will throw an
 * [IllegalThreadStateException].
 *
 * The created [Flow] is already [conflate]d to avoid slow collectors and
 * runs in a separate thread using [Dispatchers.Default].
 *
 * NOTE: This method will override any listener previously set with
 * [SensorWindowProducer.setOnNewWindowListener].
 *
 * @receiver[SensorWindowProducer]
 * @return A [Flow] of [SensorWindow].
 */
fun SensorWindowProducer.asFlow(): Flow<SensorWindow> = callbackFlow {
    setOnNewWindowListener { window ->
        try {
            trySend(window)
        } catch (ex: Exception) {
            close(ex)
        }
    }
    startSensors()
    awaitClose { stopSensors() }
}.conflate().flowOn(Dispatchers.Default)
