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
    hasDndPermission
  ) { array ->
    val serviceRunning = array[0] as Boolean
    val focusActive = array[1] as Boolean
    val startTime = array[2] as Long?
    @Suppress("UNCHECKED_CAST")
    val sessions = array[3] as List<FocusSession>
    val notifPerm = array[4] as Boolean
    val dndPerm = array[5] as Boolean
    MainScreenUiState.Success(
      isServiceRunning = serviceRunning,
      isFocusActive = focusActive,
      currentSessionStartTime = startTime,
      completedSessions = sessions,
      hasNotificationPermission = notifPerm,
      hasDndPermission = dndPerm
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

  fun toggleService(context: Context) {
    val intent = Intent(context, FocusService::class.java)
    if (isServiceRunning.value) {
      context.stopService(intent)
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }
  }

  fun clearHistory() {
    repository.clearHistory()
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
    val hasDndPermission: Boolean
  ) : MainScreenUiState

  data class Error(val throwable: Throwable) : MainScreenUiState
}
