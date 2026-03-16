package com.flexibletimer.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.flexibletimer.MainActivity
import com.flexibletimer.data.model.TimerEntry
import com.flexibletimer.data.model.TimerRunState
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : Service() {

    companion object {
        const val CHANNEL_ID = "flexible_timer_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_START_SEQUENTIAL    = "START_SEQUENTIAL"
        const val ACTION_START_GROUP         = "START_GROUP"
        const val ACTION_STOP                = "STOP"
        // Sent by TimerAlarmReceiver when a sequential segment ends
        const val ACTION_ADVANCE_SEQUENTIAL  = "ADVANCE_SEQUENTIAL"
        // Sent by TimerAlarmReceiver to restart the group UI loop after a slot alarm
        const val ACTION_RESUME_GROUP_UI     = "RESUME_GROUP_UI"
        // Sent by TimerAlarmReceiver when the last segment/slot ends
        const val ACTION_COMPLETE            = "COMPLETE"

        const val EXTRA_LABELS        = "labels"
        const val EXTRA_DURATIONS     = "durations"
        const val EXTRA_SEGMENT_INDEX = "segment_index"
        const val EXTRA_END_TIMES     = "end_times"

        // Request-code base for group slot alarms (keeps them separate from sequential ones)
        private const val GROUP_ALARM_BASE = 1000

        private val _runState = MutableStateFlow<TimerRunState>(TimerRunState.Idle)
        val runState: StateFlow<TimerRunState> = _runState.asStateFlow()
    }

    @Inject
    lateinit var vibrator: Vibrator

    private lateinit var notificationManager: NotificationManager
    private lateinit var alarmManager: AlarmManager

    private var wakeLock: PowerManager.WakeLock? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var timerJob: Job? = null

    // Tracks all pending alarm PendingIntents so we can cancel them on stop.
    private val pendingAlarms = mutableListOf<PendingIntent>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        alarmManager        = getSystemService(AlarmManager::class.java)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Flexible Timer running"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {

            ACTION_START_SEQUENTIAL -> {
                val timers = timersFromIntent(intent) ?: return START_STICKY
                acquireWakeLock()
                cancelAllAlarms()
                vibrateShort()
                launchSequentialSegment(timers, segmentIndex = 0)
            }

            // Alarm fired for segment N → receiver already vibrated → start segment N+1
            ACTION_ADVANCE_SEQUENTIAL -> {
                val timers       = timersFromIntent(intent) ?: return START_STICKY
                val segmentIndex = intent.getIntExtra(EXTRA_SEGMENT_INDEX, 0)
                acquireWakeLock()
                launchSequentialSegment(timers, segmentIndex)
            }

            ACTION_START_GROUP -> {
                val timers = timersFromIntent(intent) ?: return START_STICKY
                acquireWakeLock()
                cancelAllAlarms()
                vibrateShort()
                val endTimes = scheduleGroupAlarms(timers)
                launchGroupUiLoop(timers, endTimes)
            }

            // A group slot alarm fired; receiver already vibrated → refresh UI loop
            ACTION_RESUME_GROUP_UI -> {
                val timers   = timersFromIntent(intent) ?: return START_STICKY
                val endTimes = intent.getLongArrayExtra(EXTRA_END_TIMES) ?: return START_STICKY
                acquireWakeLock()
                launchGroupUiLoop(timers, endTimes)
            }

            ACTION_COMPLETE -> finishTimer()

            ACTION_STOP -> stopTimers()
        }
        return START_STICKY
    }

    // ── WakeLock ──────────────────────────────────────────────────────────────

    private fun acquireWakeLock() {
        val pm = getSystemService(PowerManager::class.java)
        wakeLock?.release()
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FlexibleTimer::TimerWakeLock"
        ).also { it.acquire(12 * 60 * 60 * 1000L) }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    // ── Alarm scheduling ──────────────────────────────────────────────────────

    /**
     * Schedules one alarm for the end of the given sequential segment.
     * The alarm fires TimerAlarmReceiver which vibrates and sends ACTION_ADVANCE_SEQUENTIAL.
     */
    private fun scheduleSequentialAlarm(
        timers: List<TimerEntry>,
        segmentIndex: Int,
        triggerAtMs: Long
    ) {
        val timersJson = Gson().toJson(timers)
        val alarmIntent = Intent(this, TimerAlarmReceiver::class.java).apply {
            action = TimerAlarmReceiver.ACTION_SEQUENTIAL_DONE
            putExtra(TimerAlarmReceiver.EXTRA_TIMERS_JSON, timersJson)
            putExtra(TimerAlarmReceiver.EXTRA_SEGMENT_INDEX, segmentIndex)
        }
        val pi = PendingIntent.getBroadcast(
            this, segmentIndex, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        pendingAlarms.add(pi)
        setExactAlarm(triggerAtMs, pi)
    }

    /**
     * Schedules one alarm per group slot. Returns the endTimes array so the
     * UI loop can be started with the same timestamps.
     */
    private fun scheduleGroupAlarms(timers: List<TimerEntry>): LongArray {
        val timersJson = Gson().toJson(timers)
        val startMs    = SystemClock.elapsedRealtime()
        val endTimes   = LongArray(timers.size) { i -> startMs + timers[i].durationSeconds * 1000L }

        for (i in timers.indices) {
            val alarmIntent = Intent(this, TimerAlarmReceiver::class.java).apply {
                action = TimerAlarmReceiver.ACTION_GROUP_SLOT_DONE
                putExtra(TimerAlarmReceiver.EXTRA_TIMERS_JSON, timersJson)
                putExtra(TimerAlarmReceiver.EXTRA_SLOT_INDEX, i)
                putExtra(TimerAlarmReceiver.EXTRA_END_TIMES, endTimes)
            }
            val pi = PendingIntent.getBroadcast(
                this, GROUP_ALARM_BASE + i, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            pendingAlarms.add(pi)
            setExactAlarm(endTimes[i], pi)
        }
        return endTimes
    }

    private fun setExactAlarm(triggerAtMs: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Fallback: inexact but still Doze-safe
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMs, pi)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMs, pi)
        }
    }

    private fun cancelAllAlarms() {
        pendingAlarms.forEach { alarmManager.cancel(it) }
        pendingAlarms.clear()
    }

    // ── Sequential ────────────────────────────────────────────────────────────

    /**
     * Starts the per-second UI update loop for one sequential segment and
     * schedules an alarm for when it ends. Vibration + advancing to the next
     * segment is handled entirely by TimerAlarmReceiver.
     */
    private fun launchSequentialSegment(timers: List<TimerEntry>, segmentIndex: Int) {
        timerJob?.cancel()
        val segmentEnd = SystemClock.elapsedRealtime() + timers[segmentIndex].durationSeconds * 1000L
        scheduleSequentialAlarm(timers, segmentIndex, segmentEnd)

        val name = timers[segmentIndex].label.ifBlank { "Timer ${segmentIndex + 1}" }
        timerJob = serviceScope.launch {
            while (true) {
                val now   = SystemClock.elapsedRealtime()
                val msLeft = segmentEnd - now
                if (msLeft <= 0) break
                val remaining = (msLeft + 999) / 1000
                _runState.value = TimerRunState.SequentialRunning(
                    timers         = timers,
                    currentIndex   = segmentIndex,
                    remainingSeconds = remaining,
                    label          = timers[segmentIndex].label
                )
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification("$name — ${remaining.toNotifTime()}")
                )
                delay(msLeft.coerceAtMost(1_000))
            }
            // Alarm will fire at the right time regardless of whether this loop
            // ran to completion or the CPU slept through it.
        }
    }

    // ── Group ─────────────────────────────────────────────────────────────────

    private fun launchGroupUiLoop(timers: List<TimerEntry>, endTimes: LongArray) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (true) {
                val now       = SystemClock.elapsedRealtime()
                val remaining = LongArray(timers.size) { i ->
                    ((endTimes[i] - now + 999) / 1000).coerceAtLeast(0)
                }
                if (remaining.all { it == 0L }) break

                _runState.value = TimerRunState.GroupRunning(
                    timers           = timers,
                    remainingSeconds = remaining.toList()
                )
                val notifText = remaining.mapIndexed { i, s ->
                    "${timers[i].label.ifBlank { "T${i + 1}" }} ${s.toNotifTime()}"
                }.joinToString("  ")
                notificationManager.notify(NOTIFICATION_ID, buildNotification(notifText))
                delay(1_000)
            }
        }
    }

    // ── Completion ────────────────────────────────────────────────────────────

    private fun finishTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            _runState.value = TimerRunState.Finished
            notificationManager.notify(NOTIFICATION_ID, buildNotification("Done!"))
            delay(2_000)
            _runState.value = TimerRunState.Idle
            releaseWakeLock()
            stopSelf()
        }
    }

    // ── Control ───────────────────────────────────────────────────────────────

    fun stopTimers() {
        timerJob?.cancel()
        timerJob = null
        cancelAllAlarms()
        releaseWakeLock()
        _runState.value = TimerRunState.Idle
        stopSelf()
    }

    // ── Vibration ─────────────────────────────────────────────────────────────

    private fun vibrateShort() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(150)
        }
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Flexible Timer", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Flexible Timer")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setContentIntent(tapIntent)
            .build()
    }

    private fun Long.toNotifTime(): String {
        val m = this / 60
        val s = this % 60
        return "%02d:%02d".format(m, s)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun timersFromIntent(intent: Intent): List<TimerEntry>? {
        val labels    = intent.getStringArrayListExtra(EXTRA_LABELS) ?: return null
        val durations = intent.getLongArrayExtra(EXTRA_DURATIONS)    ?: return null
        return labels.zip(durations.toList()).map { (l, d) -> TimerEntry(l, d) }
    }

    override fun onDestroy() {
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }
}
