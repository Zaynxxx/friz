package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FirewallVpnService : Service() {

    companion object {
        private const val TAG = "FirewallVpn"
        private const val NOTIFICATION_ID = 8829
        private const val CHANNEL_ID = "firewall_service_channel"
        
        const val ACTION_START = "com.example.action.START"
        const val ACTION_STOP = "com.example.action.STOP"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var preferencesManager: PreferencesManager
    private var isSimulationRunning = false

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            stopSimulation()
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service in foreground: ${e.message}", e)
        }
        
        if (!isSimulationRunning) {
            isSimulationRunning = true
            startSimulationLoop()
        }

        return START_STICKY
    }

    private fun startSimulationLoop() {
        // Observe configuration changes to automatically turn off if disabled
        serviceScope.launch {
            combine(
                preferencesManager.wifiBlockedAppsFlow,
                preferencesManager.mobileBlockedAppsFlow,
                preferencesManager.firewallEnabledFlow
            ) { wifi, mobile, enabled ->
                Triple(wifi, mobile, enabled)
            }.collect { (wifi, mobile, enabled) ->
                if (!enabled) {
                    stopSimulation()
                    stopSelf()
                }
            }
        }

        // Periodic ticker simulates offline container packet checks beautifully
        serviceScope.launch {
            while (isSimulationRunning) {
                delay(5000)
                val isEnabled = preferencesManager.firewallEnabledFlow.firstOrNull() ?: false
                if (isEnabled) {
                    val wifiBlocked = preferencesManager.wifiBlockedAppsFlow.firstOrNull() ?: emptySet()
                    val mobileBlocked = preferencesManager.mobileBlockedAppsFlow.firstOrNull() ?: emptySet()
                    
                    if (wifiBlocked.isNotEmpty() || mobileBlocked.isNotEmpty()) {
                        preferencesManager.incrementFirewalledRequests()
                    }
                }
            }
        }
    }

    private fun stopSimulation() {
        isSimulationRunning = false
    }

    private fun createNotification(): Notification {
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
            CHANNEL_ID
        } else {
            ""
        }

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                this, 
                0, 
                notificationIntent, 
                PendingIntent.FLAG_IMMUTABLE or Intent.FILL_IN_ACTION
            )
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("FreezeX Shield Active")
            .setContentText("Your device is protected. Internet restrictions are running offline.")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSimulation()
        serviceScope.cancel()
    }
}
