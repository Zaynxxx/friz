package com.example.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable? = null,
    val isSystem: Boolean = false,
    val isWifiBlocked: Boolean = false,
    val isMobileBlocked: Boolean = false,
    val isFrozen: Boolean = false
)

data class FreezeSchedule(
    val id: String,
    val label: String,
    val startTime: String, // "HH:mm" representation
    val endTime: String,  // "HH:mm" representation
    val daysOfWeek: List<Int>, // 1 = Sunday, 2 = Monday ... 7 = Saturday
    val isEnabled: Boolean = true,
    val blockWifi: Boolean = true,
    val blockMobile: Boolean = true,
    val freezeApps: Boolean = true
)

data class FocusStats(
    val blockAttempts: Int = 0,
    val timeSavedMinutes: Int = 0,
    val mostBlockedApp: String = "None yet",
    val dailyFocusMinutes: Map<String, Int> = emptyMap(), // Key: Date "yyyy-MM-dd", Value: minutes
    val firewalledRequestsCount: Int = 0
)
