package com.handmonitor.sensorslib

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class SensorWindowTest {
    @Test
    fun `fromArray creates new SensorWindow cloning the array`() {
        val array = FloatArray(100) {0.0f}
        val window  = SensorWindow.fromArray(array)

        assertThat(window.buffer.array()).isNotSameInstanceAs(array)
    }
}
