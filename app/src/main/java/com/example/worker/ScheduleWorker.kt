package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.PreferencesManager
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

class ScheduleWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ScheduleWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Evaluating scheduled sessions background worker")
        val preferencesManager = PreferencesManager(applicationContext)
        val schedules = preferencesManager.schedulesFlow.firstOrNull() ?: emptyList()
        val isFocusActive = preferencesManager.focusModeActiveFlow.firstOrNull() ?: false

        if (schedules.isEmpty()) {
            return Result.success()
        }

        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1 = Sun, 2 = Mon ... 7 = Sat
        
        val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        val currentTimeStr = timeFormat.format(calendar.time) // "HH:mm"

        var shouldActivateFocus = false
        var durationMinutes = 30 // Default fallback

        for (schedule in schedules) {
            if (!schedule.isEnabled) continue

            // Verify if today falls in the active daysOfWeek list
            val isScheduledToday = schedule.daysOfWeek.contains(currentDayOfWeek)
            if (!isScheduledToday) continue

            // Verify if currentTime is between startTime and endTime in standard format
            if (isTimeBetween(currentTimeStr, schedule.startTime, schedule.endTime)) {
                shouldActivateFocus = true
                durationMinutes = calculateDifferenceInMinutes(currentTimeStr, schedule.endTime)
                
                // Mirror schedule target rules to global preferences block
                if (schedule.blockWifi || schedule.blockMobile) {
                    preferencesManager.setFirewallEnabled(true)
                }
                break
            }
        }

        if (shouldActivateFocus) {
            if (!isFocusActive) {
                Log.i(TAG, "Entering scheduled focus mode. Duration remaining: $durationMinutes minutes.")
                preferencesManager.startFocusSession(durationMinutes)
            }
        } else {
            // If focus was started by a schedule and we are no longer in any active schedule, stop it
            // Note: We only stop if it was a schedule-based focus. For simplicity and robustness, 
            // if we are not in any schedule, we let the timed session close itself or manual stop handle it.
        }

        return Result.success()
    }

    private fun isTimeBetween(current: String, start: String, end: String): Boolean {
        return try {
            val df = SimpleDateFormat("HH:mm", Locale.US)
            val currentTime = df.parse(current)
            val startTime = df.parse(start)
            val endTime = df.parse(end)
            
            if (startTime.after(endTime)) {
                // Overnight schedule, e.g. 22:00 to 06:00
                currentTime.after(startTime) || currentTime.before(endTime)
            } else {
                !currentTime.before(startTime) && !currentTime.after(endTime)
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun calculateDifferenceInMinutes(current: String, end: String): Int {
        return try {
            val df = SimpleDateFormat("HH:mm", Locale.US)
            val currentTime = df.parse(current)
            var endTime = df.parse(end)
            
            if (startTimeIsAfter(current, end)) {
                // Standard overnight adjustments
                val calendar = Calendar.getInstance().apply {
                    time = endTime
                    add(Calendar.DATE, 1)
                }
                endTime = calendar.time
            }
            
            val diffMs = endTime.time - currentTime.time
            (diffMs / (1000 * 60)).toInt().coerceAtLeast(1)
        } catch (e: Exception) {
            30
        }
    }

    private fun startTimeIsAfter(start: String, end: String): Boolean {
        val df = SimpleDateFormat("HH:mm", Locale.US)
        val s = df.parse(start)
        val e = df.parse(end)
        return s.after(e)
    }
}
