package com.handmonitor.sensorslib

import android.hardware.SensorEvent
import android.util.Log

/**
 * A [SensorSampleFilter] is a class that helps during the process
 * of collecting [SensorEvent]s by accepting/discarding them
 * in order to maintain a constant sampling frequency.
 *
 * @param[samplingMs] The sampling period in milliseconds.
 */
class SensorSampleFilter(samplingMs: Long) {
    companion object {
        /**
         * Determines the range of acceptance of a [SensorEvent] in
         * nanoseconds.
         *
         * The total range is sampling period + SAMPLING_RANGE_NS.
         */
        const val SAMPLING_RANGE_NS = 5_000_000L
        private const val TAG = "SensorSampleFilter"
    }

    private var mLastTimeUs: Long? = null

    /**
     * Denotes the lower extreme of the sampling range.
     */
    val minRangeNs: Long = samplingMs.msToNs() - SAMPLING_RANGE_NS

    /**
     * Denotes the upper extreme of the sampling range.
     */
    val maxRangeNs: Long = samplingMs.msToNs() + SAMPLING_RANGE_NS

    /**
     * Return the last accepted timestamp in nanoseconds.
     * This value can be null if this object was never used.
     */
    val lastTimeNs: Long?
        get() = mLastTimeUs

    /**
     * This method is called every time we have a new [SensorEvent]
     * from a given sensor. This method accepts or rejects the event
     * based on his timestamp and on the timestamp of the last accepted
     * event.
     *
     * @return true if the event can be accepted, false otherwise.
     */
    fun newSample(event: SensorEvent): Boolean {
        if (mLastTimeUs == null) {
            mLastTimeUs = event.timestamp
            return false
        }

        val elapsed = event.timestamp - mLastTimeUs!!
        if (elapsed < minRangeNs) {
            Log.d(TAG, "newSample: got event with elapsed time '${elapsed}ns'")
            return false
        }
        mLastTimeUs = event.timestamp
        return true
    }
}
