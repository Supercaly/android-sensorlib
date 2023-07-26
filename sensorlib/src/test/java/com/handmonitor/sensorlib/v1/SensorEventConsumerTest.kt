package com.handmonitor.sensorlib.v1

import com.google.common.truth.Truth.assertThat
import com.handmonitor.sensorlib.mockLog
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SensorEventConsumerTest {
    @MockK
    private lateinit var mData: SensorSharedData

    @BeforeEach
    fun setup() {
        mockLog()
    }

    @Test
    fun `onNewData called when new data is ready`() {
        val mockData = floatArrayOf(0.0f, 1.1f, 2.2f, 3.3f)
        every { mData.getData() } returns mockData

        val impl = object : SensorDataHandler {
            override fun onNewData(data: FloatArray) {
                assertThat(data).isEqualTo(mockData)
                throw InterruptedException()
            }
        }
        val runnable = SensorEventConsumerRn(mData, impl)
        runnable.run()

        verify {
            mData.getData()
        }
    }
}
