package com.flexibletimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.flexibletimer.MainActivity
import com.flexibletimer.data.model.TimerEntry
import com.flexibletimer.data.model.TimerRunState
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

        const val ACTION_START_SEQUENTIAL = "START_SEQUENTIAL"
        const val ACTION_START_GROUP = "START_GROUP"
        const val ACTION_STOP = "STOP"

        const val EXTRA_LABELS = "labels"
        const val EXTRA_DURATIONS = "durations"

        private val _runState = MutableStateFlow<TimerRunState>(TimerRunState.Idle)
        val runState: StateFlow<TimerRunState> = _runState.asStateFlow()
    }

    @Inject
    lateinit var vibrator: Vibrator

    private lateinit var notificationManager: NotificationManager

    // WakeLock keeps the CPU alive when the screen turns off
    private var wakeLock: PowerManager.WakeLock? = null

    // Use Dispatchers.IO — more resilient than Default under Wear OS background restrictions
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var timerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Flexible Timer running"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SEQUENTIAL -> {
                val labels = intent.getStringArrayListExtra(EXTRA_LABELS) ?: return START_STICKY
                val durations = intent.getLongArrayExtra(EXTRA_DURATIONS) ?: return START_STICKY
                val timers = labels.zip(durations.toList()).map { (l, d) -> TimerEntry(l, d) }
                startSequential(timers)
            }
            ACTION_START_GROUP -> {
                val labels = intent.getStringArrayListExtra(EXTRA_LABELS) ?: return START_STICKY
                val durations = intent.getLongArrayExtra(EXTRA_DURATIONS) ?: return START_STICKY
                val timers = labels.zip(durations.toList()).map { (l, d) -> TimerEntry(l, d) }
                startGroup(timers)
            }
            ACTION_STOP -> stopTimers()
        }
        // START_STICKY: if the OS kills the service, restart it with the last intent
        return START_STICKY
    }

    // ── WakeLock ──────────────────────────────────────────────────────────────

    private fun acquireWakeLock() {
        val pm = getSystemService(PowerManager::class.java)
        wakeLock?.release()
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FlexibleTimer::TimerWakeLock"
        ).also { it.acquire(12 * 60 * 60 * 1000L) } // max 12 hours
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    // ── Sequential ────────────────────────────────────────────────────────────

    private fun startSequential(timers: List<TimerEntry>) {
        timerJob?.cancel()
        acquireWakeLock()
        timerJob = serviceScope.launch {
            vibrateShort()
            // Track end time per segment using the real clock so that extended
            // delays caused by Wear OS power management don't lose time.
            var segmentEnd = SystemClock.elapsedRealtime()
            for ((index, timer) in timers.withIndex()) {
                segmentEnd += timer.durationSeconds * 1000L
                val name = timer.label.ifBlank { "Timer ${index + 1}" }
                while (true) {
                    val now = SystemClock.elapsedRealtime()
                    val msLeft = segmentEnd - now
                    if (msLeft <= 0) break
                    val remaining = (msLeft + 999) / 1000   // ceiling seconds
                    _runState.value = TimerRunState.SequentialRunning(
                        timers = timers,
                        currentIndex = index,
                        remainingSeconds = remaining,
                        label = timer.label
                    )
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        buildNotification("$name — ${remaining.toNotifTime()}")
                    )
                    delay(msLeft.coerceAtMost(1_000))
                }
                if (index < timers.lastIndex) {
                    vibrateShort()
                } else {
                    vibrateTriple()
                }
            }
            _runState.value = TimerRunState.Finished
            notificationManager.notify(NOTIFICATION_ID, buildNotification("Done!"))
            delay(2_000)
            _runState.value = TimerRunState.Idle
            releaseWakeLock()
            stopSelf()
        }
    }

    // ── Group ─────────────────────────────────────────────────────────────────

    private fun startGroup(timers: List<TimerEntry>) {
        timerJob?.cancel()
        acquireWakeLock()
        timerJob = serviceScope.launch {
            vibrateShort()
            // Absolute end time for each slot so real-clock skew doesn't lose time.
            val startMs = SystemClock.elapsedRealtime()
            val endTimes = LongArray(timers.size) { i -> startMs + timers[i].durationSeconds * 1000L }
            val finishVibrated = BooleanArray(timers.size) { false }

            while (true) {
                val now = SystemClock.elapsedRealtime()
                val remaining = LongArray(timers.size) { i ->
                    ((endTimes[i] - now + 999) / 1000).coerceAtLeast(0)
                }
                if (remaining.all { it == 0L }) break

                _runState.value = TimerRunState.GroupRunning(
                    timers = timers,
                    remainingSeconds = remaining.toList()
                )
                val notifText = remaining.mapIndexed { i, s ->
                    "${timers[i].label.ifBlank { "T${i + 1}" }} ${s.toNotifTime()}"
                }.joinToString("  ")
                notificationManager.notify(NOTIFICATION_ID, buildNotification(notifText))

                // Vibrate for each timer that just finished, if others are still running
                val anyStillRunning = remaining.any { it > 0 }
                for (i in timers.indices) {
                    if (!finishVibrated[i] && remaining[i] == 0L && anyStillRunning) {
                        finishVibrated[i] = true
                        vibrateShort()
                    }
                }

                delay(1_000)
            }
            vibrateTriple()
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
        releaseWakeLock()
        _runState.value = TimerRunState.Idle
        stopSelf()
    }

    // ── Vibration ─────────────────────────────────────────────────────────────

    private fun vibrateShort() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(150)
        }
    }

    private fun vibrateTriple() {
        val pattern = longArrayOf(0, 150, 100, 150, 100, 150)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
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
            this,
            0,
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

    /** Formats seconds as MM:SS (no hours prefix to keep notification compact). */
    private fun Long.toNotifTime(): String {
        val m = this / 60
        val s = this % 60
        return "%02d:%02d".format(m, s)
    }

    override fun onDestroy() {
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }
}
