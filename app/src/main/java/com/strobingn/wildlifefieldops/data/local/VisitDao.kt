package com.strobingn.wildlife.data.local

import androidx.room.*
import com.strobingn.wildlife.data.model.Visit
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits ORDER BY visitDate DESC")
    fun getAll(): Flow<List<Visit>>

    @Query("SELECT * FROM visits WHERE jobId = :jobId ORDER BY visitDate DESC")
    fun getByJob(jobId: String): Flow<List<Visit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(visit: Visit)

    @Update
    suspend fun update(visit: Visit)

    @Delete
    suspend fun delete(visit: Visit)
}
