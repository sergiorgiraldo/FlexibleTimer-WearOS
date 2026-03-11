package com.flexibletimer.data

import com.flexibletimer.data.model.TimerEntry
import com.flexibletimer.data.model.TimerMode
import com.flexibletimer.data.model.TimerRunState
import com.flexibletimer.ui.components.toHhMmSs
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ModelTest {

    // ── TimerEntry ────────────────────────────────────────────────────────────

    @Test
    fun `TimerEntry holds label and duration`() {
        val entry = TimerEntry("Sprint", 30L)
        assertThat(entry.label).isEqualTo("Sprint")
        assertThat(entry.durationSeconds).isEqualTo(30L)
    }

    @Test
    fun `TimerEntry with empty label is valid`() {
        val entry = TimerEntry("", 60L)
        assertThat(entry.label).isEmpty()
    }

    // ── TimerRunState ─────────────────────────────────────────────────────────

    @Test
    fun `SequentialRunning carries correct index and label`() {
        val timers = listOf(TimerEntry("A", 10), TimerEntry("B", 20))
        val state = TimerRunState.SequentialRunning(
            timers = timers, currentIndex = 1, remainingSeconds = 15L, label = "B"
        )
        assertThat(state.currentIndex).isEqualTo(1)
        assertThat(state.label).isEqualTo("B")
        assertThat(state.remainingSeconds).isEqualTo(15L)
    }

    @Test
    fun `GroupRunning remainingSeconds index-aligned with timers`() {
        val timers = listOf(TimerEntry("Push", 30), TimerEntry("Pull", 60), TimerEntry("Core", 45))
        val state = TimerRunState.GroupRunning(timers, listOf(30L, 55L, 40L))
        assertThat(state.remainingSeconds[1]).isEqualTo(55L)
    }

    @Test
    fun `Idle state is singleton object`() {
        assertThat(TimerRunState.Idle).isSameInstanceAs(TimerRunState.Idle)
    }

    @Test
    fun `Finished state is singleton object`() {
        assertThat(TimerRunState.Finished).isSameInstanceAs(TimerRunState.Finished)
    }

    // ── toHhMmSs extension ───────────────────────────────────────────────────

    @Test
    fun `toHhMmSs formats zero as 00 00 00`() {
        assertThat(0L.toHhMmSs()).isEqualTo("00:00:00")
    }

    @Test
    fun `toHhMmSs formats 90 seconds as 00 01 30`() {
        assertThat(90L.toHhMmSs()).isEqualTo("00:01:30")
    }

    @Test
    fun `toHhMmSs formats 3600 as 01 00 00`() {
        assertThat(3600L.toHhMmSs()).isEqualTo("01:00:00")
    }

    @Test
    fun `toHhMmSs formats 3661 as 01 01 01`() {
        assertThat(3661L.toHhMmSs()).isEqualTo("01:01:01")
    }

    @Test
    fun `toHhMmSs pads single digit values`() {
        assertThat(65L.toHhMmSs()).isEqualTo("00:01:05")
    }

    @Test
    fun `toHhMmSs formats 59 as 00 00 59`() {
        assertThat(59L.toHhMmSs()).isEqualTo("00:00:59")
    }

    // ── TimerMode ─────────────────────────────────────────────────────────────

    @Test
    fun `TimerMode has SEQUENTIAL and GROUP`() {
        assertThat(TimerMode.values().toList()).containsExactly(TimerMode.SEQUENTIAL, TimerMode.GROUP)
    }
}
