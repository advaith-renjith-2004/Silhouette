package com.example.digitalsilhouette.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object SupabaseClient {
    private const val TAG = "SupabaseClient"

    suspend fun insertUser(email: String, name: String, passwordHash: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${SupabaseConfig.SUPABASE_URL}/rest/v1/users")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
            connection.setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Prefer", "return=representation")
            connection.doOutput = true

            val jsonInputString = JSONObject().apply {
                put("email", email)
                put("name", name)
                put("password", passwordHash)
            }.toString()

            OutputStreamWriter(connection.outputStream).use { it.write(jsonInputString) }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                // Parse the returned JSON array to get the ID
                val jsonArray = org.json.JSONArray(response)
                if (jsonArray.length() > 0) {
                    return@withContext jsonArray.getJSONObject(0).getString("id")
                }
            } else {
                val errorMsg = connection.errorStream.bufferedReader().use { it.readText() }
                Log.e(TAG, "Failed to insert user. Error: $errorMsg")
                
                // If user already exists (HTTP 409 Conflict), try to fetch their ID
                if (responseCode == 409) {
                    return@withContext fetchUserByEmail(email)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during insertUser", e)
        }
        return@withContext null
    }

    private suspend fun fetchUserByEmail(email: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${SupabaseConfig.SUPABASE_URL}/rest/v1/users?email=eq.$email&select=id")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
            connection.setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")

            if (connection.responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = org.json.JSONArray(response)
                if (jsonArray.length() > 0) {
                    return@withContext jsonArray.getJSONObject(0).getString("id")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during fetchUserByEmail", e)
        }
        return@withContext null
    }

    data class SupabaseUser(val id: String, val name: String, val email: String, val passwordHash: String)

    suspend fun fetchUserProfile(email: String): SupabaseUser? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${SupabaseConfig.SUPABASE_URL}/rest/v1/users?email=eq.$email&select=id,name,email,password")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
            connection.setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")

            if (connection.responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = org.json.JSONArray(response)
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    return@withContext SupabaseUser(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        email = obj.getString("email"),
                        passwordHash = obj.getString("password")
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during fetchUserProfile", e)
        }
        return@withContext null
    }

    suspend fun insertNetwork(userId: String, ssid: String, networkType: String) = withContext(Dispatchers.IO) {
        try {
            val url = URL("${SupabaseConfig.SUPABASE_URL}/rest/v1/saved_networks")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
            connection.setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonInputString = JSONObject().apply {
                put("user_id", userId)
                put("ssid", ssid)
                put("network_type", networkType)
            }.toString()

            OutputStreamWriter(connection.outputStream).use { it.write(jsonInputString) }
            connection.responseCode
        } catch (e: Exception) {
            Log.e(TAG, "Exception during insertNetwork", e)
        }
    }

    suspend fun insertFocusSession(userId: String, startTime: Long, endTime: Long, durationSeconds: Long) = withContext(Dispatchers.IO) {
        try {
            val url = URL("${SupabaseConfig.SUPABASE_URL}/rest/v1/focus_sessions")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
            connection.setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonInputString = JSONObject().apply {
                put("user_id", userId)
                put("start_time", startTime)
                put("end_time", endTime)
                put("duration_seconds", durationSeconds)
            }.toString()

            OutputStreamWriter(connection.outputStream).use { it.write(jsonInputString) }
            connection.responseCode
        } catch (e: Exception) {
            Log.e(TAG, "Exception during insertFocusSession", e)
        }
    }
}
