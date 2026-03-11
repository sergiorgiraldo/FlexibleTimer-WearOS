package com.flexibletimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
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

        // Intent actions
        const val ACTION_START_SEQUENTIAL = "START_SEQUENTIAL"
        const val ACTION_START_GROUP = "START_GROUP"
        const val ACTION_STOP = "STOP"

        // Intent extras
        const val EXTRA_LABELS = "labels"
        const val EXTRA_DURATIONS = "durations"

        // Shared observable state (process-wide singleton via companion)
        private val _runState = MutableStateFlow<TimerRunState>(TimerRunState.Idle)
        val runState: StateFlow<TimerRunState> = _runState.asStateFlow()
    }

    @Inject
    lateinit var vibrator: Vibrator

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Flexible Timer running"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SEQUENTIAL -> {
                val labels = intent.getStringArrayListExtra(EXTRA_LABELS) ?: return START_NOT_STICKY
                val durations = intent.getLongArrayExtra(EXTRA_DURATIONS) ?: return START_NOT_STICKY
                val timers = labels.zip(durations.toList()).map { (l, d) -> TimerEntry(l, d) }
                startSequential(timers)
            }
            ACTION_START_GROUP -> {
                val labels = intent.getStringArrayListExtra(EXTRA_LABELS) ?: return START_NOT_STICKY
                val durations = intent.getLongArrayExtra(EXTRA_DURATIONS) ?: return START_NOT_STICKY
                val timers = labels.zip(durations.toList()).map { (l, d) -> TimerEntry(l, d) }
                startGroup(timers)
            }
            ACTION_STOP -> stopTimers()
        }
        return START_NOT_STICKY
    }

    // ── Sequential ────────────────────────────────────────────────────────────

    private fun startSequential(timers: List<TimerEntry>) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            vibrateShort()   // start signal
            for ((index, timer) in timers.withIndex()) {
                var remaining = timer.durationSeconds
                while (remaining > 0) {
                    _runState.value = TimerRunState.SequentialRunning(
                        timers = timers,
                        currentIndex = index,
                        remainingSeconds = remaining,
                        label = timer.label
                    )
                    delay(1_000)
                    remaining--
                }
                if (index < timers.lastIndex) {
                    vibrateShort()   // timer finished (not last)
                } else {
                    vibrateTriple()  // last timer finished
                }
            }
            _runState.value = TimerRunState.Finished
            delay(2_000)
            _runState.value = TimerRunState.Idle
            stopSelf()
        }
    }

    // ── Group ─────────────────────────────────────────────────────────────────

    private fun startGroup(timers: List<TimerEntry>) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            vibrateShort()  // start signal
            val remaining = timers.map { it.durationSeconds }.toLongArray()

            while (remaining.any { it > 0 }) {
                _runState.value = TimerRunState.GroupRunning(
                    timers = timers,
                    remainingSeconds = remaining.toList()
                )
                delay(1_000)
                for (i in remaining.indices) {
                    if (remaining[i] > 0) remaining[i]--
                }
            }
            vibrateTriple()
            _runState.value = TimerRunState.Finished
            delay(2_000)
            _runState.value = TimerRunState.Idle
            stopSelf()
        }
    }

    // ── Control ───────────────────────────────────────────────────────────────

    fun stopTimers() {
        timerJob?.cancel()
        timerJob = null
        _runState.value = TimerRunState.Idle
        stopSelf()
    }

    // ── Vibration helpers ─────────────────────────────────────────────────────

    private fun vibrateShort() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(150)
        }
    }

    private fun vibrateTriple() {
        val pattern = longArrayOf(0, 200, 100, 200, 100, 200)
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
            CHANNEL_ID,
            "Flexible Timer",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Flexible Timer")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
