package com.flexibletimer.data

import com.flexibletimer.data.model.SavedSequence
import com.flexibletimer.data.model.TimerEntry
import com.flexibletimer.data.model.TimerMode
import com.flexibletimer.data.repository.SavedSequenceDao
import com.flexibletimer.data.repository.SequenceRepositoryImpl
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class SequenceRepositoryTest {

    private lateinit var dao: SavedSequenceDao
    private lateinit var repository: SequenceRepositoryImpl

    private val sampleSequence = SavedSequence(
        id = 1L,
        name = "Test",
        timers = listOf(TimerEntry("A", 30)),
        mode = TimerMode.SEQUENTIAL
    )

    @Before
    fun setup() {
        dao = mock()
        repository = SequenceRepositoryImpl(dao)
    }

    @Test
    fun `getSequences delegates to dao`() = runTest {
        whenever(dao.getByMode(TimerMode.SEQUENTIAL)).thenReturn(flowOf(listOf(sampleSequence)))
        val flow = repository.getSequences(TimerMode.SEQUENTIAL)
        // Verify dao is called with correct mode
        verify(dao).getByMode(TimerMode.SEQUENTIAL)
        // Flow type assertions
        assertThat(flow).isNotNull()
    }

    @Test
    fun `save delegates to dao insert`() = runTest {
        whenever(dao.insert(any())).thenReturn(1L)
        val result = repository.save(sampleSequence)
        verify(dao).insert(sampleSequence)
        assertThat(result).isEqualTo(1L)
    }

    @Test
    fun `delete delegates to dao delete`() = runTest {
        repository.delete(sampleSequence)
        verify(dao).delete(sampleSequence)
    }

    @Test
    fun `countByMode delegates to dao`() = runTest {
        whenever(dao.countByMode(TimerMode.GROUP)).thenReturn(3)
        val count = repository.countByMode(TimerMode.GROUP)
        assertThat(count).isEqualTo(3)
    }

    @Test
    fun `getById delegates to dao`() = runTest {
        whenever(dao.getById(1L)).thenReturn(sampleSequence)
        val result = repository.getById(1L)
        assertThat(result).isEqualTo(sampleSequence)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        whenever(dao.getById(99L)).thenReturn(null)
        val result = repository.getById(99L)
        assertThat(result).isNull()
    }
}
