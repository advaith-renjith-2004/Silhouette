package com.example.digitalsilhouette.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

interface DataRepository {
  val isServiceRunning: StateFlow<Boolean>
  val isFocusActive: StateFlow<Boolean>
  val currentSessionStartTime: StateFlow<Long?>
  val completedSessions: StateFlow<List<FocusSession>>

  fun setServiceRunning(running: Boolean)
  fun setFocusActive(active: Boolean, startTime: Long?)
  fun addSession(startTime: Long, endTime: Long, durationSeconds: Long)
  fun clearHistory()
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

  init {
    loadSessions()
  }

  override fun setServiceRunning(running: Boolean) {
    _isServiceRunning.value = running
  }

  override fun setFocusActive(active: Boolean, startTime: Long?) {
    _isFocusActive.value = active
    _currentSessionStartTime.value = startTime
  }

  override fun addSession(startTime: Long, endTime: Long, durationSeconds: Long) {
    val newSession = FocusSession(
      id = UUID.randomUUID().toString(),
      startTime = startTime,
      endTime = endTime,
      durationSeconds = durationSeconds
    )
    val updatedList = _completedSessions.value.toMutableList().apply {
      add(0, newSession) // Add to the top of the history list
    }
    _completedSessions.value = updatedList
    saveSessions(updatedList)
  }

  override fun clearHistory() {
    _completedSessions.value = emptyList()
    sharedPrefs.edit().remove("completed_sessions").apply()
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
