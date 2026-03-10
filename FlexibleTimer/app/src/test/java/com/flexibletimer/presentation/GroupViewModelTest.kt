package com.flexibletimer.presentation.group

import android.content.Context
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
class GroupViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: SequenceRepository
    private lateinit var context: Context
    private lateinit var viewModel: GroupViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        context = mock()
        whenever(repository.getSequences(TimerMode.GROUP)).thenReturn(flowOf(emptyList()))
        viewModel = GroupViewModel(repository, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has two timers`() {
        assertThat(viewModel.uiState.value.timers).hasSize(2)
    }

    @Test
    fun `setTimerCount clamps to min 2`() {
        viewModel.setTimerCount(1)
        assertThat(viewModel.uiState.value.timers).hasSize(2)
    }

    @Test
    fun `setTimerCount clamps to max 4`() {
        viewModel.setTimerCount(10)
        assertThat(viewModel.uiState.value.timers).hasSize(4)
    }

    @Test
    fun `setTimerCount 3 gives three timers`() {
        viewModel.setTimerCount(3)
        assertThat(viewModel.uiState.value.timers).hasSize(3)
    }

    @Test
    fun `updateTimer updates label and preserves duration`() {
        viewModel.updateTimer(1, label = "Core")
        val timer = viewModel.uiState.value.timers[1]
        assertThat(timer.label).isEqualTo("Core")
        assertThat(timer.durationSeconds).isEqualTo(60L)
    }

    @Test
    fun `saveGroup with blank name sets error`() {
        viewModel.saveGroup("")
        assertThat(viewModel.uiState.value.saveError).isNotNull()
    }

    @Test
    fun `saveGroup with valid name calls repository`() = runTest {
        whenever(repository.save(any())).thenReturn(1L)
        viewModel.saveGroup("Circuit")
        verify(repository).save(argThat { name == "Circuit" && mode == TimerMode.GROUP })
    }

    @Test
    fun `startGroup fires correct service intent`() {
        val timers = listOf(TimerEntry("Push", 30L), TimerEntry("Pull", 30L))
        viewModel.startGroup(timers)
        verify(context).startForegroundService(argThat {
            action == TimerService.ACTION_START_GROUP
        })
    }

    @Test
    fun `saved groups loaded from repository`() = runTest {
        val groups = listOf(
            SavedSequence(1, "Circuit A", listOf(TimerEntry("T1", 30)), TimerMode.GROUP)
        )
        whenever(repository.getSequences(TimerMode.GROUP)).thenReturn(flowOf(groups))
        val vm = GroupViewModel(repository, context)
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.savedGroups).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
