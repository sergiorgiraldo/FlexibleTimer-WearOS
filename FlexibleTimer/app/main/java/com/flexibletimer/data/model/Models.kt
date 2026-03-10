package com.flexibletimer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ── Core value types ────────────────────────────────────────────────────────

data class TimerEntry(
    val label: String,
    val durationSeconds: Long
)

enum class TimerMode { SEQUENTIAL, GROUP }

// ── Room entities ────────────────────────────────────────────────────────────

@Entity(tableName = "saved_sequences")
@TypeConverters(TimerEntryListConverter::class)
data class SavedSequence(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val timers: List<TimerEntry>,
    val mode: TimerMode
)

// ── Running state ────────────────────────────────────────────────────────────

sealed class TimerRunState {
    object Idle : TimerRunState()

    /** Sequential: one timer ticking at a time */
    data class SequentialRunning(
        val timers: List<TimerEntry>,
        val currentIndex: Int,
        val remainingSeconds: Long,
        val label: String
    ) : TimerRunState()

    /** Group: all timers ticking in parallel */
    data class GroupRunning(
        val timers: List<TimerEntry>,
        /** remaining seconds per slot, index-aligned */
        val remainingSeconds: List<Long>
    ) : TimerRunState()

    object Finished : TimerRunState()
}

// ── Room type converter ──────────────────────────────────────────────────────

class TimerEntryListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromList(list: List<TimerEntry>): String =
        gson.toJson(list)

    @TypeConverter
    fun toList(json: String): List<TimerEntry> {
        val type = object : TypeToken<List<TimerEntry>>() {}.type
        return gson.fromJson(json, type)
    }
}
