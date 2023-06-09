package com.handmonitor.sensorlib.v3.internal

internal data class SensorData(
    val values: FloatArray,
    val type: Int,
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorData

        if (!values.contentEquals(other.values)) return false
        if (type != other.type) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = values.contentHashCode()
        result = 31 * result + type
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
