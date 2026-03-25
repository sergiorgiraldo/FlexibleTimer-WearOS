package com.flexibletimer.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.flexibletimer.data.model.TimerEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Receives AlarmManager alarms for timer segment/slot completions.
 * Handles vibration and kicks the service to advance state.
 * This runs even when the CPU was in deep sleep — the alarm wakes it.
 */
class TimerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SEQUENTIAL_DONE = "com.flexibletimer.SEQUENTIAL_DONE"
        const val ACTION_GROUP_SLOT_DONE  = "com.flexibletimer.GROUP_SLOT_DONE"

        const val EXTRA_TIMERS_JSON    = "timers_json"
        const val EXTRA_SEGMENT_INDEX  = "segment_index"
        const val EXTRA_SLOT_INDEX     = "slot_index"
        const val EXTRA_END_TIMES      = "end_times"

        internal const val ALERT_CHANNEL_ID = "timer_alert_channel"
        private const val ALERT_NOTIFICATION_ID = 2
    }

    override fun onReceive(context: Context, intent: Intent) {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return

        when (intent.action) {
            ACTION_SEQUENTIAL_DONE -> onSequentialDone(context, intent, vibrator)
            ACTION_GROUP_SLOT_DONE -> onGroupSlotDone(context, intent, vibrator)
        }
    }

    private fun onSequentialDone(context: Context, intent: Intent, vibrator: Vibrator) {
        val segmentIndex = intent.getIntExtra(EXTRA_SEGMENT_INDEX, 0)
        val timersJson   = intent.getStringExtra(EXTRA_TIMERS_JSON) ?: return
        val timers: List<TimerEntry> = Gson().fromJson(
            timersJson, object : TypeToken<List<TimerEntry>>() {}.type
        )

        val isLast = segmentIndex >= timers.lastIndex
        if (isLast) vibrateTriple(vibrator) else vibrateIntermediate(vibrator)

        val label = timers[segmentIndex].label.ifBlank { "T${segmentIndex + 1}" }
        sendTimerAlert(context, label)

        val serviceIntent = Intent(context, TimerService::class.java).apply {
            action = if (isLast) TimerService.ACTION_COMPLETE
                     else        TimerService.ACTION_ADVANCE_SEQUENTIAL
            putStringArrayListExtra(TimerService.EXTRA_LABELS,
                ArrayList(timers.map { it.label }))
            putExtra(TimerService.EXTRA_DURATIONS,
                timers.map { it.durationSeconds }.toLongArray())
            putExtra(TimerService.EXTRA_SEGMENT_INDEX, segmentIndex + 1)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun onGroupSlotDone(context: Context, intent: Intent, vibrator: Vibrator) {
        val slotIndex = intent.getIntExtra(EXTRA_SLOT_INDEX, 0)
        val endTimes  = intent.getLongArrayExtra(EXTRA_END_TIMES) ?: return
        val timersJson = intent.getStringExtra(EXTRA_TIMERS_JSON) ?: return

        val timers: List<TimerEntry> = Gson().fromJson(
            timersJson, object : TypeToken<List<TimerEntry>>() {}.type
        )
        val label = timers.getOrNull(slotIndex)?.label?.ifBlank { "T${slotIndex + 1}" }
            ?: "T${slotIndex + 1}"
        sendTimerAlert(context, label)

        val now = SystemClock.elapsedRealtime()
        val anyOtherRunning = endTimes.indices.any { i -> i != slotIndex && endTimes[i] > now }

        if (!anyOtherRunning) {
            vibrateTriple(vibrator)
            val serviceIntent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_COMPLETE
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            vibrateIntermediate(vibrator)
            // Restart the UI loop in case the service was killed while CPU slept
            val serviceIntent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_RESUME_GROUP_UI
                putStringArrayListExtra(TimerService.EXTRA_LABELS,
                    ArrayList(timers.map { it.label }))
                putExtra(TimerService.EXTRA_DURATIONS,
                    timers.map { it.durationSeconds }.toLongArray())
                putExtra(TimerService.EXTRA_END_TIMES, endTimes)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun sendTimerAlert(context: Context, label: String) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            nm.getNotificationChannel(ALERT_CHANNEL_ID) == null
        ) {
            nm.createNotificationChannel(
                NotificationChannel(ALERT_CHANNEL_ID, "Timer Alerts", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label)
            .setContentText("$label finished")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(ALERT_NOTIFICATION_ID, notification)
    }

    // ── Vibration ─────────────────────────────────────────────────────────────

    // 3 long loud pulses — intermediate (between sequential timers / group slot done)
    private fun vibrateIntermediate(vibrator: Vibrator) {
        val pattern = longArrayOf(0, 500, 150, 500, 150, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, intArrayOf(0, 255, 0, 255, 0, 255), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    // 3 long loud pulses — start and end
    private fun vibrateTriple(vibrator: Vibrator) {
        val pattern = longArrayOf(0, 500, 150, 500, 150, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, intArrayOf(0, 255, 0, 255, 0, 255), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}
