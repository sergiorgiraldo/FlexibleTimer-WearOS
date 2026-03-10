package com.flexibletimer.data.repository

import com.flexibletimer.data.model.SavedSequence
import com.flexibletimer.data.model.TimerMode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface SequenceRepository {
    fun getSequences(mode: TimerMode): Flow<List<SavedSequence>>
    suspend fun save(sequence: SavedSequence): Long
    suspend fun delete(sequence: SavedSequence)
    suspend fun countByMode(mode: TimerMode): Int
    suspend fun getById(id: Long): SavedSequence?
}

@Singleton
class SequenceRepositoryImpl @Inject constructor(
    private val dao: SavedSequenceDao
) : SequenceRepository {

    override fun getSequences(mode: TimerMode): Flow<List<SavedSequence>> =
        dao.getByMode(mode)

    override suspend fun save(sequence: SavedSequence): Long =
        dao.insert(sequence)

    override suspend fun delete(sequence: SavedSequence) =
        dao.delete(sequence)

    override suspend fun countByMode(mode: TimerMode): Int =
        dao.countByMode(mode)

    override suspend fun getById(id: Long): SavedSequence? =
        dao.getById(id)
}
