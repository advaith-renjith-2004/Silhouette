package com.example.digitalsilhouette.data

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build

class SystemMuter(private val context: Context) {
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  private var originalRingerMode: Int? = null
  private var originalInterruptionFilter: Int? = null

  fun mute() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
        originalInterruptionFilter = notificationManager.currentInterruptionFilter
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
      } else {
        originalRingerMode = audioManager.ringerMode
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
      }
    } catch (e: Exception) {
      e.printStackTrace()
      try {
        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
      } catch (ex: Exception) {
        ex.printStackTrace()
      }
    }
  }

  fun unmute() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
        originalInterruptionFilter?.let {
          notificationManager.setInterruptionFilter(it)
        }
      } else {
        originalRingerMode?.let {
          audioManager.ringerMode = it
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      originalRingerMode = null
      originalInterruptionFilter = null
    }
  }
}
