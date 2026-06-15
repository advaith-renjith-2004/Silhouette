package com.example.digitalsilhouette.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.digitalsilhouette.data.DefaultDataRepository
import com.example.digitalsilhouette.data.SystemMuter

class FocusService : Service(), SensorEventListener {

  private lateinit var sensorManager: SensorManager
  private var accelerometer: Sensor? = null
  private lateinit var repository: DefaultDataRepository
  private lateinit var systemMuter: SystemMuter

  private var isFocusActive = false
  private var sessionStartTime: Long = 0

  // Debouncing variables
  private val DEBOUNCE_TIME_MS = 2000L
  private val MIN_SESSION_DURATION_SEC = 5L

  private var tempFaceDown = false
  private var faceDownTimeStart = 0L

  private var tempFaceUp = false
  private var faceUpTimeStart = 0L

  companion object {
    private const val CHANNEL_ID = "digital_silhouette_channel"
    private const val FOREGROUND_NOTIFICATION_ID = 1001
    private const val SUMMARY_NOTIFICATION_ID = 1002
  }

  override fun onCreate() {
    super.onCreate()
    repository = DefaultDataRepository.getInstance(this)
    systemMuter = SystemMuter(this)
    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    createNotificationChannel()
    startServiceForeground()
    registerSensor()
    repository.setServiceRunning(true)
    repository.logEvent("Service created. Registering orientation sensors.")
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    repository.logEvent("Service started via StartCommand.")
    return START_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun registerSensor() {
    accelerometer?.let {
      sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
      repository.logEvent("Registered Accelerometer listener.")
    } ?: run {
      repository.logEvent("Error: Accelerometer not supported on this device.")
    }
  }

  private fun unregisterSensor() {
    sensorManager.unregisterListener(this)
    repository.logEvent("Unregistered Accelerometer listener.")
  }

  override fun onSensorChanged(event: SensorEvent) {
    if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

    val x = event.values[0]
    val y = event.values[1]
    val z = event.values[2]

    val currentTime = System.currentTimeMillis()

    // z < -8.5f means screen is facing downwards
    // abs(x) < 2.0f and abs(y) < 2.0f means phone is flat
    val isCurrentlyFaceDown = z < -8.5f && Math.abs(x) < 2.0f && Math.abs(y) < 2.0f

    if (isCurrentlyFaceDown) {
      if (!tempFaceDown) {
        tempFaceDown = true
        faceDownTimeStart = currentTime
        repository.logEvent("Flat face-down position detected (z=${String.format("%.2f", z)}). Debouncing...")
      } else if (currentTime - faceDownTimeStart >= DEBOUNCE_TIME_MS) {
        if (!isFocusActive) {
          startFocusSession()
        }
      }
      tempFaceUp = false
    } else {
      if (!tempFaceUp) {
        tempFaceUp = true
        faceUpTimeStart = currentTime
        if (isFocusActive) {
          repository.logEvent("Device orientation changed. Debouncing lift detection...")
        }
      } else if (currentTime - faceUpTimeStart >= DEBOUNCE_TIME_MS) {
        if (isFocusActive) {
          stopFocusSession()
        }
      }
      tempFaceDown = false
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    repository.logEvent("Sensor accuracy changed: $accuracy")
  }

  private fun startFocusSession() {
    isFocusActive = true
    sessionStartTime = System.currentTimeMillis()
    systemMuter.mute()
    repository.setFocusActive(true, sessionStartTime)
    updateForegroundNotification("Deep Work Active", "Muting notifications. Flip over to finish.")
    repository.logEvent("Focus mode entered. Muting system notifications.")
  }

  private fun stopFocusSession() {
    isFocusActive = false
    val endTime = System.currentTimeMillis()
    val durationSeconds = (endTime - sessionStartTime) / 1000
    systemMuter.unmute()
    repository.setFocusActive(false, null)
    updateForegroundNotification("Monitoring Active", "Place device face-down to start deep work.")
    repository.logEvent("Focus mode ended. Restoring system sounds. Elapsed: ${durationSeconds}s.")

    if (durationSeconds >= MIN_SESSION_DURATION_SEC) {
      repository.addSession(sessionStartTime, endTime, durationSeconds)
      showSessionSummaryNotification(durationSeconds)
    } else {
      repository.logEvent("Session discarded. Duration was less than minimum ${MIN_SESSION_DURATION_SEC}s.")
    }

    sessionStartTime = 0
    tempFaceDown = false
    tempFaceUp = false
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Kinetix Focus Monitor",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Monitors phone orientation for focus mode"
      }
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }

  private fun startServiceForeground() {
    val notification = buildNotification("Monitoring Active", "Place device face-down to start deep work.")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    } else {
      startForeground(FOREGROUND_NOTIFICATION_ID, notification)
    }
  }

  private fun buildNotification(title: String, text: String): Notification {
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(title)
      .setContentText(text)
      .setSmallIcon(android.R.drawable.ic_lock_lock)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .build()
  }

  private fun updateForegroundNotification(title: String, text: String) {
    val notification = buildNotification(title, text)
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(FOREGROUND_NOTIFICATION_ID, notification)
  }

  private fun showSessionSummaryNotification(durationSeconds: Long) {
    val durationText = if (durationSeconds >= 60) {
      "${durationSeconds / 60}m ${durationSeconds % 60}s"
    } else {
      "${durationSeconds}s"
    }
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Focus Session Complete!")
      .setContentText("You just had $durationText of uninterrupted deep work!")
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .build()

    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(SUMMARY_NOTIFICATION_ID, notification)
  }

  override fun onDestroy() {
    if (isFocusActive) {
      stopFocusSession()
    }
    unregisterSensor()
    repository.setServiceRunning(false)
    repository.logEvent("Service destroyed. System offline.")
    super.onDestroy()
  }
}
