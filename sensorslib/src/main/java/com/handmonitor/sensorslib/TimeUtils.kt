package com.handmonitor.sensorslib

/**
 * Extension function that converts a time in
 * milliseconds to a time in nanoseconds.
 *
 * @receiver A [Long] time in milliseconds.
 * @return A [Long] time in nanoseconds.
 */
fun Long.msToNs() = this * 1_000_000L
