package com.example.digitalsilhouette.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.digitalsilhouette.MainActivity
import com.example.digitalsilhouette.R
import com.example.digitalsilhouette.data.DefaultDataRepository

class WifiDetectionJobService : JobService() {

    companion object {
        const val NOTIFICATION_ID = 5001
        const val CHANNEL_ID = "wifi_detection_channel"
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        checkCurrentNetwork()
        // Return false as the job is finished synchronously
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true // Reschedule if interrupted
    }

    private fun checkCurrentNetwork() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ssid = wifiManager.connectionInfo.ssid

            if (ssid == "\"AndroidWifi\"" || ssid == "<unknown ssid>" || ssid.isBlank()) {
                return
            }

            val repository = DefaultDataRepository(applicationContext)
            val isOffice = repository.targetWifiNetworks.value.contains(ssid)
            val isHome = repository.homeWifiNetworks.value.contains(ssid)

            // If it's a completely new network, send a push notification
            if (!isOffice && !isHome) {
                sendNotification(ssid)
            }
        }
    }

    private fun sendNotification(ssid: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Network Suggestions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Suggests adding new Wi-Fi networks to Focus Mode"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create an Intent to open the app (MainActivity) which will then show the popup
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("detected_ssid", ssid)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("New Wi-Fi Detected")
            .setContentText("Connected to $ssid. Tap to save as Office or Home network.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
