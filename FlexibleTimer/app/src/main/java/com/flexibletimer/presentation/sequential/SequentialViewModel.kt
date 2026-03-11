package com.flexibletimer.presentation.sequential

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flexibletimer.data.model.SavedSequence
import com.flexibletimer.data.model.TimerEntry
import com.flexibletimer.data.model.TimerMode
import com.flexibletimer.data.repository.SequenceRepository
import com.flexibletimer.service.TimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SequentialUiState(
    val timers: List<TimerEntry> = listOf(TimerEntry("", 60L)),
    val savedSequences: List<SavedSequence> = emptyList(),
    val isSaving: Boolean = false,
    val saveError: String? = null
)

@HiltViewModel
class SequentialViewModel @Inject constructor(
    private val repository: SequenceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SequentialUiState())
    val uiState: StateFlow<SequentialUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSequences(TimerMode.SEQUENTIAL).collect { sequences ->
                _uiState.update { it.copy(savedSequences = sequences) }
            }
        }
    }

    fun setTimerCount(count: Int) {
        val clamped = count.coerceIn(1, 10)
        val current = _uiState.value.timers
        val updated = List(clamped) { i ->
            current.getOrElse(i) { TimerEntry("", 60L) }
        }
        _uiState.update { it.copy(timers = updated) }
    }

    fun updateTimer(index: Int, label: String? = null, durationSeconds: Long? = null) {
        val timers = _uiState.value.timers.toMutableList()
        val current = timers[index]
        timers[index] = current.copy(
            label = label ?: current.label,
            durationSeconds = durationSeconds ?: current.durationSeconds
        )
        _uiState.update { it.copy(timers = timers) }
    }

    fun saveSequence(name: String) {
        if (name.isBlank()) {
            _uiState.update { it.copy(saveError = "Name cannot be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            repository.save(
                SavedSequence(
                    name = name.trim(),
                    timers = _uiState.value.timers,
                    mode = TimerMode.SEQUENTIAL
                )
            )
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun deleteSequence(sequence: SavedSequence) {
        viewModelScope.launch { repository.delete(sequence) }
    }

    fun clearError() = _uiState.update { it.copy(saveError = null) }

    fun startSequential(timers: List<TimerEntry>) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START_SEQUENTIAL
            putStringArrayListExtra(
                TimerService.EXTRA_LABELS,
                ArrayList(timers.map { it.label })
            )
            putExtra(TimerService.EXTRA_DURATIONS, LongArray(timers.size) { i -> timers[i].durationSeconds })
        }
        context.startForegroundService(intent)
    }

    fun stopTimer() {
        context.startService(
            Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_STOP
            }
        )
    }
}
