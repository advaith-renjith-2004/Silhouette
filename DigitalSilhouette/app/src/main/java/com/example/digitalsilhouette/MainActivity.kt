package com.example.digitalsilhouette

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.digitalsilhouette.theme.DigitalSilhouetteTheme
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import com.example.digitalsilhouette.service.WifiDetectionJobService

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    
    // Schedule Wi-Fi background job
    scheduleWifiDetectionJob()

    // Pass any detected SSID from the notification to the UI
    val detectedSsid = intent?.getStringExtra("detected_ssid")

    setContent {
      DigitalSilhouetteTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation(detectedSsid) } }
    }
  }

  private fun scheduleWifiDetectionJob() {
    val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    val componentName = ComponentName(this, WifiDetectionJobService::class.java)
    val jobInfo = JobInfo.Builder(1, componentName)
        .setPeriodic(15 * 60 * 1000)
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .build()
    jobScheduler.schedule(jobInfo)
  }
}
