package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.PreferencesManager
import com.example.service.FirewallVpnService
import com.example.worker.ScheduleWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "FreezeX received system boot completion callback")

            // Re-schedule background rule scanning in WorkManager
            try {
                val workRequest = PeriodicWorkRequestBuilder<ScheduleWorker>(15, TimeUnit.MINUTES).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "FreezeXScheduleWork",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to enqueue unique work: ${e.message}")
            }

            // Start local system simulated firewall if previously active
            val preferencesManager = PreferencesManager(context.applicationContext)
            CoroutineScope(Dispatchers.Main).launch {
                val isFirewallEnabled = preferencesManager.firewallEnabledFlow.firstOrNull() ?: false
                if (isFirewallEnabled) {
                    try {
                        val vpnIntent = Intent(context, FirewallVpnService::class.java).apply {
                            action = FirewallVpnService.ACTION_START
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(vpnIntent)
                        } else {
                            context.startService(vpnIntent)
                        }
                    } catch (e: Exception) {
                        Log.e("BootReceiver", "Failed to start FirewallVpnService on boot: ${e.message}")
                    }
                }
            }
        }
    }
}
