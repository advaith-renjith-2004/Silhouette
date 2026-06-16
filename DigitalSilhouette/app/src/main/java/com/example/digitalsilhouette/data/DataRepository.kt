package com.example.digitalsilhouette.data

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

interface DataRepository {
  val isServiceRunning: StateFlow<Boolean>
  val isFocusActive: StateFlow<Boolean>
  val currentSessionStartTime: StateFlow<Long?>
  val completedSessions: StateFlow<List<FocusSession>>
  val selectedTheme: StateFlow<String>
  val sensorLogs: StateFlow<List<String>>
  val isLoggedIn: StateFlow<Boolean>
  val userName: StateFlow<String>
  val userEmail: StateFlow<String>
  val userPassword: StateFlow<String>
  val supabaseUserId: StateFlow<String>
  val targetWifiNetworks: StateFlow<Set<String>>
  val homeWifiNetworks: StateFlow<Set<String>>

  fun setServiceRunning(running: Boolean)
  fun setFocusActive(active: Boolean, startTime: Long?)
  fun addSession(startTime: Long, endTime: Long, durationSeconds: Long)
  fun clearHistory()
  fun logEvent(event: String)
  fun setTheme(themeName: String)
  fun clearLogs()
  fun loginUser(email: String, name: String, password: String)
  suspend fun loginExistingUser(email: String, password: String): String?
  fun logoutUser()
  fun addTargetWifiNetwork(ssid: String)
  fun addHomeWifiNetwork(ssid: String)
  fun getRememberedEmail(): String
  fun getRememberedPassword(): String
}

class DefaultDataRepository(private val context: Context) : DataRepository {
  private val sharedPrefs = context.getSharedPreferences("digital_silhouette_prefs", Context.MODE_PRIVATE)

  private val _isServiceRunning = MutableStateFlow(false)
  override val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

  private val _isFocusActive = MutableStateFlow(false)
  override val isFocusActive: StateFlow<Boolean> = _isFocusActive.asStateFlow()

  private val _currentSessionStartTime = MutableStateFlow<Long?>(null)
  override val currentSessionStartTime: StateFlow<Long?> = _currentSessionStartTime.asStateFlow()

  private val _completedSessions = MutableStateFlow<List<FocusSession>>(emptyList())
  override val completedSessions: StateFlow<List<FocusSession>> = _completedSessions.asStateFlow()

  private val _selectedTheme = MutableStateFlow(sharedPrefs.getString("selected_theme", "Aether Neon") ?: "Aether Neon")
  override val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

  private val _sensorLogs = MutableStateFlow<List<String>>(emptyList())
  override val sensorLogs: StateFlow<List<String>> = _sensorLogs.asStateFlow()

  private val _isLoggedIn = MutableStateFlow(sharedPrefs.getBoolean("is_logged_in", false))
  override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

  private val _userName = MutableStateFlow(sharedPrefs.getString("user_name", "") ?: "")
  override val userName: StateFlow<String> = _userName.asStateFlow()

  private val _userEmail = MutableStateFlow(sharedPrefs.getString("user_email", "") ?: "")
  override val userEmail: StateFlow<String> = _userEmail.asStateFlow()

  private val _userPassword = MutableStateFlow(sharedPrefs.getString("user_password", "") ?: "")
  override val userPassword: StateFlow<String> = _userPassword.asStateFlow()

  private val _supabaseUserId = MutableStateFlow(sharedPrefs.getString("supabase_user_id", "") ?: "")
  override val supabaseUserId: StateFlow<String> = _supabaseUserId.asStateFlow()

  private val _targetWifiNetworks = MutableStateFlow(
    sharedPrefs.getStringSet("target_wifi_networks", setOf("\"AndroidWifi\"")) ?: setOf("\"AndroidWifi\"")
  )
  override val targetWifiNetworks: StateFlow<Set<String>> = _targetWifiNetworks.asStateFlow()

  private val _homeWifiNetworks = MutableStateFlow(
    sharedPrefs.getStringSet("home_wifi_networks", emptySet()) ?: emptySet()
  )
  override val homeWifiNetworks: StateFlow<Set<String>> = _homeWifiNetworks.asStateFlow()

