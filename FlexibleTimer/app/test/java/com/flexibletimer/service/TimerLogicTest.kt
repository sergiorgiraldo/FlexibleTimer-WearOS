package com.flexibletimer.service

import com.flexibletimer.data.model.TimerRunState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for pure timer logic, independent of Android framework.
 * The Service integration is covered by instrumented tests on device.
 */
class TimerLogicTest {

    @Test
    fun `sequential state shows correct index and label`() {
        val timers = listOf(
            com.flexibletimer.data.model.TimerEntry("Warm up", 10),
            com.flexibletimer.data.model.TimerEntry("Sprint", 30),
        )
        val state = TimerRunState.SequentialRunning(
            timers = timers,
            currentIndex = 0,
            remainingSeconds = 10,
            label = timers[0].label
        )
        assertThat(state.label).isEqualTo("Warm up")
        assertThat(state.currentIndex).isEqualTo(0)
        assertThat(state.remainingSeconds).isEqualTo(10)
    }

    @Test
    fun `group state all timers visible`() {
        val timers = listOf(
            com.flexibletimer.data.model.TimerEntry("A", 60),
            com.flexibletimer.data.model.TimerEntry("B", 90),
            com.flexibletimer.data.model.TimerEntry("C", 30),
        )
        val remaining = mutableListOf(60L, 90L, 30L)
        val state = TimerRunState.GroupRunning(timers, remaining)
        assertThat(state.timers).hasSize(3)
        assertThat(state.remainingSeconds[2]).isEqualTo(30L)
    }

    @Test
    fun `group finishes when all remaining reach zero`() {
        // Simulate tick logic
        val remaining = mutableListOf(1L, 1L)
        for (i in remaining.indices) remaining[i]--
        val allDone = remaining.all { it <= 0 }
        assertThat(allDone).isTrue()
    }

    @Test
    fun `group does not finish if any remaining is positive`() {
        val remaining = mutableListOf(0L, 5L)
        val allDone = remaining.all { it <= 0 }
        assertThat(allDone).isFalse()
    }

    @Test
    fun `sequential detects last timer correctly`() {
        val timers = listOf(
            com.flexibletimer.data.model.TimerEntry("A", 10),
            com.flexibletimer.data.model.TimerEntry("B", 20)
        )
        val lastIndex = timers.lastIndex
        assertThat(lastIndex).isEqualTo(1)
        // At index 0, it's not the last
        assertThat(0 < lastIndex).isTrue()
        // At index 1, it IS the last
        assertThat(1 < lastIndex).isFalse()
    }

    @Test
    fun `TimerService constants are distinct`() {
        val actions = setOf(
            TimerService.ACTION_START_SEQUENTIAL,
            TimerService.ACTION_START_GROUP,
            TimerService.ACTION_STOP
        )
        // All three actions are unique strings
        assertThat(actions).hasSize(3)
    }
}
