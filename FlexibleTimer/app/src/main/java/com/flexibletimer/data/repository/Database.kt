package com.flexibletimer.data.repository

import androidx.room.*
import com.flexibletimer.data.model.SavedSequence
import com.flexibletimer.data.model.TimerEntryListConverter
import com.flexibletimer.data.model.TimerMode
import kotlinx.coroutines.flow.Flow

// ── DAO ──────────────────────────────────────────────────────────────────────

@Dao
interface SavedSequenceDao {

    @Query("SELECT * FROM saved_sequences WHERE mode = :mode ORDER BY name ASC")
    fun getByMode(mode: TimerMode): Flow<List<SavedSequence>>

    @Query("SELECT * FROM saved_sequences WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SavedSequence?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(seq: SavedSequence): Long

    @Delete
    suspend fun delete(seq: SavedSequence)

    @Query("SELECT COUNT(*) FROM saved_sequences WHERE mode = :mode")
    suspend fun countByMode(mode: TimerMode): Int
}

// ── Database ─────────────────────────────────────────────────────────────────

@Database(entities = [SavedSequence::class], version = 1, exportSchema = false)
@TypeConverters(TimerEntryListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedSequenceDao(): SavedSequenceDao

    companion object {
        const val DATABASE_NAME = "flexible_timer_db"
    }
}
