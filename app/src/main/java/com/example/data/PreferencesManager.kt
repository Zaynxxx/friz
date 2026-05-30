package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "freezex_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        private val FROZEN_APPS = stringSetPreferencesKey("frozen_apps")
        private val WIFI_BLOCKED_APPS = stringSetPreferencesKey("wifi_blocked_apps")
        private val MOBILE_BLOCKED_APPS = stringSetPreferencesKey("mobile_blocked_apps")
        private val FIREWALL_ENABLED = booleanPreferencesKey("firewall_enabled")
        private val STRICT_MODE = booleanPreferencesKey("strict_mode")
        private val FOCUS_MODE_ACTIVE = booleanPreferencesKey("focus_mode_active")
        private val FOCUS_MODE_END_TIME = longPreferencesKey("focus_mode_end_time")
        private val SCHEDULES_JSON = stringPreferencesKey("schedules_json")
        
        // Stats keys
        private val BLOCK_ATTEMPTS = intPreferencesKey("block_attempts")
        private val TIME_SAVED_MINUTES = intPreferencesKey("time_saved_minutes")
        private val FIREWALLED_REQUESTS = intPreferencesKey("firewalled_requests")
        private val MOST_BLOCKED_APP = stringPreferencesKey("most_blocked_app")
    }

    // Frozen Apps Map/Set
    val frozenAppsFlow: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[FROZEN_APPS] ?: emptySet()
        }

    // Wi-Fi Blocked Apps
    val wifiBlockedAppsFlow: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[WIFI_BLOCKED_APPS] ?: emptySet()
        }

    // Mobile Blocked Apps
    val mobileBlockedAppsFlow: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[MOBILE_BLOCKED_APPS] ?: emptySet()
        }

    // Firewall master switch
    val firewallEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[FIREWALL_ENABLED] ?: false
        }

    // Strict Mode
    val strictModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[STRICT_MODE] ?: false
        }

    // Active block focus session
    val focusModeActiveFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[FOCUS_MODE_ACTIVE] ?: false
        }

    val focusModeEndTimeFlow: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[FOCUS_MODE_END_TIME] ?: 0L
        }

    // Focus schedules
    val schedulesFlow: Flow<List<FreezeSchedule>> = context.dataStore.data
        .map { preferences ->
            val jsonStr = preferences[SCHEDULES_JSON] ?: "[]"
            parseSchedules(jsonStr)
        }

    // Stats flows
    val blockAttemptsFlow: Flow<Int> = context.dataStore.data.map { it[BLOCK_ATTEMPTS] ?: 0 }
    val timeSavedMinutesFlow: Flow<Int> = context.dataStore.data.map { it[TIME_SAVED_MINUTES] ?: 0 }
    val firewalledRequestsFlow: Flow<Int> = context.dataStore.data.map { it[FIREWALLED_REQUESTS] ?: 0 }
    val mostBlockedAppFlow: Flow<String> = context.dataStore.data.map { it[MOST_BLOCKED_APP] ?: "None" }

    // Write operations
    suspend fun setAppFrozen(packageName: String, freeze: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[FROZEN_APPS]?.toMutableSet() ?: mutableSetOf()
            if (freeze) current.add(packageName) else current.remove(packageName)
            preferences[FROZEN_APPS] = current
        }
    }

    suspend fun clearAllFrozenApps() {
        context.dataStore.edit { preferences ->
            preferences[FROZEN_APPS] = emptySet()
        }
    }

    suspend fun setAppWifiBlocked(packageName: String, block: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[WIFI_BLOCKED_APPS]?.toMutableSet() ?: mutableSetOf()
            if (block) current.add(packageName) else current.remove(packageName)
            preferences[WIFI_BLOCKED_APPS] = current
        }
    }

    suspend fun setAppMobileBlocked(packageName: String, block: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[MOBILE_BLOCKED_APPS]?.toMutableSet() ?: mutableSetOf()
            if (block) current.add(packageName) else current.remove(packageName)
            preferences[MOBILE_BLOCKED_APPS] = current
        }
    }

    suspend fun setFirewallEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FIREWALL_ENABLED] = enabled
        }
    }

    suspend fun setStrictMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[STRICT_MODE] = enabled
        }
    }

    suspend fun startFocusSession(durationMinutes: Int) {
        val endTimeMillis = System.currentTimeMillis() + (durationMinutes * 60 * 1000)
        context.dataStore.edit { preferences ->
            preferences[FOCUS_MODE_ACTIVE] = true
            preferences[FOCUS_MODE_END_TIME] = endTimeMillis
        }
    }

    suspend fun stopFocusSession() {
        context.dataStore.edit { preferences ->
            preferences[FOCUS_MODE_ACTIVE] = false
            preferences[FOCUS_MODE_END_TIME] = 0L
        }
    }

    suspend fun incrementBlockAttempts(appName: String) {
        context.dataStore.edit { preferences ->
            val currAttempts = preferences[BLOCK_ATTEMPTS] ?: 0
            preferences[BLOCK_ATTEMPTS] = currAttempts + 1
            preferences[TIME_SAVED_MINUTES] = (preferences[TIME_SAVED_MINUTES] ?: 0) + 2 // estimate 2 min saved per block
            preferences[MOST_BLOCKED_APP] = appName
        }
    }

    suspend fun incrementFirewalledRequests() {
        context.dataStore.edit { preferences ->
            val count = preferences[FIREWALLED_REQUESTS] ?: 0
            preferences[FIREWALLED_REQUESTS] = count + 1
        }
    }

    suspend fun saveSchedules(schedules: List<FreezeSchedule>) {
        val jsonStr = serializeSchedules(schedules)
        context.dataStore.edit { preferences ->
            preferences[SCHEDULES_JSON] = jsonStr
        }
    }

    private fun serializeSchedules(schedules: List<FreezeSchedule>): String {
        val arr = JSONArray()
        for (sched in schedules) {
            val obj = JSONObject()
            obj.put("id", sched.id)
            obj.put("label", sched.label)
            obj.put("startTime", sched.startTime)
            obj.put("endTime", sched.endTime)
            
            val days = JSONArray()
            sched.daysOfWeek.forEach { days.put(it) }
            obj.put("daysOfWeek", days)
            
            obj.put("isEnabled", sched.isEnabled)
            obj.put("blockWifi", sched.blockWifi)
            obj.put("blockMobile", sched.blockMobile)
            obj.put("freezeApps", sched.freezeApps)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseSchedules(jsonStr: String): List<FreezeSchedule> {
        val list = mutableListOf<FreezeSchedule>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", System.currentTimeMillis().toString())
                val label = obj.optString("label", "Schedule")
                val startTime = obj.optString("startTime", "09:00")
                val endTime = obj.optString("endTime", "17:00")
                
                val daysArr = obj.optJSONArray("daysOfWeek")
                val days = mutableListOf<Int>()
                if (daysArr != null) {
                    for (j in 0 until daysArr.length()) {
                        days.add(daysArr.getInt(j))
                    }
                }
                
                val isEnabled = obj.optBoolean("isEnabled", true)
                val blockWifi = obj.optBoolean("blockWifi", true)
                val blockMobile = obj.optBoolean("blockMobile", true)
                val freezeApps = obj.optBoolean("freezeApps", true)
                
                list.add(FreezeSchedule(id, label, startTime, endTime, days, isEnabled, blockWifi, blockMobile, freezeApps))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
