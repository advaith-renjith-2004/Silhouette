package com.example.digitalsilhouette.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import com.example.digitalsilhouette.data.DefaultDataRepository
import com.example.digitalsilhouette.data.SystemMuter
import kotlinx.coroutines.*
import kotlin.coroutines.resume

class FocusService : Service() {

    private lateinit var sensorManager: SensorManager
    private lateinit var repository: DefaultDataRepository
    private lateinit var systemMuter: SystemMuter
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    companion object {
        const val ACTION_CHECK_ENVIRONMENT = "com.example.digitalsilhouette.ACTION_CHECK_ENVIRONMENT"
        private const val CHANNEL_ID = "digital_silhouette_channel"
    }

    override fun onCreate() {
        super.onCreate()
        repository = DefaultDataRepository.getInstance(this)
        systemMuter = SystemMuter(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CHECK_ENVIRONMENT) {
            repository.logEvent("Tier 2: Service awoken by STILL transition. Checking environment...")
            serviceScope.launch {
                performTieredChecks()
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun performTieredChecks() {
        // Inform user that tier 1 passed
        withContext(Dispatchers.Main) {
            StatusOverlayController.show(applicationContext, "Target Wi-Fi Detected: Checking environment...")
        }

        // Tier 2: Check Wi-Fi
        if (!checkWifiNetwork()) {
            repository.logEvent("Tier 2 Failed: Not on target Wi-Fi network.")
            stopSelf()
            return
        }

        // Tier 2: Check Accelerometer
        val isFaceDown = checkOrientationOnce()
        if (!isFaceDown) {
            repository.logEvent("Tier 2 Failed: Phone is not face down.")
            stopSelf()
            return
        }

        // Tier 3: Brief Audio Check
        val decibelCheckPassed = performAudioCheck()
        if (decibelCheckPassed) {
            withContext(Dispatchers.Main) {
                StatusOverlayController.show(applicationContext, "Quiet environment confirmed. Audio scan passed.")
            }
            
            repository.logEvent("Tier 3 Passed: Audio environment is suitable. Muting phone.")
            
            withContext(Dispatchers.Main) {
                StatusOverlayController.show(applicationContext, "Entering the Zone. Muting distractions.")
            }

            // Set phone's profile and go back to sleep
            withContext(Dispatchers.Main) {
                systemMuter.mute()
                repository.setFocusActive(true, System.currentTimeMillis())
            }
        } else {
            repository.logEvent("Tier 3 Failed: Environment too loud. Going to sleep.")
        }
        
        // Go back to sleep
        stopSelf()
    }

    private fun checkWifiNetwork(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            repository.logEvent("Wi-Fi check failed: Missing Location Permission")
            return true // Fallback to allow for demo if permissions missing
        }

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo
            val ssid = info.ssid
            
            // For the sake of the Android emulator which uses AndroidWifi
            if (ssid == "\"AndroidWifi\"" || ssid == "<unknown ssid>") {
                return true
            }
            
            if (repository.homeWifiNetworks.value.contains(ssid)) {
                repository.logEvent("Tier 2 Abort: Connected to Home Wi-Fi ($ssid). No DND required.")
                return false
            }

            return repository.targetWifiNetworks.value.contains(ssid)
        }
        return false
    }

    private suspend fun checkOrientationOnce(): Boolean = suspendCancellableCoroutine { continuation ->
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val z = event.values[2]
                    // Unregister immediately to save battery
                    sensorManager.unregisterListener(this)
                    // Check if face down
                    val isFaceDown = z < -8.5f
                    if (continuation.isActive) {
                        continuation.resume(isFaceDown)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        
        // Timeout just in case
        serviceScope.launch {
            delay(1000)
            if (continuation.isActive) {
                sensorManager.unregisterListener(listener)
                continuation.resume(false)
            }
        }
    }

    private suspend fun performAudioCheck(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            repository.logEvent("Audio check skipped: Missing Microphone Permission")
            return true // Fallback
        }

        var recorder: MediaRecorder? = null
        return try {
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            recorder.setOutputFile(cacheDir.absolutePath + "/test_audio.3gp")
            recorder.prepare()
            recorder.start()
            
            repository.logEvent("Recording ambient audio for 1 second...")
            delay(1000) // 1 second brief execution check
            
            val amplitude = recorder.maxAmplitude
            repository.logEvent("Max audio amplitude registered: $amplitude")
            
            // Example condition: Only activate if the room isn't extremely loud (amplitude < 10000)
            // Just for demonstration logic.
            amplitude < 10000 
            
        } catch (e: Exception) {
            repository.logEvent("Audio check failed: ${e.message}")
            true
        } finally {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kinetix Focus Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors environment for focus mode"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
