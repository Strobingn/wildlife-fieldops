package com.strobingn.wildlifefieldops.data.local

import androidx.room.*
import com.strobingn.wildlifefieldops.data.model.Repair
import kotlinx.coroutines.flow.Flow

@Dao
interface RepairDao {
    @Query("SELECT * FROM repairs ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Repair>>

    @Query("SELECT * FROM repairs WHERE jobId = :jobId ORDER BY createdAt DESC")
    fun getByJob(jobId: String): Flow<List<Repair>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(repair: Repair)

    @Update
    suspend fun update(repair: Repair)

    @Delete
    suspend fun delete(repair: Repair)
}
