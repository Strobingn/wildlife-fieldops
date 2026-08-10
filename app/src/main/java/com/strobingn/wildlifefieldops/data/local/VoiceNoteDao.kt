package com.strobingn.wildlifefieldops.data.local

import androidx.room.*
import com.strobingn.wildlifefieldops.data.model.VoiceNote
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceNoteDao {
    @Query("SELECT * FROM voice_notes ORDER BY createdAt DESC")
    fun getAll(): Flow<List<VoiceNote>>

    @Query("SELECT * FROM voice_notes WHERE jobId = :jobId ORDER BY createdAt DESC")
    fun getByJob(jobId: String): Flow<List<VoiceNote>>

    @Query("SELECT * FROM voice_notes WHERE id = :id")
    suspend fun getById(id: String): VoiceNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: VoiceNote)

    @Update
    suspend fun update(note: VoiceNote)

    @Delete
    suspend fun delete(note: VoiceNote)

    @Query("DELETE FROM voice_notes WHERE id = :id")
    suspend fun deleteById(id: String)
}
