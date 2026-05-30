package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppScanner(private val context: Context) {

    suspend fun getInstalledApps(
        searchQuery: String = "",
        wifiBlockedPackageNames: Set<String> = emptySet(),
        mobileBlockedPackageNames: Set<String> = emptySet(),
        frozenPackageNames: Set<String> = emptySet()
    ): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val selfPackage = context.packageName
        
        // Use Intent query to fetch launchable apps only (user-facing apps)
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        
        val apps = mutableListOf<AppInfo>()
        val seenPackages = mutableSetOf<String>()

        for (info in resolveInfos) {
            val packageName = info.activityInfo.packageName
            if (packageName == selfPackage || seenPackages.contains(packageName)) {
                continue // Skip self and duplicates
            }
            seenPackages.add(packageName)

            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                
                if (searchQuery.isNotEmpty() && !label.contains(searchQuery, ignoreCase = true) && !packageName.contains(searchQuery, ignoreCase = true)) {
                    continue
                }

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 || 
                               (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                
                val icon = try {
                    pm.getApplicationIcon(appInfo)
                } catch (e: Exception) {
                    pm.defaultActivityIcon
                }

                apps.add(
                    AppInfo(
                        name = label,
                        packageName = packageName,
                        icon = icon,
                        isSystem = isSystem,
                        isWifiBlocked = wifiBlockedPackageNames.contains(packageName),
                        isMobileBlocked = mobileBlockedPackageNames.contains(packageName),
                        isFrozen = frozenPackageNames.contains(packageName)
                    )
                )
            } catch (e: PackageManager.NameNotFoundException) {
                // Application was uninstalled in the meantime
            }
        }
        
        // Sort non-system apps first alphabetically, then system apps alphabetically
        apps.sortedWith(compareBy<AppInfo> { it.isSystem }.thenBy { it.name.lowercase() })
    }
}
