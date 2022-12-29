package com.handmonitor.sensorslib

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot

/**
 * Function that mocks the [Log] class forwarding the prints to the
 * test console so we can still see them.
 */
fun mockLog() {
    val msg = slot<String>()
    val tag = slot<String>()

    mockkStatic(Log::class)

    every { Log.d(capture(tag), capture(msg)) } answers {
        println("Log.d: ${tag.captured}: ${msg.captured}")
        0
    }
    every { Log.w(capture(tag), capture(msg)) } answers {
        println("Log.w: ${tag.captured}: ${msg.captured}")
        0
    }
    every { Log.e(capture(tag), capture(msg)) } answers {
        println("Log.e: ${tag.captured}: ${msg.captured}")
        0
    }
    every { Log.i(capture(tag), capture(msg)) } answers {
        println("Log.i: ${tag.captured}: ${msg.captured}")
        0
    }
    every { Log.v(capture(tag), capture(msg)) } answers {
        println("Log.v: ${tag.captured}: ${msg.captured}")
        0
    }
    every { Log.wtf(capture(tag), capture(msg)) } answers {
        println("Log.wtf: ${tag.captured}: ${msg.captured}")
        0
    }
}
