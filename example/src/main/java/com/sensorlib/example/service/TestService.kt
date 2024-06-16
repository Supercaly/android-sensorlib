package com.sensorlib.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.handmonitor.sensorlib.v1.SensorDataHandler
import com.handmonitor.sensorlib.v1.SensorReaderHelper
import com.handmonitor.sensorlib.v2.SensorWindowProducer
import com.handmonitor.sensorlib.v2.asFlow
import com.handmonitor.sensorlib.v3.SensorFlow
import com.sensorlib.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class TestService : Service() {
    companion object {
        private const val TAG = "TestService"

        private const val NOTIFICATION_CHANNEL_ID = "test_channel"

        val version = LibVersion.V1

        const val sampling = 10L
        const val size = 200
    }

    enum class LibVersion {
        V1,
        V2,
        V3
    }

    private val mServiceScope = CoroutineScope(Dispatchers.Default + Job())

    private var t = System.currentTimeMillis()

    private val cb = object : SensorDataHandler {
        override fun onNewData(data: FloatArray) {
            Log.d(TAG, "onNewData: ${System.currentTimeMillis() - t}")
            t = System.currentTimeMillis()
        }
    }
    private val rh = SensorReaderHelper(this@TestService, cb, sampling.toInt(), size)

    private fun getNotification(): Notification {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Test Service notifications channel",
            NotificationManager.IMPORTANCE_HIGH
        ).let {
            it.description = "Test Service channel"
            it.enableLights(true)
            it
        }
        notificationManager.createNotificationChannel(channel)

        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Test Service")
            .setContentText("This service is used to test sensorlib")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Ticker")
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: ")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ")

        startForeground(5, getNotification())

        mServiceScope.launch {
            Log.d(TAG, "onStartCommand: Start")

            when (version) {
                LibVersion.V1 -> {
                    rh.start()
                }

                LibVersion.V2 -> {
                    SensorWindowProducer(this@TestService, sampling, size).asFlow()
                        .onEach {
                            Log.d(TAG, "onEach: ${System.currentTimeMillis() - t}")
                            t = System.currentTimeMillis()
                        }
                        .onCompletion {
                            Log.d(TAG, "onCompletion: it=$it")
                        }
                        .collect()
                }

                LibVersion.V3 -> {
                    SensorFlow(this@TestService, sampling, size).asFlow()
                        .onEach {
                            Log.d(TAG, "onEach3: ${System.currentTimeMillis() - t}")
                            t = System.currentTimeMillis()
                        }
                        .onCompletion {
                            Log.d(TAG, "onCompletion3: ")
                        }
                        .collect()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        mServiceScope.cancel()
    }
}
