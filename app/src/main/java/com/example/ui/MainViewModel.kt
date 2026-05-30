package com.example.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.service.FirewallVpnService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application.applicationContext)
    private val appScanner = AppScanner(application.applicationContext)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Preferences and states observed reactively
    val isFirewallEnabled = preferencesManager.firewallEnabledFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )
    val isStrictMode = preferencesManager.strictModeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )
    val isFocusActive = preferencesManager.focusModeActiveFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )
    val focusEndTime = preferencesManager.focusModeEndTimeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0L
    )
    val schedules = preferencesManager.schedulesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Statistics observed reactively
    val blockAttempts = preferencesManager.blockAttemptsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val timeSavedMinutes = preferencesManager.timeSavedMinutesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val firewalledRequests = preferencesManager.firewalledRequestsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val mostBlockedApp = preferencesManager.mostBlockedAppFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "None"
    )

    // Reactive Installed Apps combiner, updates live as states flip
    val installedApps: StateFlow<List<AppInfo>> = combine(
        _searchQuery,
        preferencesManager.wifiBlockedAppsFlow,
        preferencesManager.mobileBlockedAppsFlow,
        preferencesManager.frozenAppsFlow
    ) { query, wifi, mobile, frozen ->
        appScanner.getInstalledApps(query, wifi, mobile, frozen)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleAppFrozen(packageName: String, label: String, isNowFrozen: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAppFrozen(packageName, !isNowFrozen)
        }
    }

    fun toggleAppWifiBlocked(packageName: String, isNowBlocked: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAppWifiBlocked(packageName, !isNowBlocked)
            reevaluateFirewallService()
        }
    }

    fun toggleAppMobileBlocked(packageName: String, isNowBlocked: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAppMobileBlocked(packageName, !isNowBlocked)
            reevaluateFirewallService()
        }
    }

    fun setFirewallEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setFirewallEnabled(enabled)
            reevaluateFirewallService()
        }
    }

    fun setStrictMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setStrictMode(enabled)
        }
    }

    fun startFocusSession(durationMinutes: Int) {
        viewModelScope.launch {
            preferencesManager.startFocusSession(durationMinutes)
        }
    }

    fun stopFocusSession() {
        viewModelScope.launch {
            preferencesManager.stopFocusSession()
        }
    }

    fun addSchedule(schedule: FreezeSchedule) {
        viewModelScope.launch {
            val current = schedules.value.toMutableList()
            current.add(schedule)
            preferencesManager.saveSchedules(current)
        }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            val current = schedules.value.filter { it.id != scheduleId }
            preferencesManager.saveSchedules(current)
        }
    }

    fun toggleScheduleEnabled(scheduleId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val current = schedules.value.map {
                if (it.id == scheduleId) it.copy(isEnabled = isEnabled) else it
            }
            preferencesManager.saveSchedules(current)
        }
    }

    private fun reevaluateFirewallService() {
        // Safe trigger command to Firewall Service
        val context = getApplication<Application>().applicationContext
        
        try {
            val intent = Intent(context, FirewallVpnService::class.java).apply {
                action = FirewallVpnService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Failed to start FirewallVpnService: ${e.message}")
        }
    }
}