  init {
    loadSessions()
    logEvent("System initialized. Welcome to Kinetix.")

    // Auto-sync profile to Supabase on startup if logged in but not synced yet
    val savedUserId = sharedPrefs.getString("supabase_user_id", null)
    val email = sharedPrefs.getString("user_email", "") ?: ""
    val name = sharedPrefs.getString("user_name", "") ?: ""
    val password = sharedPrefs.getString("user_password", "") ?: ""

    if (sharedPrefs.getBoolean("is_logged_in", false) && savedUserId == null && email.isNotEmpty()) {
      kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        logEvent("Background sync: Pushing user profile to Supabase...")
        val userId = SupabaseClient.insertUser(email, name, password)
        if (userId != null) {
          sharedPrefs.edit().putString("supabase_user_id", userId).apply()
          logEvent("Supabase Profile Auto-Synced. ID: $userId")
          syncExistingDataToSupabase(userId)
        } else {
          logEvent("Background sync failed. Will retry next startup.")
        }
      }
    }
  }

  override fun setServiceRunning(running: Boolean) {
    _isServiceRunning.value = running
    logEvent(if (running) "Focus Service started." else "Focus Service stopped.")
  }

  override fun setFocusActive(active: Boolean, startTime: Long?) {
    _isFocusActive.value = active
    _currentSessionStartTime.value = startTime
    logEvent(if (active) "Focus Session Active. System silenced." else "Focus Session Ended. Ringer restored.")
  }

  override fun addSession(startTime: Long, endTime: Long, durationSeconds: Long) {
    val newSession = FocusSession(
      id = UUID.randomUUID().toString(),
      startTime = startTime,
      endTime = endTime,
      durationSeconds = durationSeconds
    )
    val updatedList = _completedSessions.value + newSession
    _completedSessions.value = updatedList
    saveSessions(updatedList)
    logEvent("Focus session tracked: ${durationSeconds / 60}m ${durationSeconds % 60}s")

    // Sync to Supabase
    val userId = sharedPrefs.getString("supabase_user_id", null)
    if (userId != null) {
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            SupabaseClient.insertFocusSession(userId, startTime, endTime, durationSeconds)
        }
    }
  }

  override fun clearHistory() {
    _completedSessions.value = emptyList()
    sharedPrefs.edit().remove("completed_sessions").apply()
    logEvent("Session history cleared.")
  }

  override fun logEvent(event: String) {
    val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    val timestamp = timeFormat.format(Date())
    val logLine = "[$timestamp] $event"
    val currentList = _sensorLogs.value.toMutableList()
    currentList.add(logLine)
    if (currentList.size > 50) {
      currentList.removeAt(0)
    }
    _sensorLogs.value = currentList
  }

  override fun setTheme(themeName: String) {
    _selectedTheme.value = themeName
    sharedPrefs.edit().putString("selected_theme", themeName).apply()
    logEvent("Theme changed to: $themeName")
  }

  override fun clearLogs() {
    _sensorLogs.value = emptyList()
  }

  override fun loginUser(email: String, name: String, password: String) {
    _isLoggedIn.value = true
    _userName.value = name
    _userEmail.value = email
    _userPassword.value = password
    sharedPrefs.edit()
      .putBoolean("is_logged_in", true)
      .putString("user_name", name)
      .putString("user_email", email)
      .putString("user_password", password)
      .putString("remembered_email", email)
      .putString("remembered_password", password)
      .apply()
    logEvent("User $name logged in ($email).")

    // Sync to Supabase
    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      val userId = SupabaseClient.insertUser(email, name, password)
      if (userId != null) {
        sharedPrefs.edit().putString("supabase_user_id", userId).apply()
        withContext(Dispatchers.Main) {
          _supabaseUserId.value = userId
        }
        logEvent("Supabase Profile Synced. ID: $userId")
      }
    }
  }

  override suspend fun loginExistingUser(email: String, password: String): String? = withContext(Dispatchers.IO) {
    val user = SupabaseClient.fetchUserProfile(email) ?: return@withContext "User not found. Please check your email or sign up!"
    if (user.passwordHash != password) {
      return@withContext "Incorrect password. Please try again."
    }

    withContext(Dispatchers.Main) {
      _isLoggedIn.value = true
      _userName.value = user.name
      _userEmail.value = user.email
      _userPassword.value = password
      _supabaseUserId.value = user.id
    }

    sharedPrefs.edit()
      .putBoolean("is_logged_in", true)
      .putString("user_name", user.name)
      .putString("user_email", user.email)
      .putString("user_password", password)
      .putString("supabase_user_id", user.id)
      .putString("remembered_email", email)
      .putString("remembered_password", password)
      .apply()

    logEvent("User ${user.name} logged in from Supabase database.")
    syncExistingDataToSupabase(user.id)
    return@withContext null
  }

  override fun logoutUser() {
    _isLoggedIn.value = false
    _userName.value = ""
    _userEmail.value = ""
    _userPassword.value = ""
    _supabaseUserId.value = ""
    sharedPrefs.edit()
      .putBoolean("is_logged_in", false)
      .putString("user_name", "")
      .putString("user_email", "")
      .putString("user_password", "")
      .remove("supabase_user_id")
      .remove("remembered_email")
      .remove("remembered_password")
      .apply()
    logEvent("User logged out and credentials cleared.")
  }

  override fun addTargetWifiNetwork(ssid: String) {
    val currentSet = _targetWifiNetworks.value.toMutableSet()
    if (currentSet.add(ssid)) {
        _targetWifiNetworks.value = currentSet
        sharedPrefs.edit().putStringSet("target_wifi_networks", currentSet).apply()
        logEvent("Added '$ssid' to Office focus networks.")

        // Sync to Supabase
        val userId = sharedPrefs.getString("supabase_user_id", null)
        if (userId != null) {
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                SupabaseClient.insertNetwork(userId, ssid, "OFFICE")
            }
        }
    }
  }

  override fun addHomeWifiNetwork(ssid: String) {
    val currentSet = _homeWifiNetworks.value.toMutableSet()
    if (currentSet.add(ssid)) {
        _homeWifiNetworks.value = currentSet
        sharedPrefs.edit().putStringSet("home_wifi_networks", currentSet).apply()
        logEvent("Added '$ssid' to Home networks (No DND).")

        // Sync to Supabase
        val userId = sharedPrefs.getString("supabase_user_id", null)
        if (userId != null) {
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                SupabaseClient.insertNetwork(userId, ssid, "HOME")
            }
        }
    }
  }

  private fun syncExistingDataToSupabase(userId: String) {
    // Sync target wifi networks
    _targetWifiNetworks.value.forEach { ssid ->
      kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        SupabaseClient.insertNetwork(userId, ssid, "OFFICE")
      }
    }
    // Sync home wifi networks
    _homeWifiNetworks.value.forEach { ssid ->
      kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        SupabaseClient.insertNetwork(userId, ssid, "HOME")
      }
    }
    // Sync completed focus sessions
    _completedSessions.value.forEach { session ->
      kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        SupabaseClient.insertFocusSession(userId, session.startTime, session.endTime, session.durationSeconds)
      }
    }
  }

  private fun loadSessions() {
    val jsonString = sharedPrefs.getString("completed_sessions", null) ?: return
    try {
      val jsonArray = JSONArray(jsonString)
      val sessionsList = mutableListOf<FocusSession>()
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        sessionsList.add(
          FocusSession(
            id = obj.getString("id"),
            startTime = obj.getLong("startTime"),
            endTime = obj.getLong("endTime"),
            durationSeconds = obj.getLong("durationSeconds")
          )
        )
      }
      _completedSessions.value = sessionsList
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun saveSessions(sessions: List<FocusSession>) {
    try {
      val jsonArray = JSONArray()
      for (session in sessions) {
        val obj = JSONObject().apply {
          put("id", session.id)
          put("startTime", session.startTime)
          put("endTime", session.endTime)
          put("durationSeconds", session.durationSeconds)
        }
        jsonArray.put(obj)
      }
      sharedPrefs.edit().putString("completed_sessions", jsonArray.toString()).apply()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun getRememberedEmail(): String {
    return sharedPrefs.getString("remembered_email", "") ?: ""
  }

  override fun getRememberedPassword(): String {
    return sharedPrefs.getString("remembered_password", "") ?: ""
  }

  companion object {
    @Volatile
    private var INSTANCE: DefaultDataRepository? = null

    fun getInstance(context: Context): DefaultDataRepository {
      return INSTANCE ?: synchronized(this) {
        val instance = DefaultDataRepository(context.applicationContext)
        INSTANCE = instance
        instance
      }
    }
  }
}
