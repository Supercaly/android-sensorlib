package com.handmonitor.sensorlib.v3

class SensorNotSupportedException(sensorType: Int) :
    Exception("Sensor of type '$sensorType' is not supported!")
