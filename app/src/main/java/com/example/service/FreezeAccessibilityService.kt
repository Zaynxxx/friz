package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.data.PreferencesManager
import com.example.ui.BlockerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FreezeAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var preferencesManager: PreferencesManager
    
    private var frozenApps = setOf<String>()
    private var isFocusActive = false

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(applicationContext)
        
        // Listen to preferences streams
        serviceScope.launch {
            preferencesManager.frozenAppsFlow.collect {
                frozenApps = it
            }
        }
        serviceScope.launch {
            preferencesManager.focusModeActiveFlow.collect {
                isFocusActive = it
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // Avoid blocking our own app or the android system/launcher UI
            if (packageName == this.packageName || 
                packageName == "android" || 
                packageName == "com.android.systemui") {
                return
            }
            
            // Check if this app is frozen and either focus mode is active OR manual block is on
            if (frozenApps.contains(packageName) && isFocusActive) {
                // Increment stats in background
                serviceScope.launch {
                    val appLabel = getAppLabel(packageName)
                    preferencesManager.incrementBlockAttempts(appLabel)
                }

                // Redirect to Home Screen to drop backstack interaction
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)

                // Launch Blocker UI Over everything
                val blockerIntent = Intent(this, BlockerActivity::class.java).apply {
                    putExtra(BlockerActivity.EXTRA_PACKAGE_NAME, packageName)
                    putExtra(BlockerActivity.EXTRA_APP_LABEL, getAppLabel(packageName))
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(blockerIntent)
            }
        }
    }

    private fun getAppLabel(packageName: String): String {
        val pm = packageManager
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    override fun onInterrupt() {
        // App accessibility state interrupted or system memory reclaiming
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
