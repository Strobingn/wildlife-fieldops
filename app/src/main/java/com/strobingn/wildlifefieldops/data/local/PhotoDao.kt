package com.strobingn.wildlifefieldops.data.local

import androidx.room.*
import com.strobingn.wildlifefieldops.data.model.Photo
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY takenAt DESC")
    fun getAll(): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getById(id: String): Photo?

    @Query("SELECT * FROM photos WHERE jobId = :jobId ORDER BY takenAt DESC")
    fun getByJob(jobId: String): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE inspectionId = :inspectionId ORDER BY takenAt DESC")
    fun getByInspection(inspectionId: String): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE isUploaded = 0")
    suspend fun getUnuploaded(): List<Photo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: Photo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<Photo>)

    @Update
    suspend fun update(photo: Photo)

    @Delete
    suspend fun delete(photo: Photo)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deleteById(id: String)
}
