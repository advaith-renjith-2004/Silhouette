package com.example.digitalsilhouette.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.digitalsilhouette.R
import com.example.digitalsilhouette.data.DefaultDataRepository
import kotlin.math.abs

class NativeActivityService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var repository: DefaultDataRepository

    // Physics parameters
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var stillStartTime: Long = 0
    private val STILL_THRESHOLD = 0.5f // m/s^2 variance allowance
    private val STILL_DURATION_MS = 3000L // 3 seconds of perfect stillness required

    companion object {
        const val NOTIFICATION_ID = 5002
        const val CHANNEL_ID = "native_activity_channel"
        const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"
    }

    override fun onCreate() {
        super.onCreate()
        repository = DefaultDataRepository.getInstance(applicationContext)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> startTracking()
            ACTION_STOP_TRACKING -> stopTracking()
        }
        return START_NOT_STICKY
    }

    private fun startTracking() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kinetix Active")
            .setContentText("Monitoring physical activity...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        repository.logEvent("Native Activity Tracking started. Waiting for STILL state.")
        
        stillStartTime = System.currentTimeMillis()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: run {
            repository.logEvent("ERROR: No accelerometer found on device.")
            stopSelf()
        }
    }

    private fun stopTracking() {
        sensorManager.unregisterListener(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val deltaX = abs(lastX - x)
            val deltaY = abs(lastY - y)
            val deltaZ = abs(lastZ - z)

            if (deltaX > STILL_THRESHOLD || deltaY > STILL_THRESHOLD || deltaZ > STILL_THRESHOLD) {
                // Movement detected, reset the still timer
                stillStartTime = System.currentTimeMillis()
            } else {
                // It is currently still. Check if it's been still long enough
                val timeStill = System.currentTimeMillis() - stillStartTime
                if (timeStill >= STILL_DURATION_MS) {
                    onStillDetected()
                }
            }

            lastX = x
            lastY = y
            lastZ = z
        }
    }

    private fun onStillDetected() {
        repository.logEvent("Native physics declared STILL. Phone is stationary.")
        
        // Unregister to save battery immediately
        sensorManager.unregisterListener(this)

        // Hand off to Tier 2 (FocusService)
        val intent = Intent(this, FocusService::class.java).apply {
            action = FocusService.ACTION_CHECK_ENVIRONMENT
        }
        startService(intent)

        // Stop this foreground service
        stopTracking()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Activity Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Kinetix alive while monitoring movement"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
