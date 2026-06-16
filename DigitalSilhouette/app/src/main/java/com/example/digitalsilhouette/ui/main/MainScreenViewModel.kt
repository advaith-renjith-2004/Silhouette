package com.example.digitalsilhouette.ui.main

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitalsilhouette.data.DataRepository
import com.example.digitalsilhouette.data.FocusSession
import com.example.digitalsilhouette.service.FocusService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MainScreenViewModel(
  private val repository: DataRepository
) : ViewModel() {

  val isServiceRunning: StateFlow<Boolean> = repository.isServiceRunning
  val isFocusActive: StateFlow<Boolean> = repository.isFocusActive
  val currentSessionStartTime: StateFlow<Long?> = repository.currentSessionStartTime
  val completedSessions: StateFlow<List<FocusSession>> = repository.completedSessions
  val selectedTheme: StateFlow<String> = repository.selectedTheme
  val sensorLogs: StateFlow<List<String>> = repository.sensorLogs
  val userName: StateFlow<String> = repository.userName
  val userEmail: StateFlow<String> = repository.userEmail

  private val _hasNotificationPermission = MutableStateFlow(true)
  val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission.asStateFlow()

  private val _hasDndPermission = MutableStateFlow(true)
  val hasDndPermission: StateFlow<Boolean> = _hasDndPermission.asStateFlow()

  val uiState: StateFlow<MainScreenUiState> = combine(
    isServiceRunning,
    isFocusActive,
    currentSessionStartTime,
    completedSessions,
    hasNotificationPermission,
    hasDndPermission,
    selectedTheme,
    sensorLogs,
    userName,
    userEmail
  ) { array ->
    val serviceRunning = array[0] as Boolean
    val focusActive = array[1] as Boolean
    val startTime = array[2] as Long?
    @Suppress("UNCHECKED_CAST")
    val sessions = array[3] as List<FocusSession>
    val notifPerm = array[4] as Boolean
    val dndPerm = array[5] as Boolean
    val theme = array[6] as String
    @Suppress("UNCHECKED_CAST")
    val logs = array[7] as List<String>
    val name = array[8] as String
    val email = array[9] as String
    MainScreenUiState.Success(
      isServiceRunning = serviceRunning,
      isFocusActive = focusActive,
      currentSessionStartTime = startTime,
      completedSessions = sessions,
      hasNotificationPermission = notifPerm,
      hasDndPermission = dndPerm,
      selectedTheme = theme,
      sensorLogs = logs,
      userName = name,
      userEmail = email
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState.Loading)

  fun checkPermissions(context: Context) {
    // Check DND Policy Permission
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    _hasDndPermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      notificationManager.isNotificationPolicyAccessGranted
    } else {
      true
    }

    // Check POST_NOTIFICATIONS Permission
    _hasNotificationPermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  private val _showWifiPrompt = MutableStateFlow(false)
  val showWifiPrompt: StateFlow<Boolean> = _showWifiPrompt.asStateFlow()
  var currentDetectedSsid: String = ""

  fun handleToggleRequest(context: Context) {
    if (!android.provider.Settings.canDrawOverlays(context)) {
      // Prompt user to grant permission
      val intent = android.content.Intent(
        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        android.net.Uri.parse("package:${context.packageName}")
      )
      intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
      return
    }

    if (isServiceRunning.value) {
      // If already running, just turn it off
      toggleService(context)
      return
    }

    // Try to detect current SSID
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val network = connectivityManager.activeNetwork
    if (network != null) {
      val capabilities = connectivityManager.getNetworkCapabilities(network)
      if (capabilities != null && capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        val ssid = wifiManager.connectionInfo.ssid
        
        // If it's a new, untracked network and not an empty/unknown one
        if (ssid != "<unknown ssid>" && ssid.isNotBlank() && !repository.targetWifiNetworks.value.contains(ssid)) {
            currentDetectedSsid = ssid
            _showWifiPrompt.value = true
            return
        }
      }
    }
    
    // Fallback: just toggle it normally if no wifi or already known
    toggleService(context)
  }

  fun onWifiPromptResult(context: Context, saveNetwork: Boolean) {
    _showWifiPrompt.value = false
    if (saveNetwork && currentDetectedSsid.isNotBlank()) {
      repository.addTargetWifiNetwork(currentDetectedSsid)
    }
    toggleService(context)
  }

  private fun toggleService(context: Context) {
    if (isServiceRunning.value) {
      // Stop tracking
      repository.setServiceRunning(false)
      repository.logEvent("Activity tracking stopped.")
    } else {
      // Start tracking
      if (androidx.core.app.ActivityCompat.checkSelfPermission(
          context, 
          android.Manifest.permission.ACTIVITY_RECOGNITION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
        
        repository.setServiceRunning(true)
        repository.logEvent("Activity tracking started. Waiting for STILL state.")
        
        // MOCK: Since we cannot fetch play-services-location in this offline build environment,
        // we simulate the OS ActivityRecognition API detecting a "STILL" transition immediately.
        repository.logEvent("[MOCK] Simulating OS Activity Recognition: STILL detected.")
        val intent = Intent(context, com.example.digitalsilhouette.service.FocusService::class.java).apply {
            action = com.example.digitalsilhouette.service.FocusService.ACTION_CHECK_ENVIRONMENT
        }
        context.startService(intent)
        
      } else {
        repository.logEvent("Missing ACTIVITY_RECOGNITION permission.")
      }
    }
  }

  fun changeTheme(themeName: String) {
    repository.setTheme(themeName)
  }

  fun clearLogs() {
    repository.clearLogs()
  }

  fun clearHistory() {
    repository.clearHistory()
  }

  fun logoutUser() {
    repository.logoutUser()
  }
}

sealed interface MainScreenUiState {
  object Loading : MainScreenUiState

  data class Success(
    val isServiceRunning: Boolean,
    val isFocusActive: Boolean,
    val currentSessionStartTime: Long?,
    val completedSessions: List<FocusSession>,
    val hasNotificationPermission: Boolean,
    val hasDndPermission: Boolean,
    val selectedTheme: String,
    val sensorLogs: List<String>,
    val userName: String,
    val userEmail: String
  ) : MainScreenUiState

  data class Error(val throwable: Throwable) : MainScreenUiState
}
