package com.handmonitor.sensorlib

import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SensorWindowFlowTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `sensorsFlow starts, stops and produces values correctly`() = runTest {
        val producer = mockk<SensorWindowProducer>()
        val listenerSlot = slot<OnNewWindow>()
        every { producer.startSensors() } just Runs
        every { producer.stopSensors() } just Runs
        every { producer.setOnNewWindowListener(capture(listenerSlot)) } just Runs

        var lastCollectedItem: SensorWindow? = null
        val itemsToSend = listOf(
            SensorWindow.fromArray(FloatArray(600) { 0.0f }),
            SensorWindow.fromArray(FloatArray(600) { 1.0f }),
            SensorWindow.fromArray(FloatArray(600) { 2.0f })
        )

        // Collecting the flow calls startSensors
        val flow = producer.asFlow().onEach { lastCollectedItem = it }
        val collectJob = launch { flow.collect() }
        advanceUntilIdle()
        verify { producer.startSensors() }

        // Produced windows are sent in order
        itemsToSend.forEach {
            listenerSlot.captured(it)
            advanceUntilIdle()
            assertThat(lastCollectedItem).isEqualTo(it)
            assertThat(lastCollectedItem).isSameInstanceAs(it)
        }

        // Cancelling the flow calls stopSensors
        collectJob.cancel()
        advanceUntilIdle()
        verify { producer.stopSensors() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `sensorsFlow propagates errors`() = runTest {
        val producer = mockk<SensorWindowProducer>()
        every { producer.setOnNewWindowListener(any()) } just Runs
        every { producer.startSensors() } throws IllegalThreadStateException()
        every { producer.stopSensors() } just Runs

        val flow = producer.asFlow().catch {
            assertThat(it).isInstanceOf(IllegalThreadStateException::class.java)
        }
        val collectJob = launch { flow.collect() }
        advanceUntilIdle()
        verify {
            producer.startSensors()
            collectJob.isCancelled
        }
    }
}
