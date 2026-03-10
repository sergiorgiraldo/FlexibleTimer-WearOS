package com.flexibletimer.presentation.sequential

import android.content.Context
import android.content.Intent
import app.cash.turbine.test
import com.flexibletimer.data.model.SavedSequence
import com.flexibletimer.data.model.TimerEntry
import com.flexibletimer.data.model.TimerMode
import com.flexibletimer.data.repository.SequenceRepository
import com.flexibletimer.service.TimerService
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class SequentialViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: SequenceRepository
    private lateinit var context: Context
    private lateinit var viewModel: SequentialViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        context = mock()

        val savedSequences = listOf(
            SavedSequence(
                id = 1,
                name = "Morning Routine",
                timers = listOf(
                    TimerEntry("Warm up", 60),
                    TimerEntry("Work", 300)
                ),
                mode = TimerMode.SEQUENTIAL
            )
        )
        whenever(repository.getSequences(TimerMode.SEQUENTIAL)).thenReturn(flowOf(savedSequences))
        viewModel = SequentialViewModel(repository, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has one timer with default duration`() {
        val state = viewModel.uiState.value
        assertThat(state.timers).hasSize(1)
        assertThat(state.timers[0].durationSeconds).isEqualTo(60L)
        assertThat(state.timers[0].label).isEmpty()
    }

    @Test
    fun `setTimerCount adds timers when increasing`() {
        viewModel.setTimerCount(3)
        assertThat(viewModel.uiState.value.timers).hasSize(3)
    }

    @Test
    fun `setTimerCount removes timers when decreasing`() {
        viewModel.setTimerCount(5)
        viewModel.setTimerCount(2)
        assertThat(viewModel.uiState.value.timers).hasSize(2)
    }

    @Test
    fun `setTimerCount clamps to min 1`() {
        viewModel.setTimerCount(0)
        assertThat(viewModel.uiState.value.timers).hasSize(1)
    }

    @Test
    fun `setTimerCount clamps to max 10`() {
        viewModel.setTimerCount(20)
        assertThat(viewModel.uiState.value.timers).hasSize(10)
    }

    @Test
    fun `updateTimer updates label correctly`() {
        viewModel.updateTimer(0, label = "Sprint")
        assertThat(viewModel.uiState.value.timers[0].label).isEqualTo("Sprint")
        // Duration unchanged
        assertThat(viewModel.uiState.value.timers[0].durationSeconds).isEqualTo(60L)
    }

    @Test
    fun `updateTimer updates duration correctly`() {
        viewModel.updateTimer(0, durationSeconds = 120L)
        assertThat(viewModel.uiState.value.timers[0].durationSeconds).isEqualTo(120L)
        // Label unchanged
        assertThat(viewModel.uiState.value.timers[0].label).isEmpty()
    }

    @Test
    fun `existing timer data is preserved when timer count grows`() {
        viewModel.updateTimer(0, label = "Existing", durationSeconds = 99L)
        viewModel.setTimerCount(3)
        assertThat(viewModel.uiState.value.timers[0].label).isEqualTo("Existing")
        assertThat(viewModel.uiState.value.timers[0].durationSeconds).isEqualTo(99L)
    }

    @Test
    fun `saveSequence with blank name sets error`() = runTest {
        viewModel.saveSequence("  ")
        assertThat(viewModel.uiState.value.saveError).isNotNull()
    }

    @Test
    fun `saveSequence with valid name calls repository`() = runTest {
        whenever(repository.save(any())).thenReturn(1L)
        viewModel.saveSequence("My Routine")
        verify(repository).save(argThat { name == "My Routine" && mode == TimerMode.SEQUENTIAL })
    }

    @Test
    fun `clearError clears save error`() {
        viewModel.saveSequence("  ") // trigger error
        viewModel.clearError()
        assertThat(viewModel.uiState.value.saveError).isNull()
    }

    @Test
    fun `saved sequences are loaded from repository`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.savedSequences).hasSize(1)
            assertThat(state.savedSequences[0].name).isEqualTo("Morning Routine")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startSequential fires correct service intent`() {
        val timers = listOf(TimerEntry("A", 30L), TimerEntry("B", 60L))
        viewModel.startSequential(timers)
        verify(context).startForegroundService(argThat {
            action == TimerService.ACTION_START_SEQUENTIAL
        })
    }

    @Test
    fun `stopTimer fires stop service intent`() {
        viewModel.stopTimer()
        verify(context).startService(argThat {
            action == TimerService.ACTION_STOP
        })
    }
}
