package com.handmonitor.sensorlib.v3

import java.util.concurrent.TimeUnit

fun Long.toNs() = TimeUnit.MILLISECONDS.toNanos(this)
